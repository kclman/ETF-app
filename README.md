# ETF NAV Tracker v1.0.2 Direct Web

Android App，可自行輸入 ETF 代碼，App 會直接上網讀取 TWSE 即時報價與 ETF 淨值揭露頁，不再需要自己架後端 API。

## v1.0.2 重點

- 移除 API URL 欄位。
- 使用者輸入 ETF 代碼即可加入自選，例如 `0056`、`00878`、`00981A`。
- 先查 TWSE MIS 即時報價 API：`https://mis.twse.com.tw/stock/api/getStockInfo.jsp`。
- 再嘗試查 TWSE ETF 淨值揭露頁：
  - 上市 ETF：`https://mis.twse.com.tw/stock/various-areas/etf-price/indicator-disclosure-etf?lang=zhHant`
  - 上櫃 ETF：`https://mis.twse.com.tw/stock/various-areas/etf-price/value-disclosure-etf?lang=zhHant`
- 單檔更新時若 HTTP 原始內容抓不到淨值，會用隱藏 WebView 載入 TWSE 淨值揭露頁，再從頁面文字解析代碼附近的欄位。
- 每天 18:30 仍會自動更新，但背景更新不啟動 WebView，只做 HTTP 直連更新。

## 顯示欄位

- ETF 代碼
- ETF 名稱
- 市價
- 預估淨值
- 折溢價
- 前日淨值
- 資料日期 / 時間
- 來源

## GitHub Actions 編譯

GitHub 只會偵測 repo 根目錄底下的：

```text
.github/workflows/android-build.yml
```

正確根目錄應該長這樣：

```text
ETF-app/
├── .github/workflows/android-build.yml
├── app/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

不要把整個資料夾多包一層上傳，也不要只上傳 zip。GitHub 不會替你拆禮物，畢竟它不是你親戚。

## 編譯方式

1. 把本專案內容放到 GitHub repo 根目錄。
2. 進入 GitHub `Actions`。
3. 選 `Android APK Build`。
4. 點 `Run workflow`。
5. 編譯完成後，到 Artifacts 下載 `etf-nav-debug-apk`。

## 注意

TWSE 淨值揭露頁是網頁型資料，不是保證永遠穩定的公開 API。v1.0.2 已做 HTTP + WebView 雙路徑，但如果 TWSE 改版，解析規則可能仍需要更新。這就是直接爬網站的代價，技術債不會消失，只會換一張臉回來要錢。
