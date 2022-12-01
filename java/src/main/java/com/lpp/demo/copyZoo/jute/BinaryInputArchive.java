package com.lpp.demo.copyZoo.jute;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BinaryInputArchive implements InputArchive {

    // CHECKSTYLE.OFF: ConstantName - for backward compatibility
    public static final int maxBuffer = Integer.getInteger("jute.maxbuffer", 0xfffff);
    // CHECKSTYLE.ON:
    private static final int extraMaxBuffer;

    static {
        final Integer configuredExtraMaxBuffer =
                Integer.getInteger("zookeeper.jute.maxbuffer.extrasize", maxBuffer);
        if (configuredExtraMaxBuffer < 1024) {
            // Earlier hard coded value was 1024, So the value should not be less than that value
            extraMaxBuffer = 1024;
        } else {
            extraMaxBuffer = configuredExtraMaxBuffer;
        }
    }

    private final DataInput in;
    private final int maxBufferSize;
    private final int extraMaxBufferSize;

    public static BinaryInputArchive getArchive(InputStream stream) {
        return new BinaryInputArchive(new DataInputStream(stream));
    }

    public BinaryInputArchive(DataInput in) {
        this(in, maxBuffer, extraMaxBuffer);
    }

    public BinaryInputArchive(DataInput in, int maxBufferSize, int extraMaxBufferSize) {
        this.in = in;
        this.maxBufferSize = maxBufferSize;
        this.extraMaxBufferSize = extraMaxBufferSize;
    }

    @Override
    public byte readByte(String tag) throws IOException {
        return 0;
    }

    @Override
    public boolean readBool(String tag) throws IOException {
        return false;
    }

    @Override
    public int readInt(String tag) throws IOException {
        return 0;
    }

    @Override
    public long readLong(String tag) throws IOException {
        return 0;
    }

    @Override
    public float readFloat(String tag) throws IOException {
        return 0;
    }

    @Override
    public double readDouble(String tag) throws IOException {
        return 0;
    }

    @Override
    public String readString(String tag) throws IOException {
        return null;
    }

    @Override
    public byte[] readBuffer(String tag) throws IOException {
        return new byte[0];
    }

    @Override
    public void readRecord(Record r, String tag) throws IOException {

    }

    @Override
    public void startRecord(String tag) throws IOException {

    }

    @Override
    public void endRecord(String tag) throws IOException {

    }

    @Override
    public Index startVector(String tag) throws IOException {
        return null;
    }

    @Override
    public void endVector(String tag) throws IOException {

    }

    @Override
    public Index startMap(String tag) throws IOException {
        return null;
    }

    @Override
    public void endMap(String tag) throws IOException {

    }
}
