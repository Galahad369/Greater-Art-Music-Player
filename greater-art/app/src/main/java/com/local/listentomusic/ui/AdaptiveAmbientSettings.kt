package com.local.listentomusic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AmbientMotion
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.data.ambientIntensityFromStorage
import com.local.listentomusic.ui.theme.GaSpacing
import com.local.listentomusic.ui.theme.GaControl

internal fun adaptiveText(language: AppLanguage, en: String, zh: String, ja: String,
    de: String, fr: String, yue: String): String = when (language) {
    AppLanguage.ENGLISH -> en
    AppLanguage.TRADITIONAL_CHINESE -> zh
    AppLanguage.JAPANESE -> ja
    AppLanguage.GERMAN -> de
    AppLanguage.FRENCH -> fr
    AppLanguage.CANTONESE -> yue
}
internal fun adaptiveLabel(language: AppLanguage) = adaptiveText(language,
    "Adaptive Dynamic", "自適應動態氛圍", "アダプティブ・ダイナミック",
    "Adaptiv-dynamisch", "Ambiance dynamique adaptative", "自適應動態氛圍")
internal fun classicAmbientLabel(language: AppLanguage) = adaptiveText(language,
    "Classic Ambient", "經典氛圍漸層", "クラシック・アンビエント",
    "Klassische Atmosphäre", "Ambiance classique", "經典氛圍漸層")
internal fun adaptiveIntensityLabel(language: AppLanguage) = adaptiveText(language,
    "Adaptive intensity", "自適應強度", "効果の強さ", "Effektstärke", "Intensité adaptative", "自適應強度")

@Composable
internal fun AdaptiveAmbientSettings(preferences: UserPreferences, onIntensity: (Float) -> Unit,
    onReact: (Boolean) -> Unit, onMotion: (AmbientMotion) -> Unit) {
    val language = preferences.appLanguage
    var intensity by remember(preferences.adaptiveIntensity) {
        mutableFloatStateOf(ambientIntensityFromStorage(preferences.adaptiveIntensity))
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = GaSpacing.lg, vertical = GaSpacing.sm)) {
        Text(adaptiveText(language,
            "Media colours create deep glow, flowing reflections and luminous edges. No extra video decoder.",
            "媒體色彩形成深邃光暈、流動反光及發光邊緣，不增加影片解碼器。",
            "メディアの色で深い光彩、流れる反射光、輝く縁を描きます。追加の動画デコーダは使いません。",
            "Medienfarben erzeugen tiefes Leuchten, fließende Lichtreflexe und leuchtende Ränder. Kein zusätzlicher Videodecoder.",
            "Les couleurs du média créent un halo profond, des reflets fluides et des bords lumineux, sans décodeur vidéo supplémentaire.",
            "媒體色彩會形成深層光暈、流動反光同發光邊緣，唔會加影片解碼器。"),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = GaSpacing.md), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(adaptiveIntensityLabel(language))
            Text("${(intensity * 100).toInt()}%")
        }
        Slider(value = intensity, onValueChange = { intensity = it },
            onValueChangeFinished = { onIntensity(intensity) }, modifier = Modifier.heightIn(min = GaControl.touchTarget)
                .semantics { contentDescription = adaptiveIntensityLabel(language) })
        Row(Modifier.fillMaxWidth().heightIn(min = GaControl.touchTarget)
            .toggleable(preferences.adaptiveReactToVideo, role = Role.Switch, onValueChange = onReact),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(adaptiveText(language, "React to video", "回應影片色彩變化", "映像の色変化に反応",
                "Auf Video reagieren", "Réagir à la vidéo", "跟住影片色彩變化"), Modifier.weight(1f))
            Switch(checked = preferences.adaptiveReactToVideo, onCheckedChange = null)
        }
        Text(adaptiveText(language,
            "Off keeps the media palette with steady lighting. This is colour response, not beat sync.",
            "關閉後保留媒體色彩並使用穩定光效。這是色彩回應，不是節拍同步。",
            "オフでもメディアの色を保ち、光は安定します。色への反応であり、ビート同期ではありません。",
            "Aus behält die Medienpalette bei ruhigem Licht. Farbantwort, keine Beat-Synchronisation.",
            "Désactivé : palette conservée, éclairage stable. Réaction aux couleurs, pas aux rythmes.",
            "關咗會保留媒體色彩，用穩定光效。係跟色彩，唔係跟節拍。"),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(adaptiveText(language, "Motion", "動態效果", "動き", "Bewegung", "Mouvement", "動態效果"),
            Modifier.padding(top = GaSpacing.md))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(GaSpacing.sm)) {
            AmbientMotion.entries.forEach { mode ->
                val text = when (mode) {
                    AmbientMotion.OFF -> adaptiveText(language, "Off", "關閉", "オフ", "Aus", "Arrêt", "關")
                    AmbientMotion.SUBTLE -> adaptiveText(language, "Subtle", "輕柔", "控えめ", "Dezent", "Discret", "輕柔")
                    AmbientMotion.NORMAL -> adaptiveText(language, "Normal", "一般", "通常", "Normal", "Normal", "一般")
                }
                FilterChip(selected = preferences.adaptiveMotion == mode, onClick = { onMotion(mode) },
                    label = { Text(text) }, modifier = Modifier.heightIn(min = GaControl.touchTarget))
            }
        }
    }
}
