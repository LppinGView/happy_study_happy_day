package com.lpp.demo.io;

import java.io.*;

/**
 * java io 旧IO面向 流
 *
 * 因为使用流，从数据源获取数据，或者写入数据，没有缓冲区，当读取数据时，数据源没有准备好，线程将阻塞
 *
 * 按输入输出分为
 * 输入：InputStream / Reader 从数据源读取数据，比如：网络、文件等等
 * 输出：OutputStream / Writer 将数据写入 媒介中
 *
 * 比如文件io
 * FileInputStream（文件字符流）或FileReader（文件字节流）
 *
 * FileOutputStream -写-> File -读-> FileInputStream
 *
 */

public class IODemo {

    public static void main(String[] args) {
        try {
            writeByteToFile();
            readByteFromFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeByteToFile() throws IOException {
        String hello= new String( "hello word!");
        byte[] byteArray= hello.getBytes();
        File file= new File( "c:/workplace/test.txt");
        //因为是用字节流来写媒介，所以对应的是OutputStream
        //又因为媒介对象是文件，所以用到子类是FileOutputStream
        OutputStream os= new FileOutputStream( file);
        os.write( byteArray);
        os.close();
    }

    public static void readByteFromFile() throws IOException{
        File file= new File( "c:/workplace/test.txt");
        byte[] byteArray= new byte[( int) file.length()];
        //因为是用字节流来读媒介，所以对应的是InputStream
        //又因为媒介对象是文件，所以用到子类是FileInputStream
        InputStream is= new FileInputStream( file);
        int size= is.read( byteArray);
        System. out.println( "大小:"+size +";内容:" +new String(byteArray));
        is.close();
    }

}
