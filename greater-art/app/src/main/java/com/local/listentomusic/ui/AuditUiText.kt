package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage

/** Interaction results introduced by the UI audit; no English fallback for supported locales. */
internal fun auditUiText(language: AppLanguage, english: String): String? {
    val values = auditMessages[english] ?: return null
    return values[when (language) {
        AppLanguage.TRADITIONAL_CHINESE -> 0
        AppLanguage.CANTONESE -> 1
        AppLanguage.JAPANESE -> 2
        AppLanguage.GERMAN -> 3
        AppLanguage.FRENCH -> 4
        else -> return english
    }]
}

private val auditMessages = mapOf(
    "Backup saved" to listOf("設定已備份", "設定備份咗", "設定をバックアップしました", "Sicherung gespeichert", "Sauvegarde enregistrée"),
    "Could not save backup" to listOf("無法儲存備份", "儲存唔到備份", "バックアップを保存できませんでした", "Sicherung konnte nicht gespeichert werden", "Impossible d’enregistrer la sauvegarde"),
    "Settings and playlists restored" to listOf("設定及播放清單已還原", "設定同歌單還原咗", "設定とプレイリストを復元しました", "Einstellungen und Playlists wiederhergestellt", "Paramètres et listes restaurés"),
    "Invalid or unreadable backup" to listOf("備份無效或無法讀取", "備份唔啱或者讀唔到", "無効または読み取れないバックアップ", "Ungültige oder unlesbare Sicherung", "Sauvegarde invalide ou illisible"),
    "Could not update document access. Please retry." to listOf("無法更新文件存取權限，請重試。", "改唔到文件權限，請再試。", "ファイルのアクセス権を更新できません。再試行してください。", "Dateizugriff konnte nicht aktualisiert werden. Bitte erneut versuchen.", "Impossible de modifier l’accès au document. Réessayez."),
    "Use a relative folder path without . or .. or :" to listOf("請輸入相對資料夾路徑，不可包含 .、.. 或 :", "用相對資料夾路徑，唔好加 .、.. 或 :", ".、..、: を含まない相対フォルダーパスを入力してください", "Relativen Ordnerpfad ohne . oder .. oder : verwenden", "Utilisez un chemin relatif sans . ni .. ni :"),
    "Clearing cache…" to listOf("正在清除快取…", "清緊快取…", "キャッシュを削除中…", "Cache wird geleert…", "Suppression du cache…"),
    "Could not clear cache. Please retry." to listOf("無法清除快取，請重試。", "清唔到快取，請再試。", "キャッシュを削除できません。再試行してください。", "Cache konnte nicht geleert werden. Bitte erneut versuchen.", "Impossible de vider le cache. Réessayez."),
    "Paused" to listOf("已暫停", "暫停咗", "一時停止", "Pausiert", "En pause"),
    "Could not read playlist" to listOf("無法讀取播放清單", "讀唔到歌單", "プレイリストを読み込めません", "Playlist konnte nicht gelesen werden", "Impossible de lire la liste"),
    "No matching files in playlist" to listOf("播放清單沒有相符的檔案", "歌單冇相符檔案", "プレイリストに一致するファイルがありません", "Keine passenden Dateien in der Playlist", "Aucun fichier correspondant dans la liste"),
    "Playlist imported" to listOf("播放清單已匯入", "歌單匯入咗", "プレイリストをインポートしました", "Playlist importiert", "Liste importée"),
    "Could not import playlist" to listOf("無法匯入播放清單", "匯入唔到歌單", "プレイリストをインポートできません", "Playlist konnte nicht importiert werden", "Impossible d’importer la liste"),
    "Playlist exported" to listOf("播放清單已匯出", "歌單匯出咗", "プレイリストをエクスポートしました", "Playlist exportiert", "Liste exportée"),
    "Could not export playlist" to listOf("無法匯出播放清單", "匯出唔到歌單", "プレイリストをエクスポートできません", "Playlist konnte nicht exportiert werden", "Impossible d’exporter la liste"),
)
