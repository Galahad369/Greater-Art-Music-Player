package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage

internal fun uiText(language: AppLanguage, english: String, traditionalChinese: String): String {
    if (language == AppLanguage.ENGLISH) return english
    settingsUiText(language, english)?.let { return it }
    additionalUiText(language, english)?.let { return it }
    if (language == AppLanguage.TRADITIONAL_CHINESE) return traditionalChinese
    if (language == AppLanguage.CANTONESE) return cantonese[english] ?: traditionalChinese
    val index = when (language) { AppLanguage.JAPANESE -> 0; AppLanguage.GERMAN -> 1; else -> 2 }
    translations[english]?.let { return it[index] }
    val number = english.substringBefore(' ').toIntOrNull()
    if (number != null) {
        if (english.endsWith(" min")) return "$number ${listOf("分", "Min.", "min")[index]}"
        if (english.endsWith(" songs")) return "$number ${listOf("曲", "Titel", "titres")[index]}"
        if (english.endsWith(" files • offline")) return "$number ${listOf("ファイル • オフライン", "Dateien • offline", "fichiers • hors ligne")[index]}"
    }
    return english
}

private val cantonese = mapOf(
    "Allow floating player" to "開啟浮動播放器",
    "Enable Display over other apps to use the Library mini-player and Now Playing window. You can keep listening without it." to "開啟「顯示喺其他應用程式上面」，就用到歌庫迷你播放器同正在播放視窗。未開都可以繼續聽歌。",
    "Open permission settings" to "開權限設定",
    "Not now" to "遲啲先",
    "Home" to "返首頁",
    "Play" to "播歌", "Pause" to "暫停", "Previous" to "上一首", "Next" to "下一首",
    "No songs in this list" to "呢個歌單未有歌", "No matches" to "搵唔到", "Search" to "搵歌",
    "Back to library" to "返去歌庫", "Remove all layers" to "移走晒所有音訊層",
    "Parallel playback" to "一齊播", "Opening your library" to "開緊你嘅歌庫",
    "Try a different search." to "試下搵第二個字。", "No media files yet" to "暫時未有歌同影片",
    "Keep settings" to "唔改設定", "Yes, reset settings" to "係，重設晒", "Mix" to "混音",
    "Done" to "搞掂", "Cancel" to "唔搞住", "Save" to "儲存", "Mute" to "閂聲", "Unmute" to "開返聲",
    "Undo" to "反悔返轉頭", "Select multiple" to "一次過揀幾首", "Select matches" to "揀晒搵到嗰啲",
    "Local title & cover" to "改個名、換張封面", "Rule-based playlist" to "自動執歌單",
    "Only changes this app. Your original file stays untouched." to "放心，淨係改呢個 App 嘅顯示，原檔唔會郁。",
    "Playback options" to "播放設定", "A–B practice controls" to "A–B 反覆練歌",
    "Extended local search" to "搵歌勁啲", "Waveform is being prepared; seeking is ready" to "整緊波形，照拖時間軸冇問題",
    "Add to…" to "加去…", "Clear" to "清晒",
    "Toggle Ads On" to "開啟廣告",
    "Optional parody: loud colours, wobbling buttons and a five-second skip. Buttons open a Rickroll in your browser." to "可選惡搞：高飽和配色、晃動按鈕同五秒跳過。按鈕會喺瀏覽器開啟 Rickroll。",
    "ReplayGain" to "ReplayGain",
    "Black disc mode" to "黑膠碟模式",
    "Spin a black vinyl display for audio tracks. Off by default." to "播純音訊嗰陣轉黑膠碟，預設唔開。",
    "Offline play history" to "離線聽歌紀錄", "Play history" to "聽歌紀錄",
    "Record locally" to "淨係本機記", "No local play history" to "未有本機紀錄",
    "Burn history" to "燒晒紀錄", "Burn all play history?" to "燒晒所有聽歌紀錄？",
    "Favorites" to "心水歌", "Add to Favorites" to "加落心水歌", "Remove from Favorites" to "移出心水歌",
    "Share Favorites" to "分享心水歌",
    "No favorites yet. Add them with the ⋮ button." to "仲未有心水歌，用 ⋮ 按鈕加落去。",
    "Share media file" to "分享原檔", "Share playlist" to "分享歌單", "Share current queue" to "分享而家個播放隊列",
    "Share" to "分享", "Current media file" to "而家播緊嗰個檔", "Queue as M3U8" to "播放隊列（M3U8）",
    "Could not open sharing" to "開唔到分享",
    "Share current Library list" to "分享而家個歌庫清單", "Search current queue" to "搵而家個播放隊列",
    "Share selected media files" to "分享揀咗嘅原檔", "Share original media files?" to "分享原始媒體檔？",
    "Share files" to "分享檔案", "Could not share these files" to "分享唔到呢啲檔案",
    "Queue" to "播放佇列",
)

// Bundled offline translations. Keys deliberately match the existing English copy
// so old English/Traditional Chinese call sites keep their established behavior.
private val translations: Map<String, List<String>> = """
Allow floating player|フローティングプレーヤーを許可|Schwebenden Player erlauben|Autoriser le lecteur flottant
Enable Display over other apps to use the Library mini-player and Now Playing window. You can keep listening without it.|「他のアプリの上に表示」を許可すると、ライブラリのミニプレーヤーと再生画面を使えます。許可しなくても音声は再生できます。|Aktiviere „Über anderen Apps einblenden“, um den Mini-Player und das Wiedergabefenster zu nutzen. Ohne diese Erlaubnis kannst du weiter Musik hören.|Activez « Afficher par-dessus les autres applications » pour utiliser le mini-lecteur et la fenêtre de lecture. Vous pouvez continuer à écouter sans cette autorisation.
Open permission settings|権限設定を開く|Berechtigungseinstellungen öffnen|Ouvrir les autorisations
Not now|後で|Nicht jetzt|Pas maintenant
Home|ホーム|Startseite|Accueil
Repeat mode|リピートモード|Wiederholungsmodus|Mode de répétition
Undo|元に戻す|Rückgängig|Annuler l’action
Save|保存|Speichern|Enregistrer
Clear|クリア|Leeren|Effacer
Select multiple|複数選択|Mehrere auswählen|Sélection multiple
Select matches|検索結果を選択|Treffer auswählen|Sélectionner les résultats
Add to…|追加先…|Hinzufügen zu…|Ajouter à…
Playback options|再生オプション|Wiedergabeoptionen|Options de lecture
A–B practice controls|A–Bリピート|A–B-Wiederholung|Répétition A–B
Mark a section to repeat. Turning this off clears the markers.|繰り返す区間を設定。オフにするとマーカーを消去します。|Abschnitt wiederholen. Ausschalten löscht die Markierungen.|Définir un passage à répéter. Désactiver efface les repères.
Extended local search|詳細ローカル検索|Erweiterte lokale Suche|Recherche locale étendue
Search artist, album and lyrics. Builds a local cache in the background; off by default.|アーティスト・アルバム・歌詞を検索。既定はオフ。索引は端末内で作成します。|Künstler, Alben und Texte durchsuchen. Lokaler Index im Hintergrund; standardmäßig aus.|Rechercher artistes, albums et paroles. Index local en arrière-plan ; désactivé par défaut.
Local title & cover|タイトルと画像を変更|Lokaler Titel und Cover|Titre et pochette locaux
Only changes this app. Your original file stays untouched.|このアプリの表示のみ変更。元のファイルは変更しません。|Ändert nur die Anzeige in dieser App. Die Originaldatei bleibt unverändert.|Modifie uniquement l’affichage dans cette application. Le fichier original reste intact.
Display title (blank = original)|表示タイトル（空欄＝元の名前）|Anzeigetitel (leer = Original)|Titre affiché (vide = original)
Use original title and artwork|元のタイトルと画像に戻す|Originaltitel und Cover verwenden|Utiliser le titre et la pochette d’origine
Rule-based playlist|ルール付きプレイリスト|Regelbasierte Wiedergabeliste|Playlist à règles
Updates after scanning. All filled rules must match. No listening history is used.|スキャン後に更新。入力した条件すべてに一致する曲を追加。再生履歴は使いません。|Aktualisiert sich nach dem Scan. Alle Regeln müssen passen. Kein Hörverlauf.|Mise à jour après analyse. Toutes les règles remplies doivent correspondre. Aucun historique d’écoute.
Folder inside Download (optional)|Download内のフォルダー（任意）|Ordner in Download (optional)|Dossier dans Download (facultatif)
Format (optional)|形式（任意）|Format (optional)|Format (facultatif)
Title contains (optional)|タイトルに含む文字（任意）|Titel enthält (optional)|Le titre contient (facultatif)
Waveform is being prepared; seeking is ready|波形を準備中。シークは利用できます|Wellenform wird vorbereitet; Suchen ist möglich|Préparation de la forme d’onde ; navigation disponible
Settings|設定|Einstellungen|Paramètres
Language & appearance|言語と外観|Sprache und Darstellung|Langue et apparence
Language|言語|Sprache|Langue
Theme|テーマ|Design|Thème
System|システム|System|Système
Light|ライト|Hell|Clair
Dark|ダーク|Dunkel|Sombre
Text style|フォント|Schriftart|Police
App background|アプリの背景|App-Hintergrund|Arrière-plan
Default|標準|Standard|Par défaut
Image|画像|Bild|Image
Silent MP4|無音のMP4|Stummes MP4|MP4 muet
Now-playing video|再生中の動画|Aktuelles Video|Vidéo en cours
Custom image|カスタム画像|Eigenes Bild|Image personnelle
Custom background video|背景動画|Eigenes Hintergrundvideo|Vidéo personnelle
Choose image|画像を選択|Bild auswählen|Choisir une image
Change image|画像を変更|Bild ändern|Changer l’image
Choose MP4|MP4を選択|MP4 auswählen|Choisir un MP4
Change MP4|MP4を変更|MP4 ändern|Changer le MP4
Background dimming|背景の暗さ|Hintergrund abdunkeln|Assombrissement
Library row size|リストの行サイズ|Zeilengröße|Taille des lignes
Small|小|Klein|Petite
Medium|中|Mittel|Moyenne
Large|大|Groß|Grande
Show thumbnails|サムネイルを表示|Vorschaubilder anzeigen|Afficher les miniatures
Show file details|ファイル情報を表示|Dateidetails anzeigen|Afficher les détails
Playback|再生|Wiedergabe|Lecture
Playback speed|再生速度|Geschwindigkeit|Vitesse de lecture
Repeat|リピート|Wiederholung|Répétition
One|1曲|Einzeln|Un titre
All|全曲|Alle|Tous
Random|ランダム|Zufall|Aléatoire
Off|オフ|Aus|Désactivé
Show sleep timer|スリープタイマーを表示|Schlaftimer anzeigen|Afficher la minuterie
Sleep|スリープ|Timer|Minuterie
End|終了時|Ende|Fin
End of track|曲の終わり|Titelende|Fin du titre
Resume last position|続きから再生|Position fortsetzen|Reprendre la position
Automatic floating playback|自動フローティング再生|Automatisch schwebend abspielen|Lecteur flottant automatique
Floating window shape|フローティングの形|Form des Minifensters|Forme de la fenêtre flottante
Mini window|ミニウィンドウ|Minifenster|Mini-fenêtre
Compact|コンパクト|Kompakt|Compact
Follow video|動画に合わせる|Videoformat folgen|Suivre le format vidéo
Jump back / forward|戻る／進む|Zurück / Vorwärts|Reculer / Avancer
Editable play queue|再生キューの編集|Warteschlange bearbeiten|Modifier la file
Song lists|プレイリスト|Wiedergabelisten|Listes de lecture
Song list|プレイリスト|Wiedergabeliste|Liste de lecture
Create playlist|プレイリストを作成|Liste erstellen|Créer une liste
Playlist name|プレイリスト名|Listenname|Nom de la liste
Rename playlist|プレイリスト名を変更|Liste umbenennen|Renommer la liste
Rename|名前を変更|Umbenennen|Renommer
Delete playlist?|プレイリストを削除しますか？|Liste löschen?|Supprimer la liste ?
Delete list|リストを削除|Liste löschen|Supprimer la liste
Import M3U|M3Uを読み込む|M3U importieren|Importer M3U
Export list|リストを書き出す|Liste exportieren|Exporter la liste
Library & cache|ライブラリとキャッシュ|Bibliothek und Cache|Bibliothèque et cache
Preload thumbnails|サムネイルを先読み|Vorschaubilder vorladen|Précharger les miniatures
Scan Download again|Downloadを再スキャン|Download erneut durchsuchen|Analyser Download
Rescan|再スキャン|Neu einlesen|Réanalyser
Scan again|再スキャン|Erneut scannen|Analyser à nouveau
Thumbnail cache|サムネイルキャッシュ|Vorschaubild-Cache|Cache des miniatures
Clear cache|キャッシュを削除|Cache leeren|Vider le cache
Privacy|プライバシー|Datenschutz|Confidentialité
Offline by design|完全オフライン設計|Für Offlinebetrieb entwickelt|Conçu hors ligne
Developer diagnostics|開発者診断|Entwicklerdiagnose|Diagnostic développeur
Reset app settings|設定をリセット|Einstellungen zurücksetzen|Réinitialiser les paramètres
Reset every setting?|すべての設定をリセット？|Alle Einstellungen zurücksetzen?|Tout réinitialiser ?
Yes, reset settings|はい、リセット|Ja, zurücksetzen|Oui, réinitialiser
Keep settings|設定を維持|Einstellungen behalten|Conserver les paramètres
Back|戻る|Zurück|Retour
Back to library|ライブラリに戻る|Zur Bibliothek|Retour à la bibliothèque
Close|閉じる|Schließen|Fermer
Cancel|キャンセル|Abbrechen|Annuler
Save|保存|Speichern|Enregistrer
Delete|削除|Löschen|Supprimer
Remove|削除|Entfernen|Retirer
Add|追加|Hinzufügen|Ajouter
Create|作成|Erstellen|Créer
Done|完了|Fertig|Terminé
Play|再生|Abspielen|Lire
Pause|一時停止|Pause|Pause
Previous|前の曲|Vorheriger Titel|Titre précédent
Next|次の曲|Nächster Titel|Titre suivant
Mute|ミュート|Stummschalten|Couper le son
Unmute|ミュート解除|Ton einschalten|Rétablir le son
Search|検索|Suchen|Rechercher
Filter library|ライブラリを検索|Bibliothek filtern|Filtrer la bibliothèque
All songs|すべての曲|Alle Titel|Tous les titres
Sort|並び替え|Sortieren|Trier
Custom order|カスタム順|Eigene Reihenfolge|Ordre personnalisé
Name A–Z|名前 A–Z|Name A–Z|Nom A–Z
Name Z–A|名前 Z–A|Name Z–A|Nom Z–A
Hold + drag to reorder|長押しして並び替え|Zum Sortieren halten und ziehen|Maintenir et glisser pour trier
Play this list|このリストを再生|Diese Liste abspielen|Lire cette liste
Opening your library|ライブラリを開いています|Bibliothek wird geöffnet|Ouverture de la bibliothèque
No songs in this list|このリストに曲はありません|Keine Titel in dieser Liste|Cette liste est vide
No matches|一致する曲はありません|Keine Treffer|Aucun résultat
No media files yet|メディアファイルがありません|Noch keine Mediendateien|Aucun fichier multimédia
Try a different search.|別の検索を試してください。|Versuche einen anderen Suchbegriff.|Essayez une autre recherche.
Allow local file access|ローカルファイルへのアクセスを許可|Lokalen Dateizugriff erlauben|Autoriser les fichiers locaux
Open settings|設定を開く|Einstellungen öffnen|Ouvrir les paramètres
Download folder unavailable|Downloadフォルダーを開けません|Download-Ordner nicht verfügbar|Dossier Download indisponible
Folder access was lost|フォルダーへのアクセス権がありません|Ordnerzugriff verloren|Accès au dossier perdu
Grant file access again, then return to the app.|アクセスを再度許可し、アプリに戻ってください。|Erlaube den Dateizugriff erneut und kehre zurück.|Autorisez à nouveau l’accès puis revenez.
Android will open settings. Enable all-files access, then return here.|Androidの設定で全ファイルへのアクセスを許可し、ここに戻ってください。|Aktiviere in Android den Zugriff auf alle Dateien und kehre zurück.|Activez l’accès à tous les fichiers dans Android puis revenez.
This playlist is empty. Add songs with the ⋮ button.|リストは空です。⋮ボタンで曲を追加してください。|Diese Liste ist leer. Füge Titel über ⋮ hinzu.|Cette liste est vide. Ajoutez des titres avec ⋮.
Open floating player|フローティングプレーヤーを開く|Schwebenden Player öffnen|Ouvrir le lecteur flottant
Album artwork|アルバムアート|Albumcover|Pochette
Loading duration…|再生時間を読み込み中…|Dauer wird geladen…|Chargement de la durée…
Move up|上へ|Nach oben|Monter
Move down|下へ|Nach unten|Descendre
Open system equalizer|システムイコライザーを開く|System-Equalizer öffnen|Ouvrir l’égaliseur système
Excluded Download folders|除外するDownloadフォルダー|Ausgeschlossene Download-Ordner|Dossiers Download exclus
Exclude folder|フォルダーを除外|Ordner ausschließen|Exclure le dossier
Find duplicate files|重複ファイルを探す|Doppelte Dateien finden|Rechercher les doublons
Mix|ミックス|Mix|Mix
Parallel playback|同時再生|Parallele Wiedergabe|Lecture simultanée
Remove all layers|すべての音声レイヤーを削除|Alle Spuren entfernen|Retirer toutes les pistes
The main track plus nine audio layers. Video layers play sound only. Mixing lowers each layer to leave headroom.|メイン曲と最大9音声レイヤー。追加動画は音声のみ。音割れ防止のため各音量を下げます。|Haupttitel plus neun Audiospuren. Zusätzliche Videos liefern nur Ton. Der Mix senkt die Pegel für mehr Reserve.|Titre principal et neuf pistes audio. Les vidéos ajoutées donnent uniquement du son. Le mix réduit les niveaux pour garder de la marge.
English is the default. Changes apply immediately.|標準は英語です。変更はすぐ反映されます。|Englisch ist Standard. Änderungen gelten sofort.|L’anglais est la langue par défaut. Les changements sont immédiats.
Follow Android or keep one appearance.|Androidに合わせるか、外観を固定します。|Android folgen oder ein Design beibehalten.|Suivre Android ou conserver un thème.
Applied immediately and remembered locally.|すぐ反映し、本機に保存します。|Wird sofort angewendet und lokal gespeichert.|Appliqué immédiatement et enregistré localement.
Optional player control. Hidden by default.|任意の再生操作です。標準では非表示です。|Optionale Steuerung, standardmäßig ausgeblendet.|Commande facultative, masquée par défaut.
Repeat one remains the default after reset.|リセット後は1曲リピートが標準です。|Nach dem Zurücksetzen wird ein Titel wiederholt.|La répétition d’un titre reste le réglage par défaut.
Continue the last file where you stopped.|最後のファイルを続きから再生します。|Die letzte Datei an der letzten Position fortsetzen.|Reprendre le dernier fichier où vous l’avez arrêté.
Keep playing in the selected floating mode when leaving the app.|アプリを離れると選択したフローティングモードで再生します。|Beim Verlassen im gewählten schwebenden Modus weiterspielen.|Continuer dans le mode flottant choisi en quittant l’application.
How far the skip buttons move playback.|スキップ操作で移動する時間です。|Sprungweite der Vor- und Zurücktasten.|Durée des sauts avant et arrière.
Display format and file size below the title.|タイトルの下に形式とサイズを表示します。|Format und Dateigröße unter dem Titel anzeigen.|Afficher le format et la taille sous le titre.
Turn off previews for the densest list.|プレビューを消してリストをコンパクトにします。|Vorschauen für eine kompaktere Liste ausblenden.|Masquer les aperçus pour une liste plus compacte.
Warm the first library page for faster scrolling.|先読みしてスクロールを速くします。|Erste Bibliotheksseite für flüssiges Scrollen vorladen.|Précharger la première page pour un défilement fluide.
Refresh the recursive local media index.|ローカルメディアを再帰的に再検索します。|Lokalen Medienindex rekursiv aktualisieren.|Actualiser l’index local de tous les sous-dossiers.
Remove generated previews without touching media files.|メディアを変更せずプレビューだけ削除します。|Erzeugte Vorschauen ohne Mediendateien entfernen.|Supprimer les aperçus sans toucher aux fichiers.
Cache cleared. Previews will be recreated when needed.|キャッシュを削除しました。必要時に再作成します。|Cache geleert. Vorschauen werden bei Bedarf neu erstellt.|Cache vidé. Les aperçus seront recréés au besoin.
Turn off to include again. Files remain untouched.|オフにすると再び含めます。ファイルは変更しません。|Ausschalten, um wieder einzuschließen. Dateien bleiben erhalten.|Désactiver pour inclure à nouveau. Les fichiers sont conservés.
Your playlists, library order, and media files are not changed.|リスト、並び順、メディアファイルは変更しません。|Listen, Reihenfolge und Mediendateien bleiben unverändert.|Les listes, l’ordre et les fichiers restent inchangés.
File selected — access is saved locally.|選択済み。アクセス権は本機に保存します。|Datei ausgewählt – Zugriff lokal gespeichert.|Fichier choisi — accès enregistré localement.
No file selected; the default background is used.|未選択のため標準背景を使用します。|Keine Datei ausgewählt; Standardhintergrund wird genutzt.|Aucun fichier choisi ; le fond par défaut est utilisé.
Video wallpaper follows the current track across pages. Audio uses liquid metal. Wallpaper is always muted.|動画背景はページを移動しても続きます。音声曲は液体金属背景になります。背景は常に無音です。|Das Video folgt dem aktuellen Titel über alle Seiten. Audio nutzt Flüssigmetall. Das Hintergrundvideo bleibt stumm.|La vidéo suit le titre entre les pages. L’audio utilise le métal liquide. Le fond reste muet.
Silian Rail is the final reversible font choice.|Silian Railはいつでも戻せるフォントです。|Silian Rail lässt sich jederzeit wieder abwählen.|Silian Rail peut être désactivé à tout moment.
Changes the full row and thumbnail. Small is the default.|行とサムネイルの大きさを変更します。標準は小です。|Ändert Zeile und Vorschaubild. Klein ist Standard.|Modifie la ligne et la miniature. Petite par défaut.
Darkens custom media so titles and controls remain readable.|背景を暗くし、文字や操作を見やすくします。|Dunkelt Medien ab, damit Text und Steuerung lesbar bleiben.|Assombrit le fond pour garder les commandes lisibles.
Show queue editing controls on the player. Off by default to keep playback clean.|プレーヤーにキュー編集操作を表示します。標準ではオフです。|Zeigt die Bearbeitung der Warteschlange. Standardmäßig aus.|Affiche les commandes de modification de la file. Désactivé par défaut.
Create local playlists, then add songs with the ⋮ button in the library.|リストを作成し、ライブラリの⋮から曲を追加します。|Lokale Listen erstellen und Titel über ⋮ hinzufügen.|Créez des listes locales puis ajoutez des titres avec ⋮.
Add all songs containing (optional)|含む曲をすべて追加（任意）|Alle passenden Titel hinzufügen (optional)|Ajouter les titres contenant (facultatif)
Also adds the song you opened.|開いていた曲も追加します。|Fügt auch den geöffneten Titel hinzu.|Ajoute aussi le titre ouvert.
Create + add matches|作成して一致する曲を追加|Erstellen und Treffer hinzufügen|Créer et ajouter les résultats
e.g. example|例：キーワード|z. B. Suchwort|Ex. : mot-clé
This restores Mini window, dark theme, Repeat One and every other preference. Playlists and media files stay safe.|ミニウィンドウ、ダークテーマ、1曲リピートなどを標準に戻します。リストとファイルは保持します。|Stellt Minifenster, dunkles Design, Titelwiederholung und weitere Vorgaben wieder her. Listen und Dateien bleiben erhalten.|Rétablit la mini-fenêtre, le thème sombre, la répétition et les autres réglages. Les listes et fichiers sont conservés.
Mini window is the tiniest option. Compact and Follow video use Android's resizable picture-in-picture.|ミニウィンドウが最小です。他のモードはAndroidのサイズ変更可能な小窓を使用します。|Minifenster ist am kleinsten. Die anderen Modi nutzen Androids skalierbares Bild-in-Bild.|La mini-fenêtre est la plus petite. Les autres modes utilisent l’image dans l’image redimensionnable d’Android.
When the current track is a video, a muted synchronized copy appears behind the interface. Audio tracks fall back to liquid metal.|動画の再生中は同期した無音の背景を表示します。音声曲では液体金属に戻ります。|Videos erscheinen synchron und stumm im Hintergrund. Bei Audio erscheint Flüssigmetall.|Une copie vidéo synchronisée et muette apparaît derrière l’interface. L’audio utilise le métal liquide.
Use track gain tags with peak protection. Untagged files play unchanged; boosting needs a peak tag and device support.|曲のゲインとピークのタグを使用します。タグなしは変更せず、増幅にはピークタグと対応機器が必要です。|Nutzt Titel-Gain mit Spitzenschutz. Ohne Tags unverändert; Verstärkung benötigt Peak-Tags und Geräteunterstützung.|Utilise les gains avec protection des crêtes. Sans tags, aucun changement. L’amplification nécessite un tag de crête et un appareil compatible.
Adds a local DEV panel with live screen, player, queue and permission details. Nothing is transmitted.|画面、プレーヤー、キュー、権限の診断を本機で表示します。送信はしません。|Zeigt lokale Diagnose für Ansicht, Player, Warteschlange und Berechtigungen. Keine Übertragung.|Affiche un diagnostic local de l'écran, du lecteur, de la file et des autorisations. Aucune transmission.
"No Internet permission, ads, analytics, account, telemetry, or cloud library. Everything stays on this device."|ネット権限、広告配信、分析、アカウント、テレメトリー、クラウドはありません。すべて本機に保存します。|Keine Internetberechtigung, Werbenetzwerke, Analyse, Konten oder Cloud-Bibliothek. Alles bleibt auf dem Gerät.|Aucune autorisation Internet, régie publicitaire, analyse, compte ou bibliothèque cloud. Tout reste sur cet appareil.
Toggle Ads On|広告をオンにする|Werbung aktivieren|Activer les pubs
Optional parody: loud colours, wobbling buttons and a five-second skip. Buttons open a Rickroll in your browser.|任意のパロディ：派手な色、揺れるボタン、5秒スキップ。ボタンでブラウザが Rickroll を開きます。|Optionale Parodie: greller Farben, wackelnde Buttons und ein 5-Sekunden-Skip. Buttons öffnen einen Rickroll im Browser.|Parodie optionnelle : couleurs criardes, boutons qui tremblent et saut de 5 s. Les boutons ouvrent un Rickroll dans votre navigateur.
ReplayGain|ReplayGain|ReplayGain|ReplayGain
Favorites|お気に入り|Favoriten|Favoris
Share Favorites|お気に入りを共有|Favoriten teilen|Partager les favoris
No favorites yet. Add them with the ⋮ button.|まだお気に入りはありません。⋮ボタンから追加できます。|Noch keine Favoriten. Füge sie über ⋮ hinzu.|Aucun favori. Ajoutez-en avec le bouton ⋮.
Add to Favorites|お気に入りに追加|Zu Favoriten hinzufügen|Ajouter aux favoris
Remove from Favorites|お気に入りから削除|Aus Favoriten entfernen|Retirer des favoris
Share media file|メディアファイルを共有|Mediendatei teilen|Partager le fichier multimédia
Share|共有|Teilen|Partager
Current media file|現在のメディアファイル|Aktuelle Mediendatei|Fichier multimédia actuel
Queue as M3U8|キューをM3U8として共有|Warteschlange als M3U8|File au format M3U8
Could not open sharing|共有を開けませんでした|Teilen konnte nicht geöffnet werden|Impossible d’ouvrir le partage
Share playlist|プレイリストを共有|Wiedergabeliste teilen|Partager la playlist
Share current queue|現在のキューを共有|Aktuelle Warteschlange teilen|Partager la file actuelle
Share current Library list|現在のライブラリリストを共有|Aktuelle Bibliotheksliste teilen|Partager la liste actuelle
Search current queue|現在のキューを検索|Aktuelle Warteschlange durchsuchen|Rechercher dans la file actuelle
Could not share this file|このファイルを共有できませんでした|Diese Datei konnte nicht geteilt werden|Impossible de partager ce fichier
Could not share this list|このリストを共有できませんでした|Diese Liste konnte nicht geteilt werden|Impossible de partager cette liste
Current queue|現在のキュー|Aktuelle Warteschlange|File actuelle
Current Library list|現在のライブラリリスト|Aktuelle Bibliotheksliste|Liste actuelle
Share selected media files|選択したメディアファイルを共有|Ausgewählte Mediendateien teilen|Partager les fichiers sélectionnés
Share original media files?|元のメディアファイルを共有しますか？|Originaldateien teilen?|Partager les fichiers originaux ?
Share files|ファイルを共有|Dateien teilen|Partager les fichiers
Queue|キュー|Warteschlange|File d’attente
Could not share these files|これらのファイルを共有できませんでした|Diese Dateien konnten nicht geteilt werden|Impossible de partager ces fichiers
""".trimIndent().lineSequence().filter { it.isNotBlank() }.associate { line ->
    val parts = line.split('|')
    require(parts.size == 4) { "Invalid bundled translation row" }
    parts.first() to parts.drop(1)
}
