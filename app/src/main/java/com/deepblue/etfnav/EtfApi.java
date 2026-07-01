package com.deepblue.etfnav;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EtfApi {
    private static final String TWSE_QUOTE_API = "https://mis.twse.com.tw/stock/api/getStockInfo.jsp";
    private static final String TWSE_INAV_LISTED = "https://mis.twse.com.tw/stock/various-areas/etf-price/indicator-disclosure-etf?lang=zhHant";
    private static final String TWSE_INAV_OTC = "https://mis.twse.com.tw/stock/various-areas/etf-price/value-disclosure-etf?lang=zhHant";

    // 保留舊呼叫簽名，避免其他檔案大改。apiUrl 參數在直連版已不使用。
    public static EtfItem fetch(String apiUrl, String code) {
        return fetchDirect(code);
    }

    public static EtfItem fetchDirect(String code) {
        EtfItem error = new EtfItem();
        error.code = cleanCode(code);
        if (!isValidCode(error.code)) {
            error.error = "ETF 代碼格式錯誤";
            return error;
        }

        EtfItem item = fetchTwseQuote(error.code);
        if (item.hasError()) return item;

        // 先嘗試直接讀取 TWSE 淨值揭露頁的原始內容。若該頁改成純前端渲染，這一步可能抓不到。
        EtfItem navItem = fetchTwseInavFromStaticPages(error.code, item);
        if (navItem != null) item = navItem;

        if (item.source == null || item.source.isEmpty()) {
            item.source = "TWSE MIS 直連";
        }
        return item;
    }

    private static EtfItem fetchTwseQuote(String code) {
        EtfItem item = new EtfItem();
        item.code = code;
        try {
            // 同時查上市/上櫃，讓使用者只需要輸入代碼。ETF 代碼這件事已經夠煩，不需要再叫人類猜 tse/otc。
            String exCh = "tse_" + code + ".tw|otc_" + code + ".tw";
            String fullUrl = TWSE_QUOTE_API
                    + "?ex_ch=" + URLEncoder.encode(exCh, "UTF-8")
                    + "&json=1&delay=0&_=" + System.currentTimeMillis();
            String body = httpGet(fullUrl, "https://mis.twse.com.tw/stock/fibest.jsp?stock=" + code);
            JSONObject root = new JSONObject(body);
            JSONArray arr = root.optJSONArray("msgArray");
            if (arr == null || arr.length() == 0) {
                item.error = "TWSE 即時報價查無資料";
                return item;
            }
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (code.equals(o.optString("c", "").trim().toUpperCase())) {
                    item.code = code;
                    item.name = o.optString("n", "");
                    item.marketPrice = cleanNumber(firstNonDash(
                            o.optString("z", ""),
                            o.optString("pz", ""),
                            firstLevel(o.optString("a", "")),
                            firstLevel(o.optString("b", ""))
                    ));
                    item.dataDate = o.optString("d", "");
                    item.dataTime = o.optString("t", "");
                    item.source = "TWSE MIS 即時報價";
                    return item;
                }
            }
            item.error = "TWSE 有回應，但找不到 " + code;
            return item;
        } catch (Exception e) {
            item.error = "TWSE 連線失敗：" + e.getMessage();
            return item;
        }
    }

    private static EtfItem fetchTwseInavFromStaticPages(String code, EtfItem base) {
        String[] urls = new String[]{TWSE_INAV_LISTED, TWSE_INAV_OTC};
        for (String url : urls) {
            try {
                String body = httpGet(url, "https://mis.twse.com.tw/stock/");
                EtfItem parsed = parseStandardJsonOrText(body, code, base);
                if (parsed != null && parsed.nav != null && !parsed.nav.isEmpty()) {
                    parsed.source = "TWSE 淨值揭露專區";
                    return parsed;
                }
            } catch (Exception ignored) {
                // 揭露頁常因前端渲染或反爬條件抓不到，先不要讓整筆查詢死亡。軟體世界的小慈悲。
            }
        }
        return null;
    }

    public static EtfItem parseStandardJsonOrText(String body, String code, EtfItem base) {
        if (body == null || body.isEmpty()) return null;
        EtfItem fromJson = parseStandardJson(body, code, base);
        if (fromJson != null) return fromJson;
        return parseTextForNav(body, code, base);
    }

    private static EtfItem parseStandardJson(String body, String code, EtfItem base) {
        try {
            String trimmed = body.trim();
            if (trimmed.startsWith("{")) {
                JSONObject root = new JSONObject(trimmed);
                EtfItem parsed = parseMsgArray(root.optJSONArray("msgArray"), code, base);
                if (parsed != null) return parsed;
            }
        } catch (Exception ignored) {}

        try {
            Pattern p = Pattern.compile("\\{[^{}]*\\\"a\\\"\\s*:\\s*\\\"" + Pattern.quote(code) + "\\\"[^{}]*\\}");
            Matcher m = p.matcher(body);
            if (m.find()) {
                JSONObject o = new JSONObject(m.group());
                return fromInavObject(o, base);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static EtfItem parseMsgArray(JSONArray arr, String code, EtfItem base) {
        if (arr == null) return null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            if (code.equals(o.optString("a", "").trim().toUpperCase())) {
                return fromInavObject(o, base);
            }
        }
        return null;
    }

    private static EtfItem fromInavObject(JSONObject o, EtfItem base) {
        EtfItem item = copyBase(base);
        item.code = o.optString("a", item.code);
        item.name = o.optString("b", item.name);
        item.marketPrice = cleanNumber(o.optString("e", item.marketPrice));
        item.nav = cleanNumber(o.optString("f", item.nav));
        item.premiumDiscount = formatPercent(o.optString("g", item.premiumDiscount));
        item.previousNav = cleanNumber(o.optString("h", item.previousNav));
        item.dataDate = o.optString("i", item.dataDate);
        item.dataTime = o.optString("j", item.dataTime);
        return item;
    }

    public static EtfItem parseTextForNav(String rawText, String code, EtfItem base) {
        if (rawText == null) return null;
        String text = rawText
                .replace('\u00A0', ' ')
                .replace("\r", "\n")
                .replaceAll("<script[\\s\\S]*?</script>", " ")
                .replaceAll("<style[\\s\\S]*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        int idx = text.toUpperCase().indexOf(code.toUpperCase());
        if (idx < 0) return null;

        String near = text.substring(idx, Math.min(text.length(), idx + 1200));
        List<String> decimals = new ArrayList<>();
        Matcher dm = Pattern.compile("[-+]?\\d{1,5}\\.\\d{1,4}%?").matcher(near);
        while (dm.find()) {
            String v = dm.group();
            // 排除太像時間或奇怪座標的東西，雖然網頁文本本來就像被丟進果汁機。
            decimals.add(v);
            if (decimals.size() >= 8) break;
        }
        if (decimals.size() < 2) return null;

        EtfItem item = copyBase(base);
        // TWSE 淨值揭露標準欄位順序：成交價、預估淨值、預估折溢價幅度、前一營業日淨值。
        item.marketPrice = cleanNumber(decimals.get(0));
        item.nav = cleanNumber(decimals.get(1));
        if (decimals.size() >= 3) item.premiumDiscount = formatPercent(decimals.get(2));
        if (decimals.size() >= 4) item.previousNav = cleanNumber(decimals.get(3));

        Matcher date = Pattern.compile("20\\d{6}").matcher(near);
        if (date.find()) item.dataDate = date.group();
        Matcher time = Pattern.compile("\\d{2}:\\d{2}:\\d{2}").matcher(near);
        if (time.find()) item.dataTime = time.group();
        item.source = "TWSE 淨值揭露文字解析";
        return item;
    }

    private static EtfItem copyBase(EtfItem base) {
        EtfItem item = new EtfItem();
        if (base == null) return item;
        item.code = base.code;
        item.name = base.name;
        item.marketPrice = base.marketPrice;
        item.nav = base.nav;
        item.premiumDiscount = base.premiumDiscount;
        item.previousNav = base.previousNav;
        item.dataDate = base.dataDate;
        item.dataTime = base.dataTime;
        item.source = base.source;
        item.error = base.error;
        return item;
    }

    private static String httpGet(String url, String referer) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json,text/html,*/*");
            conn.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.7");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) ETFNavTracker/1.0.2");
            if (referer != null && !referer.isEmpty()) conn.setRequestProperty("Referer", referer);
            int status = conn.getResponseCode();
            InputStream in = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body = readAll(in);
            if (status < 200 || status >= 300) throw new RuntimeException("HTTP " + status);
            return body;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static String cleanCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    public static boolean isValidCode(String code) {
        return code != null && code.matches("^[0-9A-Z]{4,6}$");
    }

    private static String firstNonDash(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v == null) continue;
            String s = v.trim();
            if (!s.isEmpty() && !"-".equals(s) && !"--".equals(s)) return s;
        }
        return "";
    }

    private static String firstLevel(String raw) {
        if (raw == null) return "";
        String[] parts = raw.split("_");
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty() && !"-".equals(s)) return s;
        }
        return "";
    }

    private static String cleanNumber(String s) {
        if (s == null) return "";
        String v = s.trim().replace(",", "");
        if (v.endsWith("%")) v = v.substring(0, v.length() - 1);
        if ("-".equals(v) || "--".equals(v) || "未結出".equals(v)) return "";
        return v;
    }

    private static String formatPercent(String s) {
        String v = cleanNumber(s);
        if (v.isEmpty()) return "";
        return v.endsWith("%") ? v : v + "%";
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        return sb.toString();
    }
}
