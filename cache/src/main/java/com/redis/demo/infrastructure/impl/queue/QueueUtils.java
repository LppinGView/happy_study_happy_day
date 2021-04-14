package com.redis.demo.infrastructure.impl.queue;

/**
 * @author zmh
 */
public final class QueueUtils {

    private QueueUtils() {
    }

    public static int sequenceToScore(int sequence, int priority) {
        priority &= 0x07;
        priority = priority << 28;
        sequence &= 0x0fff_ffff;
        sequence |= priority;
        return sequence;
    }

    public static int scoreToPriority(int score) {
        score >>= 28;
        return score & 0x07;
    }

    public static int scoreToSequence(int score) {
        return score & 0x0fff_ffff;
    }

    public static long sequenceToScore(long sequence, long priority) {
        priority &= 0x0FFFL;
        priority = priority << 40;
        sequence &= 0x000F_FFff_ffff_ffffL;
        sequence |= priority;
        return sequence;
    }

    public static long scoreToPriority(long score) {
        score >>= 40;
        return score & 0x0FFFL;
    }

    public static long scoreToSequence(long score) {
        return score & 0x000F_FFff_ffff_ffffL;
    }

}
