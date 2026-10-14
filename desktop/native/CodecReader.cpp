#define NOMINMAX
#include "CodecReader.h"
#include "Features.h"
#include <fstream>
#ifdef GA_HAS_CODECS
extern "C"
{
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/opt.h>
#include <libswresample/swresample.h>
}
namespace ga
{
struct CodecReader::Impl
{
    std::ifstream input;
    std::function<bool()> cancel;
    AVFormatContext *format = nullptr;
    AVIOContext *io = nullptr;
    AVCodecContext *decoder = nullptr;
    AVPacket *packet = nullptr;
    AVFrame *frame = nullptr;
    SwrContext *convert = nullptr;
    CodecInfo metadata;
    int stream = -1;
    bool draining = false, finished = false;
    std::int64_t size = 0;
    double origin = 0, nextTimestamp = 0;
    int outputRate = 0, outputChannels = 0;
    static int inputRead(void *opaque, std::uint8_t *data, int requested)
    {
        auto self = static_cast<Impl *>(opaque);
        if (self->cancel && self->cancel())
            return AVERROR_EXIT;
        self->input.read(reinterpret_cast<char *>(data), requested);
        auto read = self->input.gcount();
        return read ? int(read) : AVERROR_EOF;
    }
    static std::int64_t inputSeek(void *opaque, std::int64_t offset, int whence)
    {
        auto self = static_cast<Impl *>(opaque);
        if (whence & AVSEEK_SIZE)
            return self->size;
        whence &= ~AVSEEK_FORCE;
        self->input.clear();
        self->input.seekg(offset, whence == SEEK_SET   ? std::ios::beg
                                  : whence == SEEK_CUR ? std::ios::cur
                                                       : std::ios::end);
        return self->input ? std::int64_t(self->input.tellg()) : AVERROR(EIO);
    }
    static int interrupted(void *opaque)
    {
        auto self = static_cast<Impl *>(opaque);
        return self->cancel && self->cancel();
    }
    ~Impl()
    {
        swr_free(&convert);
        av_frame_free(&frame);
        av_packet_free(&packet);
        avcodec_free_context(&decoder);
        avformat_close_input(&format);
        if (io)
        {
            av_freep(&io->buffer);
            avio_context_free(&io);
        }
    }
};
CodecReader::CodecReader() : impl(std::make_unique<Impl>())
{
}
CodecReader::~CodecReader() = default;
bool codecsAvailable()
{
    return avformat_version() >> 16 == 63 && avcodec_version() >> 16 == 63 && avutil_version() >> 16 == 61;
}
std::wstring codecLicense()
{
    return fromUtf8(avcodec_license());
}
CodecInfo CodecReader::info() const
{
    return impl->metadata;
}
bool CodecReader::open(const std::wstring &path, std::function<bool()> cancel, int outputRate,
                       int outputChannels)
{
    impl = std::make_unique<Impl>();
    auto &p = *impl;
    if (!localMediaPath(path) || !mediaExtension(std::filesystem::path(path).extension().wstring()))
    {
        p.metadata.error = L"Only supported local files may be decoded.";
        return false;
    }
    p.cancel = std::move(cancel);
    p.input.open(std::filesystem::path(path), std::ios::binary);
    std::error_code error;
    p.size = std::int64_t(std::filesystem::file_size(path, error));
    if (error || !p.input)
    {
        p.metadata.error = L"Local media is not readable.";
        return false;
    }
    auto buffer = static_cast<unsigned char *>(av_malloc(32768));
    p.io = avio_alloc_context(buffer, 32768, 0, &p, Impl::inputRead, nullptr, Impl::inputSeek);
    p.format = avformat_alloc_context();
    if (!p.io || !p.format)
    {
        if (!p.io)
            av_free(buffer);
        p.metadata.error = L"Codec allocation failed.";
        return false;
    }
    p.format->pb = p.io;
    p.format->flags |= AVFMT_FLAG_CUSTOM_IO;
    p.format->interrupt_callback = {Impl::interrupted, &p};
    // Only the selected file is granted. Containers cannot open referenced files
    // or protocols behind this custom stream.
    p.format->io_open = [](AVFormatContext *, AVIOContext **, const char *, int, AVDictionary **) {
        return AVERROR(EACCES);
    };
    AVDictionary *options = nullptr;
    av_dict_set(&options, "protocol_whitelist", "file", 0);
    av_dict_set(
        &options, "format_whitelist",
        "mov,mp3,flac,wav,aac,ogg,matroska,webm,avi,aiff,asf,ape,mpeg,mpegts,amr,ac3,eac3,dts,caf,dsf", 0);
    auto filename = toUtf8(path);
    int result = avformat_open_input(&p.format, filename.c_str(), nullptr, &options);
    av_dict_free(&options);
    if (result < 0 || avformat_find_stream_info(p.format, nullptr) < 0)
    {
        p.metadata.error = L"The local container could not be read.";
        return false;
    }
    auto tag = [&](const char *key) {
        auto value = av_dict_get(p.format->metadata, key, nullptr, 0);
        return value ? fromUtf8(value->value) : std::wstring{};
    };
    p.metadata.title = tag("title");
    p.metadata.artist = tag("artist");
    p.metadata.album = tag("album");
    p.metadata.duration = p.format->duration > 0 ? double(p.format->duration) / AV_TIME_BASE : 0;
    for (unsigned i = 0; i < p.format->nb_streams; ++i)
    {
        auto parameters = p.format->streams[i]->codecpar;
        if (parameters->codec_type == AVMEDIA_TYPE_VIDEO &&
            !(p.format->streams[i]->disposition & AV_DISPOSITION_ATTACHED_PIC))
        {
            p.metadata.width = parameters->width;
            p.metadata.height = parameters->height;
        }
    }
    p.stream = av_find_best_stream(p.format, AVMEDIA_TYPE_AUDIO, -1, -1, nullptr, 0);
    if (p.stream < 0)
    {
        p.metadata.error = L"No audio stream was found.";
        return false;
    }
    auto source = p.format->streams[p.stream];
    auto codec = avcodec_find_decoder(source->codecpar->codec_id);
    p.decoder = avcodec_alloc_context3(codec);
    if (!codec || !p.decoder || avcodec_parameters_to_context(p.decoder, source->codecpar) < 0)
    {
        p.metadata.error = L"This codec is unavailable.";
        return false;
    }
    p.decoder->thread_count = 2;
    if (avcodec_open2(p.decoder, codec, nullptr) < 0)
    {
        p.metadata.error = L"The audio decoder could not open.";
        return false;
    }
    p.metadata.rate = p.decoder->sample_rate;
    p.metadata.channels = p.decoder->ch_layout.nb_channels;
    p.metadata.codec = fromUtf8(codec->name);
    p.origin = source->start_time != AV_NOPTS_VALUE ? source->start_time * av_q2d(source->time_base) : 0;
    p.outputRate = outputRate > 0 ? outputRate : p.metadata.rate;
    p.outputChannels = outputChannels > 0 ? outputChannels : p.metadata.channels;
    if (p.outputRate < 8000 || p.outputRate > 768000 || p.outputChannels < 1 || p.outputChannels > 32)
    {
        p.metadata.error = L"Unsupported audio layout.";
        return false;
    }
    AVChannelLayout layout{};
    av_channel_layout_default(&layout, p.outputChannels);
    result = swr_alloc_set_opts2(&p.convert, &layout, AV_SAMPLE_FMT_FLT, p.outputRate, &p.decoder->ch_layout,
                                 p.decoder->sample_fmt, p.metadata.rate, 0, nullptr);
    av_channel_layout_uninit(&layout);
    if (result < 0 || swr_init(p.convert) < 0)
    {
        p.metadata.error = L"Audio sample conversion could not initialize.";
        return false;
    }
    p.frame = av_frame_alloc();
    p.packet = av_packet_alloc();
    return p.frame && p.packet;
}
bool CodecReader::read(std::vector<float> &samples, double &timestamp)
{
    auto &p = *impl;
    samples.clear();
    if (!p.decoder || p.finished)
        return false;
    while (!p.cancel || !p.cancel())
    {
        int result = avcodec_receive_frame(p.decoder, p.frame);
        if (result == 0)
        {
            auto delay = swr_get_delay(p.convert, p.metadata.rate);
            if (p.frame->sample_rate != p.metadata.rate ||
                p.frame->ch_layout.nb_channels != p.metadata.channels)
            {
                p.metadata.error = L"Audio layout changed during decode.";
                p.finished = true;
                return false;
            }
            int capacity =
                int(av_rescale_rnd(delay + p.frame->nb_samples, p.outputRate, p.metadata.rate, AV_ROUND_UP));
            if (capacity < 0 || capacity > p.outputRate * 10)
            {
                p.metadata.error = L"Invalid audio frame size.";
                p.finished = true;
                return false;
            }
            samples.resize(std::size_t(capacity) * p.outputChannels);
            unsigned char *output = reinterpret_cast<unsigned char *>(samples.data());
            int count =
                swr_convert(p.convert, &output, capacity,
                            const_cast<const std::uint8_t **>(p.frame->extended_data), p.frame->nb_samples);
            timestamp =
                p.frame->best_effort_timestamp == AV_NOPTS_VALUE
                    ? p.nextTimestamp
                    : p.frame->best_effort_timestamp * av_q2d(p.format->streams[p.stream]->time_base) -
                          p.origin - double(delay) / p.metadata.rate;
            av_frame_unref(p.frame);
            if (count < 0)
            {
                p.metadata.error = L"Audio conversion failed.";
                p.finished = true;
                return false;
            }
            samples.resize(std::size_t(count) * p.outputChannels);
            p.nextTimestamp = timestamp + double(count) / p.outputRate;
            return true;
        }
        if (result == AVERROR_EOF)
        {
            int capacity = int(av_rescale_rnd(swr_get_delay(p.convert, p.metadata.rate), p.outputRate,
                                              p.metadata.rate, AV_ROUND_UP));
            if (capacity > 0 && capacity <= p.outputRate * 10)
            {
                samples.resize(std::size_t(capacity) * p.outputChannels);
                auto output = reinterpret_cast<unsigned char *>(samples.data());
                int count = swr_convert(p.convert, &output, capacity, nullptr, 0);
                if (count > 0)
                {
                    samples.resize(std::size_t(count) * p.outputChannels);
                    timestamp = p.nextTimestamp;
                    p.nextTimestamp += double(count) / p.outputRate;
                    return true;
                }
            }
            samples.clear();
            p.finished = true;
            return false;
        }
        if (result != AVERROR(EAGAIN))
        {
            p.metadata.error = L"Audio decoding failed.";
            p.finished = true;
            return false;
        }
        if (p.draining)
        {
            p.finished = true;
            return false;
        }
        do
        {
            result = av_read_frame(p.format, p.packet);
            if (result < 0)
                break;
            if (p.packet->stream_index == p.stream)
                break;
            av_packet_unref(p.packet);
        } while (!p.cancel || !p.cancel());
        if (p.cancel && p.cancel())
        {
            av_packet_unref(p.packet);
            return false;
        }
        if (result < 0)
        {
            if (result != AVERROR_EOF)
            {
                p.metadata.error = L"Container read failed.";
                p.finished = true;
                return false;
            }
            avcodec_send_packet(p.decoder, nullptr);
            p.draining = true;
        }
        else
        {
            result = avcodec_send_packet(p.decoder, p.packet);
            av_packet_unref(p.packet);
            if (result < 0 && result != AVERROR(EAGAIN))
            {
                p.metadata.error = L"Audio packet was rejected.";
                p.finished = true;
                return false;
            }
        }
    }
    return false;
}
bool CodecReader::seek(double seconds)
{
    auto &p = *impl;
    if (!p.format || !std::isfinite(seconds) || seconds < 0)
        return false;
    auto target = std::int64_t((seconds + p.origin) * AV_TIME_BASE);
    if (avformat_seek_file(p.format, -1, INT64_MIN, target, INT64_MAX, 0) < 0)
        return false;
    avcodec_flush_buffers(p.decoder);
    swr_close(p.convert);
    if (swr_init(p.convert) < 0)
    {
        p.metadata.error = L"Audio converter could not reset.";
        return false;
    }
    p.nextTimestamp = seconds;
    p.draining = p.finished = false;
    return true;
}
} // namespace ga
#else
namespace ga
{
struct CodecReader::Impl
{
    CodecInfo metadata;
};
CodecReader::CodecReader() : impl(std::make_unique<Impl>())
{
}
CodecReader::~CodecReader() = default;
bool CodecReader::open(const std::wstring &, std::function<bool()>, int, int)
{
    impl->metadata.error = L"Codec libraries have not been bundled in this build.";
    return false;
}
bool CodecReader::read(std::vector<float> &, double &)
{
    return false;
}
bool CodecReader::seek(double)
{
    return false;
}
CodecInfo CodecReader::info() const
{
    return impl->metadata;
}
bool codecsAvailable()
{
    return false;
}
std::wstring codecLicense()
{
    return L"No external codec runtime in this build.";
}
} // namespace ga
#endif
