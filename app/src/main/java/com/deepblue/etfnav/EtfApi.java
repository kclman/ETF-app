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

public class EtfApi {
    public static EtfItem fetch(String apiUrl, String code) {
        EtfItem error = new EtfItem();
        error.code = cleanCode(code);
        if (!isValidCode(error.code)) {
            error.error = "ETF 代碼格式錯誤";
            return error;
        }
        if (apiUrl == null || apiUrl.trim().isEmpty() || apiUrl.contains("your-domain.example")) {
            error.error = "尚未設定後端 API URL";
            return error;
        }

        HttpURLConnection conn = null;
        try {
            String sep = apiUrl.contains("?") ? "&" : "?";
            String fullUrl = apiUrl + sep + "code=" + URLEncoder.encode(error.code, "UTF-8");
            conn = (HttpURLConnection) new URL(fullUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "ETFNavTracker-Android/1.0.0");

            int status = conn.getResponseCode();
            InputStream in = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body = readAll(in);
            if (status < 200 || status >= 300) {
                error.error = "API 回應錯誤 HTTP " + status;
                return error;
            }
            return parseResponse(body, error.code);
        } catch (Exception e) {
            error.error = "連線失敗：" + e.getMessage();
            return error;
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

    private static EtfItem parseResponse(String body, String targetCode) throws Exception {
        JSONObject root = new JSONObject(body);

        if (root.has("success") && !root.optBoolean("success", true)) {
            EtfItem item = new EtfItem();
            item.code = targetCode;
            item.error = root.optString("message", "ETF 代碼不存在或目前無資料");
            return item;
        }

        if (root.has("msgArray")) {
            JSONArray arr = root.optJSONArray("msgArray");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    String code = o.optString("a", "").trim().toUpperCase();
                    if (targetCode.equals(code)) {
                        EtfItem item = EtfItem.fromJson(o);
                        item.code = code;
                        item.source = "TWSE-style msgArray";
                        return item;
                    }
                }
            }
            EtfItem item = new EtfItem();
            item.code = targetCode;
            item.error = "API 有資料，但找不到這檔 ETF";
            return item;
        }

        EtfItem item = EtfItem.fromJson(root);
        if (item.code == null || item.code.isEmpty()) item.code = targetCode;
        if (!targetCode.equals(item.code.trim().toUpperCase())) item.code = targetCode;
        if ((item.nav == null || item.nav.isEmpty()) && (item.marketPrice == null || item.marketPrice.isEmpty())) {
            item.error = "API 格式可讀，但沒有 nav / market_price 欄位";
        }
        return item;
    }

    private static String readAll(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }
}
