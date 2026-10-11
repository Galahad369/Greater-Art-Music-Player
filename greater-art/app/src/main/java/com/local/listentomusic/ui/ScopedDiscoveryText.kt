package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.DiscoveryProblem

internal fun scopedText(language: AppLanguage, en: String, zh: String, ja: String,
    de: String, fr: String, yue: String): String = when (language) {
    AppLanguage.ENGLISH -> en
    AppLanguage.TRADITIONAL_CHINESE -> zh
    AppLanguage.JAPANESE -> ja
    AppLanguage.GERMAN -> de
    AppLanguage.FRENCH -> fr
    AppLanguage.CANTONESE -> yue
}

internal fun scopedScanLabel(language: AppLanguage) = scopedText(language,
    "Inspect indexed Downloads", "檢查下載資料夾媒體索引", "ダウンロードの索引を確認",
    "Download-Index prüfen", "Inspecter l’index des téléchargements", "睇吓下載媒體索引")

internal fun scopedSelectedFolderText(language: AppLanguage) = scopedText(language,
    "Folder selected; read access is checked during inspection.", "已選擇資料夾；檢查時會確認讀取權限。",
    "フォルダ選択済み。確認時に読み取り許可を検証します。", "Ordner ausgewählt; Leseberechtigung wird bei der Prüfung kontrolliert.",
    "Dossier sélectionné ; l’autorisation sera vérifiée lors de l’inspection.", "揀咗資料夾；檢查嗰陣會確認讀取權限。")

internal fun scopedScanningText(language: AppLanguage) = scopedText(language,
    "Scanning authorized media (metadata only)…", "正在掃描已授權媒體（只讀取資料）…",
    "許可されたメディアを確認中（メタデータのみ）…", "Autorisierte Medien werden geprüft (nur Metadaten)…",
    "Analyse des médias autorisés (métadonnées uniquement)…", "掃緊已授權媒體（只讀資料）…")

internal fun scopedResultText(language: AppLanguage, media: Int, inspected: Int) = scopedText(language,
    "$media media files indexed; $inspected entries inspected. Library playback is unchanged.",
    "已建立 $media 個媒體索引；已檢查 $inspected 個項目。音樂庫播放維持不變。",
    "$media 件のメディアを索引化、$inspected 件を確認。ライブラリの再生は変更していません。",
    "$media Medien indexiert; $inspected Einträge geprüft. Die Bibliothekswiedergabe bleibt unverändert.",
    "$media médias indexés ; $inspected entrées examinées. La lecture de la bibliothèque reste inchangée.",
    "搵到 $media 個媒體；睇咗 $inspected 個項目。歌庫播放冇改。")

internal fun scopedProblemText(language: AppLanguage, problem: DiscoveryProblem): String = when (problem) {
    DiscoveryProblem.PERMISSION_DENIED -> scopedText(language,
        "Read permission missing or revoked; select the folder again.", "讀取權限已遺失或撤銷；請重新選擇資料夾。",
        "読み取り許可がありません。フォルダを再選択してください。", "Leseberechtigung fehlt oder wurde widerrufen. Ordner erneut wählen.",
        "Autorisation de lecture absente ou révoquée. Sélectionnez à nouveau le dossier.", "讀取權限冇咗；請再揀資料夾。")
    DiscoveryProblem.PROVIDER_UNAVAILABLE -> scopedText(language,
        "Provider unavailable; retry the scan.", "資料提供者無法使用；請重試掃描。", "プロバイダを利用できません。再試行してください。",
        "Anbieter nicht verfügbar. Scan wiederholen.", "Fournisseur indisponible. Relancez l’analyse.", "資料提供者用唔到；試多次掃描。")
    DiscoveryProblem.INVALID_ROOT -> scopedText(language,
        "Invalid folder selection; choose an authorized folder.", "資料夾選擇無效；請選擇已授權資料夾。", "フォルダ選択が無効です。許可されたフォルダを選択してください。",
        "Ungültiger Ordner. Autorisierten Ordner wählen.", "Dossier non valide. Choisissez un dossier autorisé.", "揀嘅資料夾唔啱；請揀已授權資料夾。")
    DiscoveryProblem.LIMIT_REACHED -> scopedText(language,
        "Partial result: scan limit reached. Select a smaller folder.", "部分結果：已達掃描上限。請選擇較小的資料夾。", "一部の結果のみ：走査上限です。小さいフォルダを選択してください。",
        "Teilergebnis: Scan-Limit erreicht. Kleineren Ordner wählen.", "Résultat partiel : limite atteinte. Choisissez un dossier plus petit.", "只搵到部分：到咗掃描上限。揀個細啲嘅資料夾。")
}
