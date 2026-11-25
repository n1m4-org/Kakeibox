# Kakeibox

Android向けのシンプルな家計簿アプリです。

## 機能
- **クイック入力**: ＋ボタンで下からシートが開き、金額を打ってカテゴリをタップするだけで今日の支出を記録（Snackbarから取り消し可、⌫長押しで全消去）
- **ホーム画面ウィジェット**
  - 支出クイック入力: 今月の支出合計を表示し、「＋ 支出を追加」でアプリを開かずにクイック入力
  - 今日の予算: 今日の残り・使用率バー・使用額を表示（＋からクイック入力）
- **明細**: 収入・支出を記録し、月ごと・日付ごとに一覧表示（タップで編集・削除）
- **月次サマリ**: 収入・支出・収支を表示
- **予算管理**: 月の予算を設定すると、残額と使用率をバーで表示（80%で黄、超過で赤）
- **1日の予算**: 固定額＋繰り越し方式。明細画面の「今日」カードに今日の残り・使用率を表示
  - 今日の予算 = 1日の予算 ＋ 繰り越し（前日までの「1日の予算 − その日の支出」の合計）
  - 繰り越しは月初、または1日の予算を変更した日から数え直し
- **レポート**: カテゴリ別の内訳をドーナツグラフと割合で表示（支出・収入を切替）
- カテゴリは線画（Material Outlined）アイコンで表示
- データは端末内（Room / SQLite）に保存

## 技術構成
| 項目 | 内容 |
|---|---|
| 言語 | Kotlin 2.0 |
| UI | Jetpack Compose (Material 3) |
| DB | Room |
| 画面遷移 | Navigation Compose |
| ウィジェット | Jetpack Glance |
| 対応OS | Android 8.0 (API 26) 以上 |

```
app/src/main/java/com/kakeibox/
├── MainActivity.kt          画面遷移（明細 / レポート / 入力）＋クイック入力シート
├── QuickAddActivity.kt      ウィジェットから開くクイック入力（透明Activity）
├── AppGraph.kt              Repositoryの生成（保存時にウィジェットを更新）
├── widget/                  ホーム画面ウィジェット
├── data/                    Entity・DAO・DB・集計ロジック・予算保存
└── ui/
    ├── MainViewModel.kt     表示中の月・明細・集計を StateFlow で提供
    ├── quickadd/            クイック入力（キーパッド・カテゴリ選択）
    ├── screens/             HomeScreen / EditScreen / ReportScreen
    └── components/          月切替・ドーナツグラフ
```

## ビルド
Android Studio でプロジェクトを開いて実行するか、コマンドラインで以下を実行します。

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # 単体テスト
```
