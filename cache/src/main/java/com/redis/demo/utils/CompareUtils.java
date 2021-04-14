package com.redis.demo.utils;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author hupeng.net@hotmail.com
 */
@SuppressWarnings("WeakerAccess")
public class CompareUtils {

    //region areEquals, Integer/int

    /**
     * 请使用 {@link CompareUtils#notNullAndEquals}或者 {@link CompareUtils#nullOrEquals }明确指定两个Integer在null的情况下是否相等
     *
     * @deprecated 请使用 {@link CompareUtils#notNullAndEquals}或者 {@link CompareUtils#nullOrEquals }明确指定两个Integer在null的情况下是否相等
     */
    @SuppressWarnings("unused")
    @Deprecated
    public static boolean areEquals(Integer a, Integer b) {
        throw new UnsupportedOperationException("请使用notNullAndEquals或者nullOrEquals明确指定两个Integer在null的情况下是否相等");
    }

    public static boolean areEquals(Integer a, int b) {
        return a != null && a == b;
    }

    public static boolean areEquals(int a, Integer b) {
        return areEquals(b, a);
    }

    public static boolean areEquals(int a, int b) {
        return a == b;
    }

    public static boolean notNullAndEquals(Integer a, Integer b) {
        return a != null && a.equals(b);
    }

    public static boolean nullOrEquals(Integer a, Integer b) {
        return Objects.equals(a, b);
    }
    //endregion

    //region areEquals, Long/long

    /**
     * 请使用 {@link CompareUtils#notNullAndEquals}或者 {@link CompareUtils#nullOrEquals }明确指定两个Long在null的情况下是否相等
     *
     * @deprecated 请使用 {@link CompareUtils#notNullAndEquals}或者 {@link CompareUtils#nullOrEquals }明确指定两个Long在null的情况下是否相等
     */
    @SuppressWarnings("unused")
    @Deprecated
    public static boolean areEquals(Long a, Long b) {
        throw new UnsupportedOperationException("请使用notNullAndEquals或者nullOrEquals明确指定两个Long在null的情况下是否相等");
    }

    public static boolean areEquals(Long a, long b) {
        return a != null && a == b;
    }

    public static boolean areEquals(long a, Long b) {
        return areEquals(b, a);
    }

    public static boolean areEquals(long a, long b) {
        return a == b;
    }

    public static boolean notNullAndNotEquals(Long a, Long b) {
        return !(a == null || a.equals(b));
    }

    public static boolean notNullAndEquals(Long a, Long b) {
        return a != null && a.equals(b);
    }

    public static boolean nullOrEquals(Long a, Long b) {
        return Objects.equals(a, b);
    }
    //endregion

    //region greaterThan, Integer/int
    public static boolean greaterThan(int a, int b) {
        return a > b;
    }

    public static boolean greaterThan(Integer a, int b) {
        return a != null && a > b;
    }

    public static boolean greaterThan(int a, Integer b) {
        return b != null && a > b;
    }

    public static boolean greaterThan(Integer a, Integer b) {
        return a != null && b != null && a > b;
    }

    public static boolean greaterThanZero(int a) {
        return a > 0;
    }

    public static boolean greaterThanZero(Integer a) {
        return greaterThan(a, 0);
    }
    //endregion

    //region greaterThan, Long/long
    public static boolean greaterThan(long a, long b) {
        return a > b;
    }

    public static boolean greaterThan(Long a, long b) {
        return a != null && a > b;
    }

    public static boolean greaterThan(long a, Long b) {
        return b != null && a > b;
    }

    public static boolean greaterThan(Long a, Long b) {
        return a != null && b != null && a > b;
    }

    public static boolean greaterThanZero(long a) {
        return a > 0;
    }

    public static boolean greaterThanZero(Number a) {
        return a != null && a.longValue() > 0;
    }
    //endregion

    //region greaterThan, BigDecimal/int
    public static boolean greaterThan(BigDecimal a, int b) {
        return a != null && a.compareTo(BigDecimal.valueOf(b)) > 0;
    }

    public static boolean greaterThanZero(BigDecimal a) {
        return greaterThan(a, 0);
    }

    public static boolean lessThan(BigDecimal a, int b) {
        return a != null && a.compareTo(BigDecimal.valueOf(b)) < 0;
    }

    public static boolean lessOrEqual(BigDecimal a, int b) {
        return a != null && a.compareTo(BigDecimal.valueOf(b)) <= 0;
    }

    public static boolean lessThanZero(BigDecimal a) {
        return lessThan(a, 0);
    }

    public static boolean greaterThan(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) > 0;
    }

    public static boolean greaterOrEqual(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) >= 0;
    }
    //endregion

    //region greaterOrEqual, Integer/int
    public static boolean greaterOrEqual(int a, int b) {
        return a >= b;
    }

    public static boolean greaterOrEqual(Integer a, int b) {
        return a != null && a >= b;
    }

    public static boolean greaterOrEqual(int a, Integer b) {
        return b != null && a >= b;
    }

    public static boolean greaterOrEqual(Integer a, Integer b) {
        return a != null && b != null && a >= b;
    }

    public static boolean greaterOrEqualZero(int a) {
        return a >= 0;
    }

    public static boolean greaterOrEqualZero(Integer a) {
        return greaterOrEqual(a, 0);
    }
    //endregion

    //region greaterOrEqual, Long/long
    public static boolean greaterOrEqual(long a, long b) {
        return a >= b;
    }

    public static boolean greaterOrEqual(Long a, long b) {
        return a != null && a >= b;
    }

    public static boolean greaterOrEqual(long a, Long b) {
        return b != null && a >= b;
    }

    public static boolean greaterOrEqual(Long a, Long b) {
        return a != null && b != null && a >= b;
    }

    public static boolean greaterOrEqualZero(long a) {
        return a >= 0;
    }

    public static boolean greaterOrEqualZero(Long a) {
        return greaterOrEqual(a, 0);
    }
    //endregion

    //region lessThan Integer/int
    public static boolean lessThan(int a, int b) {
        return a < b;
    }

    public static boolean lessThan(Integer a, int b) {
        return a != null && a < b;
    }

    public static boolean lessThan(int a, Integer b) {
        return b != null && a < b;
    }

    public static boolean lessThan(Integer a, Integer b) {
        return a != null && b != null && a < b;
    }

    public static boolean lessThanZero(int a) {
        return a < 0;
    }

    public static boolean lessThanZero(Integer a) {
        return lessThan(a, 0);
    }
    //endregion

    //region lessThan Long/long
    public static boolean lessThan(long a, long b) {
        return a < b;
    }

    public static boolean lessThan(Long a, long b) {
        return a != null && a < b;
    }

    public static boolean lessThan(long a, Long b) {
        return b != null && a < b;
    }

    public static boolean lessThan(Long a, Long b) {
        return a != null && b != null && a < b;
    }

    public static boolean lessThanZero(long a) {
        return a < 0;
    }

    public static boolean lessThanZero(Long a) {
        return lessThan(a, 0);
    }
    //endregion

    //region lessOrEqual Integer/int
    public static boolean lessOrEqual(int a, int b) {
        return a <= b;
    }

    public static boolean lessOrEqual(Integer a, int b) {
        return a != null && a <= b;
    }

    public static boolean lessOrEqual(int a, Integer b) {
        return b != null && a <= b;
    }

    public static boolean lessOrEqual(Integer a, Integer b) {
        return a != null && b != null && a <= b;
    }

    public static boolean lessOrEqualZero(int a) {
        return a <= 0;
    }

    public static boolean lessOrEqualZero(Integer a) {
        return lessOrEqual(a, 0);
    }
    //endregion

    //region lessOrEqual Long/long
    public static boolean lessOrEqual(long a, long b) {
        return a <= b;
    }

    public static boolean lessOrEqual(Long a, long b) {
        return a != null && a <= b;
    }

    public static boolean lessOrEqual(long a, Long b) {
        return b != null && a <= b;
    }

    public static boolean lessOrEqual(Long a, Long b) {
        return a != null && b != null && a <= b;
    }

    public static boolean lessOrEqualZero(long a) {
        return a <= 0;
    }

    public static boolean lessOrEqualZero(Long a) {
        return lessOrEqual(a, 0);
    }
    //endregion


    public static boolean isNullOrZero(Integer a) {
        return null == a || 0 == a;
    }

    public static boolean isNullOrLessEqualsZero(Long a) {
        return null == a || a <= 0;
    }

    public static boolean isNullOrLessEqualsZero(Number a) {
        return null == a || a.longValue() <= 0;
    }

    public static boolean isNullOrLessEqualsZero(BigDecimal a) {
        return null == a || a.compareTo(BigDecimal.ZERO) <= 0;
    }

    public static boolean isNullOrLessEqualsZero(Integer a) {
        return null == a || a <= 0;
    }

    public static boolean areEqual(NamedEnum a, NamedEnum b) {
        return a != null && a.equals(b);
    }

    public static boolean areEqual(NamedEnum a, int b) {
        return a != null && a.getId() == b;
    }

}
