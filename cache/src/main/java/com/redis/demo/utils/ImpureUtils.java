package com.redis.demo.utils;

import org.jetbrains.annotations.TestOnly;

import java.util.Date;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static java.util.Objects.isNull;

/**
 * 可支持测试的非纯函数库
 */
@SuppressWarnings("WeakerAccess")
public final class ImpureUtils {

    private static volatile Supplier<Date> dateGenerator = TrueDateGenerator.INSTANCE;
    private static volatile LongSupplier currentTimeMillisGenerator = TrueCurrentTimeMillisGenerator.INSTANCE;

    public static Date currentDate() {
        return dateGenerator.get();
    }

    public static long currentTimeMillis() {
        return currentTimeMillisGenerator.getAsLong();
    }

    @TestOnly
    public static void setCurrentDate(Date date) {
        if (isNull(date)) {
            dateGenerator = TrueDateGenerator.INSTANCE;
        } else {
            dateGenerator = () -> date;
        }
    }

    @TestOnly
    public static void setCurrentDate(long millis) {
        if (millis <= 0) {
            setCurrentDate(null);
        } else {
            setCurrentDate(new Date(millis));
        }
    }

    @TestOnly
    public static void setCurrentTimeMillis(long mill) {
        if (mill <= 0) {
            currentTimeMillisGenerator = TrueCurrentTimeMillisGenerator.INSTANCE;
        } else {
            currentTimeMillisGenerator = () -> mill;
        }
    }

    @TestOnly
    public static void resetAll() {
        setCurrentDate(null);
        setCurrentTimeMillis(0);
    }

    static class TrueDateGenerator implements Supplier<Date> {
        static Supplier<Date> INSTANCE = new TrueDateGenerator();

        @Override
        public Date get() {
            return new Date();
        }
    }

    static class TrueCurrentTimeMillisGenerator implements LongSupplier {
        static LongSupplier INSTANCE = new TrueCurrentTimeMillisGenerator();

        @Override
        public long getAsLong() {
            return System.currentTimeMillis();
        }
    }
}

