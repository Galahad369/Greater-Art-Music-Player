package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage

/** New Settings copy is translated before the legacy English-key fallback. */
internal fun settingsUiText(language: AppLanguage, english: String): String? {
    val index = when (language) {
        AppLanguage.ENGLISH -> return null
        AppLanguage.TRADITIONAL_CHINESE -> 0
        AppLanguage.CANTONESE -> 1
        AppLanguage.JAPANESE -> 2
        AppLanguage.GERMAN -> 3
        AppLanguage.FRENCH -> 4
    }
    return settingsTranslations[english]?.get(index)
}

private val settingsTranslations = mapOf(
    "Tap to expand" to listOf("點按展開", "撳開睇設定", "タップして展開", "Zum Öffnen tippen", "Toucher pour développer"),
    "Tap to collapse" to listOf("點按收起", "撳返收埋", "タップして折りたたむ", "Zum Schließen tippen", "Toucher pour replier"),
    "Mini window size" to listOf("迷你視窗大小", "迷你窗大細", "ミニウィンドウのサイズ", "Mini-Fenstergröße", "Taille de la mini-fenêtre"),
    "Ambient" to listOf("氛圍漸層", "氛圍漸層", "アンビエント", "Ambient", "Ambiance"),
    "Forest · green" to listOf("森林 · 綠", "森林 · 綠", "フォレスト · 緑", "Wald · Grün", "Forêt · vert"),
    "Slate · blue" to listOf("岩板 · 藍", "岩板 · 藍", "スレート · 青", "Schiefer · Blau", "Ardoise · bleu"),
    "Amber · gold" to listOf("琥珀 · 金", "琥珀 · 金", "アンバー · 金", "Bernstein · Gold", "Ambre · or"),
    "Indigo · violet" to listOf("靛藍 · 紫", "靛藍 · 紫", "インディゴ · 紫", "Indigo · Violett", "Indigo · violet"),
    "Rose · pink" to listOf("玫瑰 · 粉", "玫瑰 · 粉", "ローズ · ピンク", "Rose · Rosa", "Rose · rose"),
    "Monochrome" to listOf("黑白", "黑白", "モノクロ", "Monochrom", "Monochrome"),
    "OLED · true black" to listOf("OLED · 純黑", "OLED · 純黑", "OLED · 完全な黒", "OLED · echtes Schwarz", "OLED · noir pur"),
    "Small follows Mini size. Medium and Large enlarge the row and artwork." to listOf("小列跟隨迷你視窗大小。中、大列會放大列與縮圖。", "小列跟迷你窗大細，中同大會放大列同封面。", "小はミニサイズに連動。中と大は行と画像を拡大。", "Klein folgt der Mini-Größe. Mittel und Groß vergrößern Zeile und Bild.", "Petit suit la taille mini. Moyen et Grand agrandissent la ligne et l’image."),
    "Sizes the floating player and its docked preview. No invisible border." to listOf("調整浮動播放器及固定預覽大小，不加入隱形邊框。", "改浮動播放器同固定預覽大細，冇隱形邊框。", "フローティングプレーヤーとドックの画像サイズ。隠れた余白なし。", "Größe des schwebenden Players und der Vorschau. Kein unsichtbarer Rand.", "Taille du lecteur flottant et de son aperçu. Sans bord invisible."),
    "Accent colours for controls. OLED uses true black; all other colours follow Light / Dark." to listOf("控制按鈕的重點色。OLED 使用純黑底色；其他配色跟隨淺色／深色設定。", "揀按鈕重點色。OLED 用純黑，其他跟淺色／深色設定。", "操作ボタンのアクセント色。OLEDは完全な黒、他は明暗設定に従います。", "Akzentfarben der Bedienelemente. OLED nutzt echtes Schwarz; andere folgen Hell/Dunkel.", "Couleurs d’accent des commandes. OLED utilise le noir pur ; les autres suivent Clair/Sombre."),
    "A soft gradient from the playing video. No extra decoder or audio stream; neutral when no video colours are available." to listOf("使用播放中影片色彩的柔和漸層，不增加解碼器或聲音。沒有影片色彩時使用中性色。", "用播緊條片嘅色做柔和漸層，唔加解碼器同聲音；冇影片色就用中性色。", "再生動画の色から柔らかなグラデーション。追加のデコーダーや音声なし。色がない場合は中性色。", "Sanfter Verlauf aus dem laufenden Video. Kein zusätzlicher Decoder oder Ton; ohne Videofarben neutral.", "Dégradé doux tiré de la vidéo. Aucun décodeur ni son supplémentaire ; neutre sans couleurs vidéo."),
)
