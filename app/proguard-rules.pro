# DB・設定・バックアップ・CSV に名前で保存している列挙型（TxType・PaymentMethod・ThemeMode など）や
# データのクラスは、縮小で名前が変わると以前のデータを読めなくなるため、そのまま残す
-keep enum com.kakeibox.** { *; }
-keep class com.kakeibox.data.** { *; }
