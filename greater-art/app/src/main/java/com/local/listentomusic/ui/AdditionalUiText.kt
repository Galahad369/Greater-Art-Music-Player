package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage

/** Newer graph and safety controls, bundled locally with all supported languages. */
internal fun additionalUiText(language: AppLanguage, key: String): String? {
    val row = additionalTranslations[key] ?: return null
    return row[when (language) {
        AppLanguage.ENGLISH -> 0
        AppLanguage.TRADITIONAL_CHINESE, AppLanguage.CANTONESE -> 1
        AppLanguage.JAPANESE -> 2
        AppLanguage.GERMAN -> 3
        AppLanguage.FRENCH -> 4
    }]
}

internal val additionalTranslations = """
Align by sound|按聲音對齊|音で位置合わせ|Nach Klang ausrichten|Aligner par le son
Aligning by sound|正在按聲音對齊|音を解析中|Klang wird analysiert|Analyse du son
Uncertain matches kept unchanged|未能確認的匹配保持不變|不確かな一致は変更しません|Unsichere Treffer bleiben unverändert|Correspondances incertaines inchangées
Stack changed; align again|疊播已改變，請重新對齊|ミックスが変わりました。再解析してください|Mix geändert; erneut ausrichten|Mix modifié ; réalignez
Could not analyse these files|無法分析這些檔案|ファイルを解析できません|Dateien konnten nicht analysiert werden|Impossible d’analyser ces fichiers
Reset alignment|重設對齊|位置合わせをリセット|Ausrichtung zurücksetzen|Réinitialiser l’alignement
Stack|疊播|重ねて再生|Mehrfachmix|Mix simultané
Stack together|一齊播放|まとめて重ねる|Zusammen mischen|Mixer ensemble
Add track|加入歌曲|曲を追加|Titel hinzufügen|Ajouter un titre
Tap to add track|按此加入歌曲|タップして曲を追加|Tippen, um Titel hinzuzufügen|Touchez pour ajouter un titre
Add local track|加入本機歌曲|ローカル曲を追加|Lokalen Titel hinzufügen|Ajouter un titre local
Primary visual|主要畫面|メイン映像|Hauptbild|Image principale
Make primary|設為主要|メインにする|Als Hauptbild|Définir comme image principale
Ended|已播完|終了|Beendet|Terminé
Mute|靜音|ミュート|Stumm|Couper le son
Unmute|取消靜音|ミュート解除|Ton an|Rétablir le son
Solo|獨奏|ソロ|Solo|Solo
Unsolo|取消獨奏|ソロ解除|Solo aus|Quitter solo
Remove track|移除歌曲|曲を削除|Titel entfernen|Retirer le titre
Play together|一齊播放|まとめて再生|Zusammen abspielen|Lire ensemble
Could not add this track|無法加入這首歌曲|この曲を追加できません|Titel konnte nicht hinzugefügt werden|Impossible d’ajouter ce titre
Could not start these files on this device|這部裝置無法播放這些檔案|この端末では再生できません|Dateien konnten auf diesem Gerät nicht gestartet werden|Lecture impossible sur cet appareil
Playback unavailable|無法播放|再生できません|Wiedergabe nicht verfügbar|Lecture indisponible
Pause Stack|暫停疊播|重ね再生を一時停止|Mix pausieren|Mettre le mix en pause
Play Stack|播放疊播|重ね再生を開始|Mix abspielen|Lire le mix
Stop|停止|停止|Stopp|Arrêter
Nodes|關聯圖|関連グラフ|Verbindungen|Connexions
Restore settings and playlists?|還原設定與播放清單？|設定とプレイリストを復元しますか？|Einstellungen und Wiedergabelisten wiederherstellen?|Restaurer les réglages et playlists ?
Choose backup|選擇備份|バックアップを選択|Sicherung auswählen|Choisir une sauvegarde
The selected backup replaces portable settings and playlists. Media files remain unchanged.|所選備份將取代可攜式設定與播放清單，不會更改媒體檔案。|選択したバックアップで設定とプレイリストを置き換えます。メディアは変更しません。|Die Sicherung ersetzt übertragbare Einstellungen und Wiedergabelisten. Mediendateien bleiben unverändert.|La sauvegarde remplace les réglages transférables et les playlists. Les médias restent inchangés.
Liquid metal|液態金屬|リキッドメタル|Flüssigmetall|Métal liquide
Back up settings|備份設定|設定をバックアップ|Einstellungen sichern|Sauvegarder les réglages
Restore backup|還原備份|バックアップを復元|Sicherung wiederherstellen|Restaurer une sauvegarde
Folder / subfolder|資料夾 / 子資料夾|フォルダー / サブフォルダー|Ordner / Unterordner|Dossier / sous-dossier
← Library|← 音樂庫|← ライブラリ|← Bibliothek|← Bibliothèque
Retry|重試|再試行|Erneut versuchen|Réessayer
Preparing filename connections…|正在建立檔名關聯…|ファイル名の関連を準備中…|Dateinamen werden verknüpft…|Préparation des connexions…
Your library is empty. Add media in Download, then refresh Library.|音樂庫是空的。在 Download 加入媒體後重新整理。|Downloadにメディアを追加して更新してください。|Medien in Download ablegen und die Bibliothek aktualisieren.|Ajoutez des médias dans Download puis actualisez la bibliothèque.
Fit|完整顯示|全体表示|Einpassen|Ajuster
Find|尋找|検索|Suchen|Rechercher
Playing|播放中|再生中|Aktueller Titel|En lecture
Controls|控制|調整|Steuerung|Réglages
Animate nodes|展開節點|ノードを展開|Knoten animieren|Animer les nœuds
Pinch to zoom · Drag to move · Tap to play|雙指縮放 · 拖曳移動 · 輕觸播放|ピンチで拡大 · ドラッグで移動 · タップで再生|Zoomen · Ziehen zum Verschieben · Tippen zum Abspielen|Pincer pour zoomer · Glisser pour déplacer · Toucher pour lire
Find a node|尋找節點|ノードを検索|Knoten suchen|Trouver un nœud
Filename|檔案名稱|ファイル名|Dateiname|Nom du fichier
Close|關閉|閉じる|Schließen|Fermer
Graph controls|關聯圖設定|グラフ設定|Graph-Einstellungen|Réglages du graphe
Connection strength|關聯強度|関連の強さ|Verbindungsstärke|Force des connexions
Node size|節點大小|ノードの大きさ|Knotengröße|Taille des nœuds
Link visibility|連線可見度|線の濃さ|Liniensichtbarkeit|Visibilité des liens
Link length|連線長度|線の長さ|Verbindungslänge|Longueur des liens
Node repulsion|節點排斥力|ノードの反発力|Abstoßung|Répulsion des nœuds
Elasticity|彈性|弾性|Elastizität|Élasticité
Filename labels|檔名標籤|ファイル名を表示|Dateinamen anzeigen|Afficher les noms
Hide isolated nodes|隱藏獨立節點|孤立ノードを非表示|Isolierte Knoten ausblenden|Masquer les nœuds isolés
Size by strong connections|依強關聯調整大小|強い関連に応じた大きさ|Größe nach starken Verbindungen|Taille selon les connexions fortes
The double-ring node is playing. Larger nodes have more strong filename connections.|雙環節點為播放中曲目。節點越大代表檔名強關聯越多。|二重リングは再生中です。大きいノードほど強い関連が多くあります。|Der Doppelring zeigt den aktuellen Titel. Große Knoten haben mehr starke Verbindungen.|Le double anneau indique le titre en lecture. Les grands nœuds ont davantage de connexions fortes.
Restore graph defaults|還原關聯圖預設值|グラフ設定を初期化|Graph zurücksetzen|Réinitialiser le graphe
Apply|套用|適用|Anwenden|Appliquer
Cancel|取消|キャンセル|Abbrechen|Annuler
Background fit|背景適配|背景の表示方法|Hintergrundanpassung|Ajustement du fond
Choose how custom images and videos fill the screen. Cut to screen size is the default.|選擇背景圖片與影片如何填滿螢幕。預設為裁切填滿。|背景の表示方法を選択。標準は画面に合わせて切り抜きます。|Darstellung von Bildern und Videos wählen. Standard ist bildschirmfüllender Zuschnitt.|Choisir l’affichage des images et vidéos. Le recadrage est utilisé par défaut.
Background|背景|背景|Hintergrund|Arrière-plan
Color theme|色彩主題|カラーテーマ|Farbthema|Thème de couleur
Choose an accent palette while keeping your light/dark mode preference.|在保留淺色／深色模式偏好的同時，選擇你喜歡的色彩調性。|ライト/ダーク設定を保ったまま、アクセント配色を選びます。|Akzentpalette wählen, ohne Hell/Dunkel-Einstellung zu ändern.|Choisissez une palette d’accent sans changer le mode clair/sombre.
Luna|Luna|Luna|Luna|Luna
Orbit|Orbit|Orbit|Orbit|Orbit
Sol|Sol|Sol|Sol|Sol
Astra|Astra|Astra|Astra|Astra
Nova|Nova|Nova|Nova|Nova
Space Black|Space Black|Space Black|Space Black|Space Black
Library & lists|音樂庫與清單|ライブラリとリスト|Bibliothek und Listen|Bibliothèque et listes
Privacy & data|私隱與資料|プライバシーとデータ|Datenschutz und Daten|Confidentialité et données
Developer|開發者|開発者|Entwickler|Développeur
Stretch|拉伸|引き伸ばす|Strecken|Étirer
Cut to screen size|裁切填滿|画面に合わせて切り抜く|Bildschirmfüllend zuschneiden|Recadrer pour remplir
Clear filter|清除篩選|検索をクリア|Filter löschen|Effacer le filtre
Close player|關閉播放器|プレーヤーを閉じる|Player schließen|Fermer le lecteur
Continue|繼續|続行|Weiter|Continuer
DELETE FILE|刪除檔案|ファイルを削除|DATEI LÖSCHEN|SUPPRIMER LE FICHIER
Delete original file|刪除原始檔案|元のファイルを削除|Originaldatei löschen|Supprimer le fichier original
Delete this file?|要刪除此檔案嗎？|このファイルを削除しますか？|Diese Datei löschen?|Supprimer ce fichier ?
Deleting…|正在刪除…|削除中…|Wird gelöscht…|Suppression…
Exit fullscreen|離開全螢幕|全画面を終了|Vollbild beenden|Quitter le plein écran
File was not deleted|檔案未刪除|ファイルは削除されませんでした|Datei wurde nicht gelöscht|Le fichier n’a pas été supprimé
Final confirmation|最後確認|最終確認|Letzte Bestätigung|Confirmation finale
Fullscreen|全螢幕|全画面|Vollbild|Plein écran
I understand|我明白|理解しました|Verstanden|Je comprends
Newest first|最新優先|新しい順|Neueste zuerst|Plus récents d’abord
Oldest first|最舊優先|古い順|Älteste zuerst|Plus anciens d’abord
Reading Download locally|正在本機讀取 Download|Downloadを端末内で読み込み中|Download wird lokal gelesen|Lecture locale de Download
There is no undo inside Greater Art.|Greater Art 內無法復原。|Greater Art内では元に戻せません。|In Greater Art kann dies nicht rückgängig gemacht werden.|Cette action ne peut pas être annulée dans Greater Art.
This image provider cannot keep access. Choose a local image.|無法保留此圖片的存取權，請選擇本機圖片。|この画像へのアクセスは保持できません。ローカル画像を選んでください。|Zugriff kann nicht gespeichert werden. Lokales Bild auswählen.|L’accès ne peut pas être conservé. Choisissez une image locale.
This removes the original|這會刪除原始檔案|元のファイルが削除されます|Dies löscht das Original|Ceci supprime l’original
Waveform unavailable; seeking still works|無法讀取波形，仍可拖曳播放位置|波形は利用できませんがシークは可能です|Keine Wellenform; Suchen ist weiterhin möglich|Forme d’onde indisponible ; navigation toujours possible
Sans serif|無襯線|サンセリフ|Serifenlos|Sans empattement
Serif|襯線|セリフ|Serifenschrift|Avec empattement
Monospace|等寬|等幅|Festbreitenschrift|Chasse fixe
Cursive|手寫|筆記体|Schreibschrift|Cursive
Black disc mode|黑膠唱片模式|ブラックディスクモード|Schwarze Schallplatte|Mode disque noir
Spin a black vinyl display for audio tracks. Off by default.|音訊曲目顯示平滑旋轉的黑膠唱片，預設關閉。|音声トラックで黒いレコードを滑らかに回転表示します。初期設定はオフです。|Zeigt bei Audiotiteln eine sanft rotierende schwarze Schallplatte. Standardmäßig aus.|Affiche un vinyle noir tournant doucement pour les pistes audio. Désactivé par défaut.
Offline play history|離線播放紀錄|オフライン再生履歴|Offline-Wiedergabeverlauf|Historique hors ligne
Off by default. When enabled, played tracks and times stay only on this device and are never included in backups.|預設關閉。啟用後，播放曲目與時間只會留在此裝置，且永遠不會加入備份。|初期設定はオフです。有効にすると、再生した曲と時刻は端末内だけに保存され、バックアップには含まれません。|Standardmäßig aus. Titel und Zeiten bleiben nur auf diesem Gerät und werden nie gesichert.|Désactivé par défaut. Les titres et heures restent sur cet appareil et ne sont jamais sauvegardés.
Play history|播放紀錄|再生履歴|Wiedergabeverlauf|Historique de lecture
Record locally|只在本機記錄|端末内に記録|Lokal aufzeichnen|Enregistrer localement
Disabled by default. History is never exported in settings backups.|預設關閉，播放紀錄永遠不會匯出到設定備份。|初期設定はオフです。履歴は設定バックアップに出力されません。|Standardmäßig aus. Der Verlauf wird nie mit den Einstellungen exportiert.|Désactivé par défaut. L’historique n’est jamais exporté avec les réglages.
No local play history|尚無本機播放紀錄|ローカル再生履歴はありません|Kein lokaler Wiedergabeverlauf|Aucun historique local
Burn history|燒毀紀錄|履歴を焼却|Verlauf verbrennen|Brûler l’historique
Burn all play history?|燒毀所有播放紀錄？|再生履歴をすべて焼却しますか？|Gesamten Wiedergabeverlauf verbrennen?|Brûler tout l’historique ?
This permanently removes the local record. Media files and playlists are unchanged.|這會永久移除本機紀錄，媒體檔案與播放清單不會變更。|ローカル記録を完全に削除します。メディアとプレイリストは変更されません。|Entfernt den lokalen Verlauf dauerhaft. Medien und Wiedergabelisten bleiben unverändert.|Supprime définitivement l’historique local. Les médias et playlists ne changent pas.
""".trimIndent().lineSequence().filter(String::isNotBlank).associate { line ->
    val fields = line.split('|')
    require(fields.size == 5 && fields.none(String::isBlank)) { "Incomplete offline translation" }
    fields[0] to fields
}
