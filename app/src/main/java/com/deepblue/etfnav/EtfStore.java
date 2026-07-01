package com.deepblue.etfnav;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EtfStore {
    private static final String PREF = "etf_nav_store";
    private static final String KEY_CODES = "watch_codes";
    private static final String KEY_API_URL = "api_url";
    public static final String DEFAULT_API_URL = "https://your-domain.example/api/etf/latest";

    private final SharedPreferences sp;

    public EtfStore(Context context) {
        sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public String getApiUrl() {
        return sp.getString(KEY_API_URL, DEFAULT_API_URL);
    }

    public void setApiUrl(String url) {
        sp.edit().putString(KEY_API_URL, url == null ? DEFAULT_API_URL : url.trim()).apply();
    }

    public List<String> getCodes() {
        String raw = sp.getString(KEY_CODES, "[]");
        List<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                String code = arr.optString(i, "").trim().toUpperCase();
                if (!code.isEmpty()) list.add(code);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public void addCode(String code) {
        Set<String> set = new LinkedHashSet<>(getCodes());
        set.add(code.trim().toUpperCase());
        saveCodes(new ArrayList<>(set));
    }

    public void removeCode(String code) {
        List<String> list = getCodes();
        list.remove(code.trim().toUpperCase());
        saveCodes(list);
    }

    private void saveCodes(List<String> codes) {
        JSONArray arr = new JSONArray();
        for (String c : codes) arr.put(c);
        sp.edit().putString(KEY_CODES, arr.toString()).apply();
    }

    public void saveItem(EtfItem item) {
        try {
            sp.edit().putString("item_" + item.code, item.toJson().toString()).apply();
        } catch (Exception ignored) {}
    }

    public EtfItem getItem(String code) {
        String raw = sp.getString("item_" + code.trim().toUpperCase(), null);
        if (raw == null) return null;
        try {
            return EtfItem.fromJson(new JSONObject(raw));
        } catch (Exception e) {
            return null;
        }
    }
}
