package com.i5mc.tilemgr.v2;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import lombok.val;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public enum L2Pool {

    INSTANCE;

    private final Cache<String, Object> pool = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    private final Object invalid = new Object();

    @SneakyThrows
    public static <T> T find(String key, Supplier<T> supplier) {
        val l = INSTANCE.pool.get(key, () -> {
            val i = supplier.get();
            return i == null ? INSTANCE.invalid : i;
        });
        return l == INSTANCE.invalid ? null : (T) l;
    }

}
