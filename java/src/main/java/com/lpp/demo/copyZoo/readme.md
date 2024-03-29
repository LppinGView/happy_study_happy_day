# copyZoo 1.0
### 设计目标：C/S架构，简单网络文件存储，支持增删查改功能
### 说明：客户端连接到网络服务器，实现简单文件的增删查改功能
### 开发语言：java
 * server
   * 接受连接请求
   * 反序列化请求
   * 校验请求格式
   * 根据命令，执行不同处理程序，返回结果 
   * 返回数据(序列化)

 * client
   * 校验请求格式
   * 序列化请求
   * 发送请求命令
   * 等待返回结果
   * 反序列化返回结果
   * 展示请求结果

### 关键技术点：
 * server网络通讯使用多线程reactor模式
 * 数据持久化
 * 空闲网络连接管理

# copyZoo 2.0
### 设计目标：增加zab共识算法，扩展为分布式文件存储

### 后续支持
 * 基础模块：crash恢复(写前日志)、jmx监控、jvm暂停监控、Metrics、集群构建、上云等等
 * 功能模块：watch机制、权限

### 申明：该项目源于本人自我发起的“copy运动”，请记住，肯定不是ctr+V！本项目整体设计参考开源项目zookeeper，仅为学习目的，帮助自我提高软件设计能力，后续有时间可以继续其他copy系列。如有任何问题，可联系邮箱278019309@qq.com
