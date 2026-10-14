#define NOMINMAX
#include "VideoAmbient.h"
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <d3d11.h>
#include <d3d10.h>
#include <filesystem>
#include <fstream>
#include <mfapi.h>
#include <mferror.h>
#include <mfidl.h>
#include <wrl/client.h>
using Microsoft::WRL::ComPtr;
namespace ga
{
namespace
{
std::atomic<std::uint64_t> paletteEpoch{0};
}
void VideoAmbientTap::reset()
{
    std::lock_guard lock(mutex);
    auto generation = ++paletteEpoch;
    palette = {};
    palette.generation = generation;
    framesSeen = 0;
    failOnceForTest = false;
}
VideoPalette VideoAmbientTap::snapshot() const
{
    std::lock_guard lock(mutex);
    auto result = palette;
    result.framesSeen = framesSeen.load();
    return result;
}
void VideoAmbientTap::publish(const std::array<COLORREF, 36> &pixels, std::uint64_t generation, bool gpu)
{
    std::lock_guard lock(mutex);
    if (!enabled || generation != palette.generation)
        return;
    float brightness = 0, saturation = 0;
    for (int half = 0; half < 2; ++half)
    {
        int red = 0, green = 0, blue = 0;
        for (int i = half * 18; i < (half + 1) * 18; ++i)
        {
            int r = GetRValue(pixels[i]), g = GetGValue(pixels[i]), b = GetBValue(pixels[i]);
            red += r;
            green += g;
            blue += b;
            brightness += (.2126f * r + .7152f * g + .0722f * b) / 255;
            int high = std::max({r, g, b}), low = std::min({r, g, b});
            saturation += high ? float(high - low) / high : 0;
        }
        palette.colors[half] = RGB(red / 18, green / 18, blue / 18);
    }
    palette.brightness = brightness / 36;
    palette.saturation = saturation / 36;
    ++palette.samples;
    if (gpu)
        ++palette.gpuSamples;
}
namespace
{
// Read-only, synchronous pass-through MFT. It returns the SAME IMFSample,
// preserving payload, timestamps, duration, flags, HDR metadata and source quality.
// Optional insertion lets incompatible/protected pipelines retain native playback.
class ColorTap final : public IMFTransform
{
    std::atomic<ULONG> references{1};
    std::mutex mutex;
    std::shared_ptr<VideoAmbientTap> tap;
    ComPtr<IMFAttributes> attributes;
    ComPtr<IMFMediaType> input, output;
    ComPtr<IMFSample> sample;
    UINT32 width = 0, height = 0;
    GUID subtype{};
    bool fullRange = false, bt709 = true;
    ULONGLONG lastCopy = 0;
    ComPtr<ID3D11Texture2D> staging;
    ComPtr<ID3D11Device> device;
    ComPtr<ID3D11DeviceContext> context;
    bool pending = false;
    std::uint64_t pendingGeneration = 0;
    DXGI_FORMAT pendingFormat = DXGI_FORMAT_UNKNOWN;
    UINT pendingStorageHeight = 0;
    COLORREF yuv(int y, int u, int v) const
    {
        double yy = fullRange ? y : (y - 16) * 255.0 / 219;
        double uu = (u - 128) * (fullRange ? 1 : 255.0 / 224);
        double vv = (v - 128) * (fullRange ? 1 : 255.0 / 224);
        auto c = [](double value) { return int(std::clamp(std::round(value), 0.0, 255.0)); };
        return bt709 ? RGB(c(yy + 1.5748 * vv), c(yy - .1873 * uu - .4681 * vv), c(yy + 1.8556 * uu))
                     : RGB(c(yy + 1.402 * vv), c(yy - .344136 * uu - .714136 * vv), c(yy + 1.772 * uu));
    }
    void pixels(const BYTE *data, LONG stride, DXGI_FORMAT format, std::uint64_t generation,
                UINT storageHeight = 0)
    {
        if (!data || !width || !height || stride <= 0)
            return;
        std::array<COLORREF, 36> values{};
        for (int row = 0; row < 6; ++row)
            for (int column = 0; column < 6; ++column)
            {
                UINT x = (column * 2 + 1) * width / 12, y = (row * 2 + 1) * height / 12;
                COLORREF value;
                if (format == DXGI_FORMAT_B8G8R8A8_UNORM)
                {
                    const auto p = data + std::size_t(y) * stride + x * 4;
                    value = RGB(p[2], p[1], p[0]);
                }
                else if (format == DXGI_FORMAT_NV12 || format == DXGI_FORMAT_P010)
                {
                    int bytes = format == DXGI_FORMAT_P010 ? 2 : 1;
                    const auto luma = data + std::size_t(y) * stride + x * bytes;
                    const auto uv = data + std::size_t(storageHeight ? storageHeight : height) * stride +
                                    (y / 2) * stride + (x & ~1u) * bytes;
                    // Palette-only quantization; the native high-bit-depth frame is untouched.
                    if (bytes == 2)
                    {
                        const auto yy = reinterpret_cast<const std::uint16_t *>(luma);
                        const auto chroma = reinterpret_cast<const std::uint16_t *>(uv);
                        value = yuv(yy[0] >> 8, chroma[0] >> 8, chroma[1] >> 8);
                    }
                    else
                        value = yuv(luma[0], uv[0], uv[1]);
                }
                else
                    return; // Sampling support is narrower than pass-through support.
                values[row * 6 + column] = value;
            }
        tap->publish(values, generation, storageHeight != 0);
    }
    void observe(IMFSample *incoming)
    {
        if (!tap->enabled)
        {
            pending = false;
            staging.Reset();
            device.Reset();
            context.Reset();
            return;
        }
        auto now = GetTickCount64();
        if (pending && context)
        {
            D3D11_MAPPED_SUBRESOURCE mapped{};
            // Never wait for GPU readback on the decode thread. Late colors are
            // optional; video always passes through immediately.
            HRESULT hr = context->Map(staging.Get(), 0, D3D11_MAP_READ, D3D11_MAP_FLAG_DO_NOT_WAIT, &mapped);
            if (SUCCEEDED(hr))
            {
                pixels(static_cast<const BYTE *>(mapped.pData), LONG(mapped.RowPitch), pendingFormat,
                       pendingGeneration, pendingStorageHeight);
                context->Unmap(staging.Get(), 0);
                pending = false;
            }
            else if (hr != DXGI_ERROR_WAS_STILL_DRAWING)
            {
                pending = false;
                staging.Reset();
            }
        }
        if (pending || now - lastCopy < 900 || width < 2 || height < 2 || width > 8192 || height > 8192)
            return;
        ComPtr<IMFMediaBuffer> buffer;
        if (FAILED(incoming->GetBufferByIndex(0, &buffer)))
            return;
        lastCopy = now;
        auto generation = tap->snapshot().generation;
        ComPtr<IMFDXGIBuffer> dxgi;
        if (SUCCEEDED(buffer.As(&dxgi)))
        {
            ComPtr<ID3D11Texture2D> texture;
            UINT subresource = 0;
            if (FAILED(dxgi->GetResource(IID_PPV_ARGS(&texture))) ||
                FAILED(dxgi->GetSubresourceIndex(&subresource)))
                return;
            D3D11_TEXTURE2D_DESC desc{};
            texture->GetDesc(&desc);
            if (desc.Format != DXGI_FORMAT_NV12 && desc.Format != DXGI_FORMAT_P010 &&
                desc.Format != DXGI_FORMAT_B8G8R8A8_UNORM)
                return;
            // Bound analysis memory independently; oversized/HDR sources still
            // pass through at their original format and dimensions.
            if (std::uint64_t(desc.Width) * desc.Height * 4 > 128ull * 1024 * 1024 || desc.Width < width ||
                desc.Height < height || desc.SampleDesc.Count != 1)
                return;
            ComPtr<ID3D11Device> currentDevice;
            texture->GetDevice(&currentDevice);
            ComPtr<ID3D11DeviceContext> immediate;
            currentDevice->GetImmediateContext(&immediate);
            ComPtr<ID3D10Multithread> threading;
            if (FAILED(immediate.As(&threading)) || !threading->GetMultithreadProtected())
                return;
            D3D11_TEXTURE2D_DESC previous{};
            if (staging)
                staging->GetDesc(&previous);
            if (!staging || currentDevice.Get() != device.Get() || previous.Width != desc.Width ||
                previous.Height != desc.Height || previous.Format != desc.Format)
            {
                staging.Reset();
                device = currentDevice;
                device->GetImmediateContext(&context);
                desc.Usage = D3D11_USAGE_STAGING;
                desc.BindFlags = 0;
                desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
                desc.MiscFlags = 0;
                desc.ArraySize = 1;
                desc.MipLevels = 1;
                if (FAILED(device->CreateTexture2D(&desc, nullptr, &staging)))
                    return;
            }
            context->CopySubresourceRegion(staging.Get(), 0, 0, 0, 0, texture.Get(), subresource, nullptr);
            pending = true;
            pendingGeneration = generation;
            pendingFormat = desc.Format;
            pendingStorageHeight = desc.Height;
            return;
        }
        DXGI_FORMAT format = subtype == MFVideoFormat_NV12   ? DXGI_FORMAT_NV12
                             : subtype == MFVideoFormat_P010 ? DXGI_FORMAT_P010
                             : (subtype == MFVideoFormat_RGB32 || subtype == MFVideoFormat_ARGB32)
                                 ? DXGI_FORMAT_B8G8R8A8_UNORM
                                 : DXGI_FORMAT_UNKNOWN;
        if (format == DXGI_FORMAT_UNKNOWN)
            return;
        ComPtr<IMF2DBuffer2> twoD;
        BYTE *start = nullptr, *allocation = nullptr;
        LONG stride = 0;
        DWORD length = 0;
        if (SUCCEEDED(buffer.As(&twoD)) &&
            SUCCEEDED(twoD->Lock2DSize(MF2DBuffer_LockFlags_Read, &start, &stride, &allocation, &length)))
        {
            auto rows = (format == DXGI_FORMAT_NV12 || format == DXGI_FORMAT_P010)
                            ? std::uint64_t(height) * 3 / 2
                            : height;
            auto needed = rows * (stride > 0 ? stride : 0);
            if (stride > 0 && start >= allocation && std::uint64_t(start - allocation) + needed <= length)
                pixels(start, stride, format, generation);
            twoD->Unlock2D();
            return;
        }
        if (SUCCEEDED(buffer->Lock(&start, nullptr, &length)))
        {
            stride = LONG(MFGetAttributeUINT32(input.Get(), MF_MT_DEFAULT_STRIDE,
                                               format == DXGI_FORMAT_NV12   ? width
                                               : format == DXGI_FORMAT_P010 ? width * 2
                                                                            : width * 4));
            auto rows = (format == DXGI_FORMAT_NV12 || format == DXGI_FORMAT_P010)
                            ? std::uint64_t(height) * 3 / 2
                            : height;
            if (stride > 0 && rows * stride <= length)
                pixels(start, stride, format, generation);
            buffer->Unlock();
        }
    }

  public:
    explicit ColorTap(std::shared_ptr<VideoAmbientTap> value) : tap(std::move(value))
    {
        if (SUCCEEDED(MFCreateAttributes(&attributes, 3)))
        {
            attributes->SetUINT32(MF_SA_D3D11_AWARE, TRUE);
            attributes->SetUINT32(MF_SA_D3D_AWARE, TRUE);
        }
    }
    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID id, void **result) override
    {
        if (!result)
            return E_POINTER;
        *result = nullptr;
        if (id != __uuidof(IUnknown) && id != __uuidof(IMFTransform))
            return E_NOINTERFACE;
        *result = static_cast<IMFTransform *>(this);
        AddRef();
        return S_OK;
    }
    ULONG STDMETHODCALLTYPE AddRef() override
    {
        return ++references;
    }
    ULONG STDMETHODCALLTYPE Release() override
    {
        auto count = --references;
        if (!count)
            delete this;
        return count;
    }
    HRESULT STDMETHODCALLTYPE GetStreamLimits(DWORD *a, DWORD *b, DWORD *c, DWORD *d) override
    {
        if (!a || !b || !c || !d)
            return E_POINTER;
        *a = *b = *c = *d = 1;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetStreamCount(DWORD *a, DWORD *b) override
    {
        if (!a || !b)
            return E_POINTER;
        *a = *b = 1;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetStreamIDs(DWORD, DWORD *, DWORD, DWORD *) override
    {
        return E_NOTIMPL;
    }
    HRESULT STDMETHODCALLTYPE GetInputStreamInfo(DWORD id, MFT_INPUT_STREAM_INFO *info) override
    {
        if (id)
            return MF_E_INVALIDSTREAMNUMBER;
        if (!info)
            return E_POINTER;
        *info = {};
        info->dwFlags = MFT_INPUT_STREAM_WHOLE_SAMPLES | MFT_INPUT_STREAM_SINGLE_SAMPLE_PER_BUFFER |
                        MFT_INPUT_STREAM_HOLDS_BUFFERS;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetOutputStreamInfo(DWORD id, MFT_OUTPUT_STREAM_INFO *info) override
    {
        if (id)
            return MF_E_INVALIDSTREAMNUMBER;
        if (!info)
            return E_POINTER;
        *info = {};
        info->dwFlags = MFT_OUTPUT_STREAM_WHOLE_SAMPLES | MFT_OUTPUT_STREAM_SINGLE_SAMPLE_PER_BUFFER |
                        MFT_OUTPUT_STREAM_PROVIDES_SAMPLES;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetAttributes(IMFAttributes **result) override
    {
        if (!result)
            return E_POINTER;
        return attributes ? attributes.CopyTo(result) : E_OUTOFMEMORY;
    }
    HRESULT STDMETHODCALLTYPE GetInputStreamAttributes(DWORD, IMFAttributes **) override
    {
        return E_NOTIMPL;
    }
    HRESULT STDMETHODCALLTYPE GetOutputStreamAttributes(DWORD, IMFAttributes **) override
    {
        return E_NOTIMPL;
    }
    HRESULT STDMETHODCALLTYPE DeleteInputStream(DWORD) override
    {
        return E_NOTIMPL;
    }
    HRESULT STDMETHODCALLTYPE AddInputStreams(DWORD, DWORD *) override
    {
        return E_NOTIMPL;
    }
    HRESULT available(DWORD id, DWORD index, IMFMediaType **result, bool isOutput)
    {
        if (id)
            return MF_E_INVALIDSTREAMNUMBER;
        if (!result)
            return E_POINTER;
        *result = nullptr;
        std::lock_guard lock(mutex);
        if (input)
            return index ? MF_E_NO_MORE_TYPES : input.CopyTo(result);
        if (isOutput)
            return MF_E_TRANSFORM_TYPE_NOT_SET;
        const GUID *types[]{&MFVideoFormat_P010, &MFVideoFormat_NV12, &MFVideoFormat_RGB32,
                            &MFVideoFormat_ARGB32, &MFVideoFormat_YUY2};
        if (index >= std::size(types))
            return MF_E_NO_MORE_TYPES;
        ComPtr<IMFMediaType> candidate;
        auto hr = MFCreateMediaType(&candidate);
        if (FAILED(hr))
            return hr;
        candidate->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
        candidate->SetGUID(MF_MT_SUBTYPE, *types[index]);
        return candidate.CopyTo(result);
    }
    HRESULT STDMETHODCALLTYPE GetInputAvailableType(DWORD id, DWORD index, IMFMediaType **result) override
    {
        return available(id, index, result, false);
    }
    HRESULT STDMETHODCALLTYPE GetOutputAvailableType(DWORD id, DWORD index, IMFMediaType **result) override
    {
        return available(id, index, result, true);
    }
    HRESULT setType(DWORD id, IMFMediaType *type, DWORD flags, bool isInput)
    {
        if (id)
            return MF_E_INVALIDSTREAMNUMBER;
        if (flags & ~MFT_SET_TYPE_TEST_ONLY)
            return E_INVALIDARG;
        std::lock_guard lock(mutex);
        if (sample)
            return MF_E_TRANSFORM_CANNOT_CHANGE_MEDIATYPE_WHILE_PROCESSING;
        if (type)
        {
            GUID major{};
            if (FAILED(type->GetGUID(MF_MT_MAJOR_TYPE, &major)) || major != MFMediaType_Video)
                return MF_E_INVALIDMEDIATYPE;
            GUID format{};
            type->GetGUID(MF_MT_SUBTYPE, &format);
            auto transfer = MFGetAttributeUINT32(type, MF_MT_TRANSFER_FUNCTION, 0);
            if ((transfer == MFVideoTransFunc_2084 || transfer == MFVideoTransFunc_HLG) &&
                (format == MFVideoFormat_NV12 || format == MFVideoFormat_RGB32 ||
                 format == MFVideoFormat_ARGB32))
                return MF_E_INVALIDMEDIATYPE; // Never request an 8-bit HDR analysis path.
            auto other = isInput ? nullptr : input.Get();
            DWORD equal = 0;
            if (other && type->IsEqual(other, &equal) != S_OK)
                return MF_E_INVALIDMEDIATYPE;
        }
        if (flags & MFT_SET_TYPE_TEST_ONLY)
            return S_OK;
        (isInput ? input : output) = type;
        if (isInput && output)
        {
            DWORD equal = 0;
            if (!type || type->IsEqual(output.Get(), &equal) != S_OK)
                output.Reset();
        }
        pending = false;
        staging.Reset();
        device.Reset();
        context.Reset();
        lastCopy = 0;
        if (input)
        {
            MFGetAttributeSize(input.Get(), MF_MT_FRAME_SIZE, &width, &height);
            input->GetGUID(MF_MT_SUBTYPE, &subtype);
            fullRange = MFGetAttributeUINT32(input.Get(), MF_MT_VIDEO_NOMINAL_RANGE, MFNominalRange_16_235) ==
                        MFNominalRange_0_255;
            bt709 = MFGetAttributeUINT32(input.Get(), MF_MT_YUV_MATRIX, MFVideoTransferMatrix_BT709) !=
                    MFVideoTransferMatrix_BT601;
        }
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE SetInputType(DWORD id, IMFMediaType *type, DWORD flags) override
    {
        return setType(id, type, flags, true);
    }
    HRESULT STDMETHODCALLTYPE SetOutputType(DWORD id, IMFMediaType *type, DWORD flags) override
    {
        return setType(id, type, flags, false);
    }
    HRESULT STDMETHODCALLTYPE GetInputCurrentType(DWORD id, IMFMediaType **result) override
    {
        if (id)
            return MF_E_INVALIDSTREAMNUMBER;
        if (!result)
            return E_POINTER;
        std::lock_guard lock(mutex);
        return input ? input.CopyTo(result) : MF_E_TRANSFORM_TYPE_NOT_SET;
    }
    HRESULT STDMETHODCALLTYPE GetOutputCurrentType(DWORD id, IMFMediaType **result) override
    {
        if (id)
            return MF_E_INVALIDSTREAMNUMBER;
        if (!result)
            return E_POINTER;
        std::lock_guard lock(mutex);
        return output ? output.CopyTo(result) : MF_E_TRANSFORM_TYPE_NOT_SET;
    }
    HRESULT STDMETHODCALLTYPE GetInputStatus(DWORD id, DWORD *flags) override
    {
        if (id)
            return MF_E_INVALIDSTREAMNUMBER;
        if (!flags)
            return E_POINTER;
        std::lock_guard lock(mutex);
        *flags = sample ? 0 : MFT_INPUT_STATUS_ACCEPT_DATA;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetOutputStatus(DWORD *flags) override
    {
        if (!flags)
            return E_POINTER;
        std::lock_guard lock(mutex);
        *flags = sample ? MFT_OUTPUT_STATUS_SAMPLE_READY : 0;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE SetOutputBounds(LONGLONG, LONGLONG) override
    {
        return E_NOTIMPL;
    }
    HRESULT STDMETHODCALLTYPE ProcessEvent(DWORD, IMFMediaEvent *) override
    {
        return E_NOTIMPL;
    }
    HRESULT STDMETHODCALLTYPE ProcessMessage(MFT_MESSAGE_TYPE message, ULONG_PTR) override
    {
        std::lock_guard lock(mutex);
        if (message == MFT_MESSAGE_COMMAND_FLUSH || message == MFT_MESSAGE_NOTIFY_END_STREAMING)
        {
            sample.Reset();
            pending = false;
            staging.Reset();
            device.Reset();
            context.Reset();
            lastCopy = 0;
        }
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE ProcessInput(DWORD id, IMFSample *incoming, DWORD flags) override
    {
        if (id)
            return MF_E_INVALIDSTREAMNUMBER;
        if (!incoming)
            return E_POINTER;
        if (flags)
            return E_INVALIDARG;
        std::lock_guard lock(mutex);
        if (!input || !output)
            return MF_E_TRANSFORM_TYPE_NOT_SET;
        if (sample)
            return MF_E_NOTACCEPTING;
        if (tap->failOnceForTest.exchange(false))
            return E_FAIL;
        sample = incoming;
        ++tap->framesSeen;
        try
        {
            observe(incoming);
        }
        catch (...)
        {
            pending = false;
        } // Decorative failure never rejects a video frame.
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE ProcessOutput(DWORD flags, DWORD count, MFT_OUTPUT_DATA_BUFFER *buffers,
                                            DWORD *status) override
    {
        if (flags || count != 1)
            return E_INVALIDARG;
        if (!buffers || !status)
            return E_POINTER;
        if (buffers[0].dwStreamID)
            return MF_E_INVALIDSTREAMNUMBER;
        std::lock_guard lock(mutex);
        if (!sample)
            return MF_E_TRANSFORM_NEED_MORE_INPUT;
        if (buffers[0].pSample)
            return E_INVALIDARG;
        buffers[0].pSample = sample.Detach();
        buffers[0].dwStatus = 0;
        buffers[0].pEvents = nullptr;
        *status = 0;
        return S_OK;
    }
};
} // namespace
HRESULT createVideoAmbientEffect(std::shared_ptr<VideoAmbientTap> tap, IMFTransform **result)
{
    if (!result)
        return E_POINTER;
    *result = nullptr;
    try
    {
        *result = new ColorTap(std::move(tap));
        return S_OK;
    }
    catch (...)
    {
        return E_OUTOFMEMORY;
    }
}
bool videoAmbientTests(const std::wstring &directory)
{
    auto com = CoInitializeEx(nullptr, COINIT_MULTITHREADED);
    auto startup = MFStartup(MF_VERSION);
    std::ofstream report(std::filesystem::path(directory) / L"video-ambient-tests.txt");
    bool ok = true;
    auto check = [&](bool condition, const char *name) {
        report << (condition ? "PASS " : "FAIL ") << name << "\n";
        ok = ok && condition;
    };
    auto tap = std::make_shared<VideoAmbientTap>();
    check(ambientSamplingAllowed(true, false, false, false, 2), "visible Adaptive enables sampling");
    check(!ambientSamplingAllowed(false, false, false, false, 2) &&
              !ambientSamplingAllowed(true, true, false, false, 2),
          "hidden and fullscreen disable decorative work");
    check(!ambientSamplingAllowed(true, false, true, false, 2) &&
              !ambientSamplingAllowed(true, false, false, true, 2),
          "contrast and native recovery disable sampling");
    check(!ambientSamplingAllowed(true, false, false, false, 0) &&
              !ambientSamplingAllowed(true, false, false, false, 3),
          "solid and custom image avoid video analysis");
    tap->enabled = true;
    tap->reset();
    ComPtr<IMFTransform> transform;
    check(SUCCEEDED(createVideoAmbientEffect(tap, &transform)), "create optional pass-through transform");
    MFT_INPUT_STREAM_INFO ownership{};
    check(SUCCEEDED(transform->GetInputStreamInfo(0, &ownership)) &&
              (ownership.dwFlags & MFT_INPUT_STREAM_HOLDS_BUFFERS),
          "declares retained input ownership honestly");
    ComPtr<IMFMediaType> type;
    MFCreateMediaType(&type);
    type->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
    type->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_RGB32);
    MFSetAttributeSize(type.Get(), MF_MT_FRAME_SIZE, 16, 16);
    check(SUCCEEDED(transform->SetInputType(0, type.Get(), 0)) &&
              SUCCEEDED(transform->SetOutputType(0, type.Get(), 0)),
          "identical video types negotiated");
    ComPtr<IMFSample> sample;
    ComPtr<IMFMediaBuffer> buffer;
    MFCreateSample(&sample);
    MFCreateMemoryBuffer(1024, &buffer);
    BYTE *data = nullptr;
    buffer->Lock(&data, nullptr, nullptr);
    for (int i = 0; i < 256; ++i)
    {
        data[i * 4] = 0;
        data[i * 4 + 1] = 0;
        data[i * 4 + 2] = 240;
        data[i * 4 + 3] = 255;
    }
    buffer->Unlock();
    buffer->SetCurrentLength(1024);
    sample->AddBuffer(buffer.Get());
    sample->SetSampleTime(12345678);
    sample->SetSampleDuration(333333);
    check(SUCCEEDED(transform->ProcessInput(0, sample.Get(), 0)), "accept original sample");
    check(transform->ProcessInput(0, sample.Get(), 0) == MF_E_NOTACCEPTING, "bounded one-sample admission");
    MFT_OUTPUT_DATA_BUFFER output{};
    DWORD status = 0;
    check(SUCCEEDED(transform->ProcessOutput(0, 1, &output, &status)) && output.pSample == sample.Get(),
          "returns exact input sample identity");
    LONGLONG time = 0, duration = 0;
    if (output.pSample)
    {
        output.pSample->GetSampleTime(&time);
        output.pSample->GetSampleDuration(&duration);
        output.pSample->Release();
    }
    check(time == 12345678 && duration == 333333, "preserves timestamp and duration");
    buffer->Lock(&data, nullptr, nullptr);
    bool unchanged = true;
    for (int i = 0; i < 256; ++i)
        unchanged = unchanged && data[i * 4 + 2] == 240 && data[i * 4 + 3] == 255;
    buffer->Unlock();
    check(unchanged, "does not modify source bytes");
    auto palette = tap->snapshot();
    check(palette.samples == 1 && GetRValue(palette.colors[0]) == 240 && GetBValue(palette.colors[0]) == 0,
          "sparse colors read from real decoded buffer");
    tap->reset();
    std::array<COLORREF, 36> stale{};
    stale.fill(RGB(0, 0, 255));
    tap->publish(stale, palette.generation);
    check(tap->snapshot().samples == 0, "stale media generation rejected");
    tap->enabled = false;
    tap->publish(stale, tap->snapshot().generation);
    check(tap->snapshot().samples == 0, "disabled analysis never publishes");
    ComPtr<IMFTransform> reverseOrder;
    createVideoAmbientEffect(tap, &reverseOrder);
    check(SUCCEEDED(reverseOrder->SetOutputType(0, type.Get(), 0)) &&
              SUCCEEDED(reverseOrder->SetInputType(0, type.Get(), 0)),
          "output-first negotiation preserves matching type");
    check(SUCCEEDED(reverseOrder->ProcessInput(0, sample.Get(), 0)),
          "output-first negotiation accepts original buffer");
    MFT_OUTPUT_DATA_BUFFER reverseOutput{};
    check(SUCCEEDED(reverseOrder->ProcessOutput(0, 1, &reverseOutput, &status)) &&
              reverseOutput.pSample == sample.Get(),
          "output-first path remains identical pass-through");
    if (reverseOutput.pSample)
        reverseOutput.pSample->Release();
    ComPtr<IMFMediaType> advertised;
    ComPtr<IMFTransform> fresh;
    createVideoAmbientEffect(tap, &fresh);
    check(SUCCEEDED(fresh->GetInputAvailableType(0, 0, &advertised)),
          "unconfigured input advertises a negotiable video type");
    reverseOrder.Reset();
    fresh.Reset();
    advertised.Reset();
    ComPtr<IMFMediaType> hdr;
    MFCreateMediaType(&hdr);
    type->CopyAllItems(hdr.Get());
    hdr->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_NV12);
    hdr->SetUINT32(MF_MT_TRANSFER_FUNCTION, MFVideoTransFunc_2084);
    ComPtr<IMFTransform> hdrTap;
    createVideoAmbientEffect(tap, &hdrTap);
    check(hdrTap->SetInputType(0, hdr.Get(), MFT_SET_TYPE_TEST_ONLY) == MF_E_INVALIDMEDIATYPE,
          "tagged HDR never negotiates 8-bit analysis input");
    hdr->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_P010);
    check(SUCCEEDED(hdrTap->SetInputType(0, hdr.Get(), 0)) &&
              SUCCEEDED(hdrTap->SetOutputType(0, hdr.Get(), 0)),
          "high-bit-depth HDR type stays unchanged");
    check(SUCCEEDED(hdrTap->ProcessInput(0, sample.Get(), 0)),
          "opaque HDR payload passes without modification");
    MFT_OUTPUT_DATA_BUFFER hdrOutput{};
    check(SUCCEEDED(hdrTap->ProcessOutput(0, 1, &hdrOutput, &status)) && hdrOutput.pSample == sample.Get(),
          "HDR sample identity and metadata preserved");
    if (hdrOutput.pSample)
        hdrOutput.pSample->Release();
    hdrTap.Reset();
    hdr.Reset();
    transform->ProcessMessage(MFT_MESSAGE_COMMAND_FLUSH, 0);
    output = {};
    check(transform->ProcessOutput(0, 1, &output, &status) == MF_E_TRANSFORM_NEED_MORE_INPUT,
          "flush retains no pending frame");
    transform.Reset();
    sample.Reset();
    buffer.Reset();
    type.Reset();
    if (SUCCEEDED(startup))
        MFShutdown();
    if (SUCCEEDED(com))
        CoUninitialize();
    return ok;
}
} // namespace ga
