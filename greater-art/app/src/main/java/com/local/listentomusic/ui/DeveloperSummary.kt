package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage

/** Presentation facts only: no media names, paths, decoder dumps or parsed report fragments. */
internal data class DeveloperSummary(
    val version: String,
    val screen: String,
    val playing: Boolean,
    val video: Boolean,
    val queueSize: Int,
    val librarySize: Int? = null,
    val stackTracks: Int = 0,
    val surfaceOwner: String = "NONE",
    val warnings: List<String> = emptyList(),
    val hasFailure: Boolean = false,
) {
    fun lines(language: AppLanguage): List<String> = buildList {
        fun label(key: String) = diagnosticsText(language, key)
        add("${label("App")}: $version")
        add("${label("Screen")}: $screen")
        add(listOfNotNull(label(if (playing) "Playing" else "Paused"), label(if (video) "Video" else "Audio"),
            "${label("Queue")}: ${queueSize.coerceAtLeast(0)}",
            librarySize?.let { "${label("Library")}: ${it.coerceAtLeast(0)}" }).joinToString(" · "))
        add("Stack: ${if (stackTracks > 0) stackTracks.toString() else label("Off")}")
        add("${label("Surface")}: $surfaceOwner")
        add("${label("Warnings")}: ${warnings.distinct().joinToString(", ").ifEmpty { label("None") }}")
        if (hasFailure) add(label("Local failure record available"))
    }
}

internal fun hasLocalFailure(report: String): Boolean = report.lineSequence().any { it.startsWith("failure=") }

/** Never replace the diagnostic payload with the short on-screen summary. */
internal fun completeDeveloperReport(detailReport: String, regions: List<String>): String = buildString {
    appendLine(detailReport)
    appendLine("ACTIVE REGIONS")
    regions.forEach { appendLine("- $it") }
}

internal fun diagnosticsText(language: AppLanguage, english: String): String {
    val index = when (language) {
        AppLanguage.ENGLISH -> return english
        AppLanguage.TRADITIONAL_CHINESE -> 0
        AppLanguage.CANTONESE -> 1
        AppLanguage.JAPANESE -> 2
        AppLanguage.GERMAN -> 3
        AppLanguage.FRENCH -> 4
    }
    return diagnosticsTranslations[english]?.get(index) ?: english
}

private val diagnosticsTranslations = mapOf(
    "Developer" to listOf("開發者", "開發者", "開発者", "Entwickler", "Développeur"),
    "Needs attention" to listOf("需要留意", "要留意", "要確認", "Prüfung nötig", "À vérifier"),
    "OK · local only" to listOf("正常 · 僅限本機", "正常 · 只限本機", "正常 · 端末内のみ", "OK · nur lokal", "OK · local uniquement"),
    "Close" to listOf("關閉", "關閉", "閉じる", "Schließen", "Fermer"),
    "App" to listOf("應用程式", "App", "アプリ", "App", "Appli"),
    "Screen" to listOf("畫面", "畫面", "画面", "Ansicht", "Écran"),
    "Playing" to listOf("播放中", "播放中", "再生中", "Wiedergabe", "Lecture"),
    "Paused" to listOf("已暫停", "暫停咗", "一時停止", "Pausiert", "En pause"),
    "Video" to listOf("影片", "影片", "動画", "Video", "Vidéo"),
    "Audio" to listOf("音訊", "音訊", "音声", "Audio", "Audio"),
    "Queue" to listOf("佇列", "播放隊列", "キュー", "Warteschlange", "File"),
    "Library" to listOf("音樂庫", "歌庫", "ライブラリ", "Bibliothek", "Bibliothèque"),
    "Off" to listOf("關閉", "關咗", "オフ", "Aus", "Désactivé"),
    "Surface" to listOf("影像輸出", "畫面輸出", "映像出力", "Videoausgabe", "Sortie vidéo"),
    "Warnings" to listOf("警告", "警告", "警告", "Warnungen", "Alertes"),
    "None" to listOf("無", "冇", "なし", "Keine", "Aucune"),
    "Local failure record available" to listOf("有本機錯誤紀錄", "有本機錯誤紀錄", "端末内のエラー記録あり", "Lokaler Fehlerbericht vorhanden", "Rapport d’erreur local disponible"),
    "Floating permission: enabled" to listOf("浮動視窗權限：已開啟", "浮動視窗權限：開咗", "重ねて表示：許可", "Overlay-Berechtigung: erteilt", "Superposition : autorisée"),
    "Floating permission: not granted" to listOf("浮動視窗權限：未開啟", "浮動視窗權限：未開", "重ねて表示：未許可", "Overlay-Berechtigung: fehlt", "Superposition : non autorisée"),
    "Open permission settings" to listOf("開啟權限設定", "開權限設定", "権限設定を開く", "Berechtigungen öffnen", "Ouvrir les autorisations"),
    "Pick an element" to listOf("選取介面元素", "揀介面元素", "要素を選択", "Element auswählen", "Sélectionner un élément"),
    "Technical details" to listOf("技術詳情", "技術詳情", "技術情報", "Technische Details", "Détails techniques"),
    "Hide technical details" to listOf("收起技術詳情", "收埋技術詳情", "技術情報を隠す", "Details ausblenden", "Masquer les détails"),
    "Show region IDs" to listOf("顯示區域代號", "顯示區域代號", "領域IDを表示", "Bereichs-IDs anzeigen", "Afficher les identifiants"),
    "Copy bug report" to listOf("複製錯誤報告", "複製錯誤報告", "不具合報告をコピー", "Fehlerbericht kopieren", "Copier le rapport"),
    "Turn off Developer Mode" to listOf("關閉開發者模式", "關閉開發者模式", "開発者モードをオフ", "Entwicklermodus ausschalten", "Désactiver le mode développeur"),
)
