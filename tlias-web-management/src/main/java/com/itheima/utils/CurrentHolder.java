package com.itheima.utils;

public class CurrentHolder {
    private static final ThreadLocal<Integer> currentId = new ThreadLocal<>();

    public static void setId(Integer id) {
        currentId.set(id);
    }

    public static Integer getId() {
        return currentId.get();
    }

    public static void remove() {
        currentId.remove();
    }
}
