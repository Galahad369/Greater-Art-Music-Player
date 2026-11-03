package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage

/** New privacy copy has an explicit translation in every supported language. */
internal enum class TrustCopy(private vararg val translations: String) {
    TITLE("Trust & privacy", "信任與私隱", "信頼とプライバシー", "Vertrauen und Datenschutz", "Confiance et confidentialité"),
    BUILD("Installed build", "已安裝版本", "インストール済みビルド", "Installierter Build", "Version installée"),
    COMPATIBILITY("Compatibility sideload identity; not a production publisher certificate.", "相容側載簽署身分，並非正式發行者憑證。", "互換サイドロード用の署名です。製品版の発行者証明書ではありません。", "Kompatible Sideload-Signatur; kein Produktionszertifikat.", "Signature de compatibilité pour installation directe, pas un certificat de production."),
    UNVERIFIED("Signing identity is not recognized here. Compare its fingerprint with the release receipt.", "此處未識別簽署身分，請與發行紀錄的指紋比對。", "署名の識別は未確認です。リリース記録の指紋と比較してください。", "Signaturidentität hier nicht bestätigt. Fingerabdruck mit dem Freigabenachweis vergleichen.", "Identité de signature non confirmée ici. Comparez son empreinte au relevé de publication."),
    DEBUG("Debuggable build", "可偵錯版本", "デバッグ可能", "Debug-Build", "Version débogable"),
    OPTIMIZED("Non-debuggable build", "不可偵錯版本", "デバッグ不可", "Nicht debuggbarer Build", "Version non débogable"),
    CERTIFICATE("Certificate SHA-256", "憑證 SHA-256", "証明書 SHA-256", "Zertifikat SHA-256", "Certificat SHA-256"),
    NETWORK("Network access", "網絡存取", "ネットワークアクセス", "Netzwerkzugriff", "Accès réseau"),
    NO_INTERNET("Installed APK has no Internet permission. External sharing and browser apps have their own network access.", "已安裝 APK 沒有網絡權限。外部分享及瀏覽器應用程式有各自的網絡存取權。", "インストール済みAPKにインターネット権限はありません。共有先やブラウザーの通信は別です。", "Das installierte APK hat keine Internetberechtigung. Ziel- und Browser-Apps können selbst online sein.", "L’APK installé n’a pas d’autorisation Internet. Les applications de partage et le navigateur peuvent accéder au réseau."),
    INTERNET_PRESENT("Internet permission is present in this APK. Do not treat it as an offline-verified build.", "此 APK 包含網絡權限，不應視為已驗證離線版本。", "このAPKにはインターネット権限があります。オフライン検証済みとして扱わないでください。", "Dieses APK enthält eine Internetberechtigung. Es ist nicht als offline geprüft zu behandeln.", "Cet APK possède une autorisation Internet. Ne le considérez pas comme une version hors ligne vérifiée."),
    ACCESS("File and overlay access", "檔案與浮動視窗權限", "ファイルとオーバーレイ", "Datei- und Overlayzugriff", "Fichiers et superposition"),
    BROAD("All Files Access is declared by this build", "此版本宣告所有檔案存取權", "このビルドは全ファイルアクセスを宣言", "Dieser Build deklariert Zugriff auf alle Dateien", "Cette version déclare l’accès à tous les fichiers"),
    LEGACY_READ("Legacy shared-storage read permission", "舊版共用儲存空間讀取權限", "旧形式の共有ストレージ読取権限", "Ältere Leseberechtigung für gemeinsamen Speicher", "Ancienne autorisation de lecture du stockage partagé"),
    GRANTED("Granted", "已允許", "許可済み", "Erlaubt", "Autorisé"),
    DENIED("Not granted", "未允許", "未許可", "Nicht erlaubt", "Non autorisé"),
    FOLDER("Selected media folder", "已選媒體資料夾", "選択したフォルダー", "Ausgewählter Medienordner", "Dossier multimédia sélectionné"),
    NO_FOLDER("No selected-folder grant; the existing Download mode is used.", "尚無所選資料夾權限，使用原有下載資料夾模式。", "選択フォルダーの権限はありません。従来のDownloadモードです。", "Keine ausgewählte Ordnerfreigabe; bisheriger Download-Modus aktiv.", "Aucun dossier autorisé ; le mode Téléchargements existant est utilisé."),
    OVERLAY("Floating player permission", "浮動播放器權限", "フローティング再生の権限", "Berechtigung für schwebenden Player", "Autorisation du lecteur flottant"),
    PERMISSIONS("Manage Android permissions", "管理 Android 權限", "Android権限を管理", "Android-Berechtigungen verwalten", "Gérer les autorisations Android"),
    FOLDERS("Media folders", "媒體資料夾", "メディアフォルダー", "Medienordner", "Dossiers multimédias"),
    STORAGE("Preferences, generated caches and reports stay in app-private storage. Original media files are separate.", "設定、產生的快取及報告儲存在應用程式私有空間，原始媒體檔案另外保存。", "設定・生成キャッシュ・レポートはアプリ専用領域に保存します。元のメディアは別です。", "Einstellungen, erzeugte Caches und Berichte bleiben im privaten App-Speicher. Originalmedien sind getrennt.", "Préférences, caches générés et rapports restent dans le stockage privé de l’application. Les médias originaux sont séparés."),
    HISTORY("Local listening history", "本機播放紀錄", "端末内の再生履歴", "Lokaler Hörverlauf", "Historique d’écoute local"),
    HISTORY_NOTE("Optional and off by default. Turning it off stops new records; clearing removes saved records, not songs.", "可選，預設關閉。關閉後停止新增紀錄；清除只移除紀錄，不刪除歌曲。", "任意で既定はオフです。オフで記録を停止し、消去は曲を削除しません。", "Optional, standardmäßig aus. Ausschalten stoppt neue Einträge; Löschen entfernt keine Titel.", "Facultatif, désactivé par défaut. Désactiver arrête les nouveaux enregistrements ; effacer ne supprime aucun morceau."),
    CLEAR_HISTORY("Clear listening history", "清除播放紀錄", "再生履歴を消去", "Hörverlauf löschen", "Effacer l’historique d’écoute"),
    DIAGNOSTICS("Local diagnostics", "本機診斷", "端末内の診断", "Lokale Diagnose", "Diagnostics locaux"),
    DIAGNOSTICS_NOTE("No automatic uploads. Reports omit messages and media paths. Preview before sharing. Clearing does not erase Android’s own system exit records.", "不會自動上傳。報告不含錯誤訊息或媒體路徑。分享前請預覽。清除不會移除 Android 系統本身的結束紀錄。", "自動送信はありません。メッセージやメディアパスを除外します。共有前に確認してください。Androidの終了記録は消去しません。", "Kein automatischer Upload. Berichte enthalten keine Meldungen oder Medienpfade. Vor Teilen prüfen. Android-Systemprotokolle bleiben bestehen.", "Aucun envoi automatique. Les rapports excluent messages et chemins des médias. Vérifiez avant de partager. Les journaux système Android restent intacts."),
    PREVIEW("Preview report", "預覽報告", "レポートを確認", "Bericht ansehen", "Aperçu du rapport"),
    CLEAR("Clear local report", "清除本機報告", "端末内レポートを消去", "Lokalen Bericht löschen", "Effacer le rapport local"),
    SHARE("Open sharing", "開啟分享", "共有を開く", "Teilen öffnen", "Ouvrir le partage"),
    EMPTY("No exportable local report.", "沒有可匯出的本機報告。", "共有できる端末内レポートはありません。", "Kein exportierbarer lokaler Bericht.", "Aucun rapport local exportable."),
    CONFIRM_CLEAR("Remove only the local diagnostic report? Your media and preferences stay unchanged.", "只移除本機診斷報告？媒體與設定不會更改。", "診断レポートのみ消去しますか？メディアと設定は変更しません。", "Nur den lokalen Diagnosebericht entfernen? Medien und Einstellungen bleiben unverändert.", "Supprimer uniquement le rapport local ? Médias et préférences restent inchangés."),
    CLEARED("Local diagnostic report cleared.", "已清除本機診斷報告。", "端末内レポートを消去しました。", "Lokaler Diagnosebericht gelöscht.", "Rapport local effacé."),
    FAILED("Could not complete this action. No report was sent automatically.", "無法完成操作，沒有自動傳送報告。", "操作できませんでした。自動送信はしていません。", "Aktion fehlgeschlagen. Kein Bericht wurde automatisch gesendet.", "Impossible de terminer. Aucun rapport n’a été envoyé automatiquement."),
    LIMITS("A valid signature identifies the signing key, not a guarantee of safety. Full security assessment and physical-device acceptance remain separate checks.", "有效簽署可識別簽署金鑰，但不保證安全。完整安全評估與實體裝置驗收仍是獨立檢查。", "有効な署名は鍵を識別しますが、安全性を保証しません。完全な安全性評価と実機確認は別です。", "Eine gültige Signatur identifiziert den Schlüssel, garantiert aber keine Sicherheit. Vollständige Sicherheitsprüfung und Gerätetests sind separate Schritte.", "Une signature valide identifie la clé, sans garantir la sécurité. L’évaluation complète et les essais sur appareils réels restent distincts."),
    UNAVAILABLE("Installed build details could not be read. Nothing is marked verified.", "無法讀取已安裝版本資料，不會標示為已驗證。", "ビルド情報を読み取れませんでした。検証済みとは表示しません。", "Build-Informationen nicht lesbar. Nichts wird als geprüft markiert.", "Informations de version illisibles. Aucun élément n’est marqué comme vérifié.");

    fun text(language: AppLanguage): String = translations[when (language) {
        AppLanguage.ENGLISH -> 0
        AppLanguage.TRADITIONAL_CHINESE, AppLanguage.CANTONESE -> 1
        AppLanguage.JAPANESE -> 2
        AppLanguage.GERMAN -> 3
        AppLanguage.FRENCH -> 4
    }]
}
