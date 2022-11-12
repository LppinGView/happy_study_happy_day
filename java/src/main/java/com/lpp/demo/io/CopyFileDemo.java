package com.lpp.demo.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CopyFileDemo {

    /**
     * @param args
     * @throws FileNotFoundException
     */
    public static void main(String[] args) {
        String src ="c:/workplace/test.txt";
        String dest="c:/workplace/to/test.txt";
        try {
            copyFile(src,dest);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("文件不存在");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("拷贝文件失败|关闭流失败");
        }
    }

    /**
     * 文件的拷贝
     * @param  源文件路径
     * @param  目录文件路径
     * @throws FileNotFoundException,IOException
     * @return
     */
    public static void copyFile(String srcPath,String destPath) throws FileNotFoundException,IOException {
        //1、建立联系 源(存在且为文件) +目的地(文件可以不存在)
        File src =new File(srcPath);
        File dest =new File(destPath);
        if(! src.isFile()){ //不是文件或者为null
            System.out.println("只能拷贝文件");
            throw new IOException("只能拷贝文件");
        }
        //2、选择流
        InputStream is =new FileInputStream(src);
        OutputStream os =new FileOutputStream(dest);
        //3、文件拷贝   循环+读取+写出
        byte[] flush =new byte[1024];
        int len =0;
        //读取
        while(-1!=(len=is.read(flush))){
            //写出
            os.write(flush, 0, len);
        }
        os.flush(); //强制刷出

        //关闭流
        os.close();
        is.close();
    }

}