# ETF 淨值追蹤 App

Android 原生 Java 版，設計目標是可以直接丟到 GitHub，用 GitHub Actions 編譯 Debug APK。

## 功能

- 使用者自行輸入 ETF 代碼，例如 `0056`、`00878`、`00713`、`00981A`
- 自選清單儲存在手機本機 SharedPreferences
- 可手動更新單檔或全部 ETF
- 每天 18:30 透過 AlarmManager 嘗試背景更新
- API URL 可在 App 首頁輸入與儲存
- 支援兩種 API JSON 格式：
  - 自家後端格式
  - TWSE-style `msgArray` 格式

## App 端期待的自家後端格式

`GET /api/etf/latest?code=0056`

```json
{
  "success": true,
  "code": "0056",
  "name": "元大高股息",
  "market_price": "38.20",
  "nav": "38.05",
  "premium_discount": "+0.39%",
  "previous_nav": "37.98",
  "data_date": "20260701",
  "data_time": "14:35:00",
  "source": "backend"
}
```

查不到：

```json
{
  "success": false,
  "code": "99999",
  "message": "ETF 代碼不存在或目前無淨值資料"
}
```

## GitHub 編譯方式

1. 建立一個新的 GitHub repository
2. 把本專案全部檔案上傳到 repo 根目錄
3. 進入 GitHub repo 的 `Actions`
4. 執行 `Android APK Build`
5. 完成後到 workflow run 的 `Artifacts` 下載 `etf-nav-debug-apk`

## 本機編譯

如果你本機有 Android Studio，直接開啟資料夾即可。若用命令列，需要安裝 Android SDK、JDK 17、Gradle 9.3.1。

```bash
gradle assembleDebug --no-daemon
```

APK 位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 注意

這版先處理 App 與 GitHub 編譯主線。ETF 淨值資料來源建議由後端統一抓 MOPS / TWSE / 投信資料，再提供乾淨 JSON 給 App。不要讓 App 直接到處爬網站，因為那是把未來維護地獄提前搬進手機。
