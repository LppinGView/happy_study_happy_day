package org.example.config;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.nonNull;

public class ZookeeperConfig {
    private static ZooKeeper zooKeeper;
    private static final ConcurrentMap<String, ZooKeeper> zookeeperMap = new ConcurrentHashMap<>();
    private static final ZookeeperConfig config = new ZookeeperConfig();

    private ZookeeperConfig(){
        try {
            boolean existConnect = zookeeperMap.containsKey("zooKeeper");
            if (existConnect && nonNull(zookeeperMap.get("zooKeeper"))){
                this.zooKeeper = (zookeeperMap.get("zooKeeper"));
            }else {
                this.zooKeeper = new ZooKeeper("172.17.8.146:2181", 200000, null);
                if (existConnect){
                    zookeeperMap.replace("zooKeeper", this.zooKeeper);
                }else {
                    zookeeperMap.put("zooKeeper", this.zooKeeper);
                }
            }
        } catch (IOException e) {
        }
    }

    public static ZookeeperConfig connet(){
        return config;
    }

    private static boolean existNode(String path) throws InterruptedException, KeeperException {
        return zooKeeper.exists(path, false) != null;
    }

    public static ZookeeperConfig initEphemeralNode(String path){
        try {
            if ( !existNode(path) ){
                zooKeeper.create(path, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    public static long getNodeData(String path){
        try {
            initEphemeralNode(path);
            byte[] data = zooKeeper.getData(path, null, null);
            if (null != data || data.length >0){
                String s = new String(data);
                return StringUtils.isNotBlank(s)? Long.parseLong(new String(data)) : 0L;
            }else {
                return 0L;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ZookeeperConfig setNodeData(String path, long value){
        try {
            initEphemeralNode(path);
            zooKeeper.setData(path, Long.toString(value).getBytes(), -1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    public static ZookeeperConfig incrementValue(String path){
        try {
            Stat stat = new Stat();
            byte[] data = zooKeeper.getData(path, false, stat);
            int value = 0;
            if (data.length != 0){
                value = Integer.parseInt(new String(data));
            }
            value++;

            zooKeeper.setData(path, Integer.toString(value).getBytes(), stat.getVersion());
            System.out.println("Updated value: " + value);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return config;
    }
}
