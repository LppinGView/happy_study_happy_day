package com.lpp.demo.nio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * New Io 面向缓冲区，线程再读写时，不会阻塞
 *
 * 1.Channel 是对原IO包中的流的模拟，可以通过它读取和写入数据，通道是双向的，可以用于读、写或者同时用于读写
 * 包括以下通道：
 *  FileChannel：从文件中读写数据；
 *  DatagramChannel：通过 UDP 读写网络中数据；
 *  SocketChannel：通过 TCP 读写网络中数据；
 *  ServerSocketChannel：可以监听新进来的 TCP 连接，对每一个新进来的连接都会创建一个 SocketChannel。
 *
 * 2.不会直接对通道进行读写数据，而是要先经过缓冲区。
 * 缓冲区包括以下类型：
 *  ByteBuffer
 *  CharBuffer
 *  ShortBuffer
 *  IntBuffer
 *  LongBuffer
 *  FloatBuffer
 *  DoubleBuffer
 *
 *
 *  SocketChannel  -->  ByteBuffer  -->  网卡
 *                <--   ByteBuffer  <--
 */

public class NIODemo {


    /**
     * 使用通道读写文件
     * @param src
     * @param dist
     * @throws IOException
     */
    public static void fastCopy(String src, String dist) throws IOException {

        /* 获得源文件的输入字节流 */
        FileInputStream fin = new FileInputStream(src);

        /* 获取输入字节流的文件通道 */
        FileChannel fcin = fin.getChannel();

        /* 获取目标文件的输出字节流 */
        FileOutputStream fout = new FileOutputStream(dist);

        /* 获取输出字节流的文件通道 */
        FileChannel fcout = fout.getChannel();

        /* 为缓冲区分配 1024 个字节 */
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

        while (true) {

            /* 从输入通道中读取数据到缓冲区中 */
            int r = fcin.read(buffer);

            /* read() 返回 -1 表示 EOF */
            if (r == -1) {
                break;
            }

            /* 切换读写 */
            buffer.flip();

            /* 把缓冲区的内容写入输出文件中 */
            fcout.write(buffer);

            /* 清空缓冲区 */
            buffer.clear();
        }
    }
}


