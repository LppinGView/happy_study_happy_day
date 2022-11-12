# java.io
 * 面向stream。当数据未准备好时，线程read，write会阻塞。
 * 分为输出输入流。每个流是单向的。
 * 输出流OutputStream/ Writer  输入流InputStream/ Reader。其中OutputStream/InputStream是处理byte字节流，Writer/Reader是char字符流
 * 传统网络编程，socket连接读取网卡数据，使用阻塞io，导致io成为瓶颈
# java.nio
 * 面向缓冲区。当数据未准备好，线程读写数据时不会被阻塞。
 * 线程不会直接面对数据源读写数据，是通过buffer读写数据的。
 * 通道时双向的，channel 即可读也可写，但是读写切换时，需要buffer.flip()
 * 针对网络编程，阻塞io导致并发量不高，网络编程这块优化了网络IO模块

# 网络编程
 * 传统网络编程，服务端使用ServerSocket，监听端口。之后accept等待客户端连接，每次连接建立之后（三次握手），创建一个新Socket，使用该Socket
   可以创建输入流，从网卡中获取客户端数据，之后进行业务处理（业务计算），业务处理完使用输出流向客户端返回处理结果。 accept 会阻塞线程。
   输入流读取数据，当数据没有准备好，也会阻塞线程
 * 编程模型从，一连接一线程IO，到多路复用IO，再到非阻塞同步Reactor反应器模式，以及Proactor异步网络模式。

### [Scalable IO in Java](https://gee.cs.oswego.edu/dl/cpjslides/nio.pdf)