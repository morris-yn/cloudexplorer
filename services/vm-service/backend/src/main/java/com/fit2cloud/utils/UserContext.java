package com.fit2cloud.utils;

import java.util.HashMap;
import java.util.Map;

public class UserContext {
    private static final ThreadLocal<Map> userHolder = new ThreadLocal<>();
    private static final Map<String,Object> map = new HashMap<>();

    public static void setUser(Object user) {
        map.put("user",user);
        userHolder.set(map);
    }

    public static Object getUser() {
        return userHolder.get().get("user");
    }

    public static void setToken(Object token) {
        map.put("token",token);
        userHolder.set(map);
    }

    public static String getToken() {
        return (String) userHolder.get().get("token");
    }

    public static void clear() {
        userHolder.remove();
    }
}
