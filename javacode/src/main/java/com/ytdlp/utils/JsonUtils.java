package com.ytdlp.utils;

import com.google.gson.*;
import java.util.*;

/**
 * JSON工具类 - 使用Gson进行JSON处理
 */
public class JsonUtils {
    
    private static final Gson gson = new Gson();
    
    /**
     * 解析JSON字符串
     */
    public static JsonElement parseJson(String json) {
        try {
            return JsonParser.parseString(json);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取JSON对象中的字符串值
     */
    public static String getString(JsonObject obj, String key, String defaultValue) {
        if (obj == null || !obj.has(key)) {
            return defaultValue;
        }
        JsonElement element = obj.get(key);
        return element.isJsonNull() ? defaultValue : element.getAsString();
    }
    
    /**
     * 获取JSON对象中的长整型值
     */
    public static Long getLong(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement element = obj.get(key);
        return element.isJsonNull() ? null : element.getAsLong();
    }
    
    /**
     * 获取JSON对象中的布尔值
     */
    public static Boolean getBoolean(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement element = obj.get(key);
        return element.isJsonNull() ? null : element.getAsBoolean();
    }
    
    /**
     * 获取JSON对象中的数组
     */
    public static JsonArray getArray(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement element = obj.get(key);
        return element.isJsonArray() ? element.getAsJsonArray() : null;
    }
    
    /**
     * 将对象转换为JSON字符串
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
    
    /**
     * 从JSON字符串解析为指定类型的对象
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return gson.fromJson(json, classOfT);
        } catch (Exception e) {
            return null;
        }
    }
}
