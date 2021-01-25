package com.java.demo.ioc.core.v2;

import com.java.demo.annotion.Autowired;
import com.java.demo.bean.InstanceA;
import com.java.demo.proxy.MyProxyBeanPostProcessor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleIocV2 {

    /**
     * 类在实例化的过程中，这个过程可以拆解成多个步骤
     * 1.实例化(在内存中分配一个空间，并将引用赋值给该实例对象)
     * 2.属性赋值(过程是什么？)
     * 3.初始化(属性值赋值给实例化的对象，此时对象已经完备)
     */

    /**
     * 调用无参构造函数 实例化对象，此时bean还未进行初始化
     * 使用一级缓存确实可以属性注入引起的循环依赖问题，但是
     * 此时实例A还未进行属性值初始化，一级缓存中就有未初始化的A，
     * 初始化好完整的实例B，B中存在A的引用，对象A的field或者属性可以延后设置
     * 当A还在初始化时，另一个线程来获取A时，会拿到未初始化好的A，
     * 由于A的属性未初始化，可能出现空指针异常
     *
     * 故需要将各层进行分层，用至少两级缓存，使用了两级缓存后，当A还在初始化时，
     * 先放入二级缓存，初始化完成后，放入一级缓存
     * 另一个线程getBean，将从一级缓存获取不到bean，将会去创建步骤，此时通过一个set
     * 来控制是否创建单例对象（set中已存在，则不进行创建步骤）
     * registeredSingletons 对象注册池
     *
     * |
     * A' --> B' --> A --> B
     * |<------------------|
     * |
     * A初始化完成后，A包含完整的B对象: A(B)
     * A初始化完成后，此时B中也存在了完整的对象A: B(A)
     * 但是在A实例化到A初始化完成的过程中，一级缓存中A的对象是未初始化的对象，属性可能为空值!!!
     *
     * */
    //一级缓存 单例缓存池，getBean将从这个缓存池中获取初始化好的实例对象
    private static final Map<String, Object>  singletonObjects = new ConcurrentHashMap<>(256);

    /**
     * 在二级缓存基础上，此时如果实例对象是被代理对象，则会出现B初始化完成后，B中的A还是未被代理的对象!
     * 此时需要三级代理来解决
     *
     * |
     * A' --> B' --> A --> B''
     * |<------------------|
     * |
     * A初始化完成后，A包含完整的B的代理对象: A''(B'')
     * A初始化完成后，此时B中则还是A的原有对象: B''(A)
     */
    //二级缓存
    private static final Map<String, Object>  earlySingletonObjects = new ConcurrentHashMap<>(16);

    /**
     *
     * 在创建bean的时候，首先从缓存中获取单例的bean，这个缓存就是singletonObjects，
     * 如果获取不到且bean正在创建中，就再从earlySingletonObjects中获取，如果还是
     * 获取不到且允许从singletonFactories中通过getObject拿到对象，就从singletonFactories中获取，
     * 如果获取到了就存入earlySingletonObjects并从singletonFactories中移除
     *
     * 在Spring中存在第三级缓存，在创建对象时判断是否是单例，允许循环依赖，
     * 正在创建中，就将其从earlySingletonObjects中移除掉，并在singletonFactories放入新的对象，
     * 这样后续再查询beanName时会走到singletonFactory.getObject()，
     * 其中就会去调用各个beanPostProcessor的getEarlyBeanReference方法，
     * 返回的对象就是代理后的对象。
     *
     */
    //三级缓存
    private static final Map<String, Object> singletonFactories = new HashMap(16);

    private static Set<String> registeredSingletons = new LinkedHashSet<>(256);

    public static void main(String[] args) throws Exception {

        String beanName = "com.java.demo.bean.InstanceA";

        createBean(beanName);

        InstanceA instanceA =(InstanceA) getBean(beanName);
        instanceA.say();
    }

    private static Object getBean(String beanName) {
        return singletonObjects.get(beanName);
    }

    /**
      * @param beanName
     * @return
     * @throws Exception
     */
    private static Object createBean(String beanName) throws Exception {
        Class clz = Class.forName(beanName);
        Field[] fields = clz.getDeclaredFields();
        //先从缓存中获取bean
        if (null != getSingleton(beanName)){
            return getSingleton(beanName);
        }
        //实例化
        Object bean = doCreateBean(clz);

        //属性赋值
        for (Field field : fields) {
            //属性值上存在自定义注解
            Autowired annotation = field.getAnnotation(Autowired.class);
            if (Objects.nonNull(annotation)){
                Class<?> type = field.getType();
                //如果存在注入注解 则实例化该对象 递归调用
                Object beanDepan = createBean(type.getName());
                //属性赋值 由于属性为private 设置为允许修改
                field.setAccessible(true);
                field.set(bean, beanDepan);
            }
        }

        //初始化
        //放入单例缓存池中 一级缓存
        singletonObjects.put(beanName, bean);
        return bean;
    }

    //根据beanName从缓存中获取Bean实例
    private static Object getSingleton(String beanName){
        Object bean = singletonObjects.get(beanName);
        if (null == bean){
            bean = earlySingletonObjects.get(beanName);
            if (null == bean){
                ObjectFactory singletonFactory =(ObjectFactory) singletonFactories.get(beanName);
                if (null != singletonFactory){
                    //处理三级缓存中的A的 ObjectFactory 即A的被代理对象
                    bean = singletonFactory.getObject();
                    //放入二级缓存 移除三级缓存 可以将A的被代理对象放入二级缓存池中
                    earlySingletonObjects.put(beanName, bean);
                    singletonFactories.remove(beanName);
                }
            }
        }
        return bean;
    }

    private static Object doCreateBean(Class<?> clzss) throws IllegalAccessException, InstantiationException {
        //创建实例
        Object bean = clzss.newInstance();
        String beanName = clzss.getName();
        //将实例化对象装入 三级缓存中 同时放入lamada表达式，作为后调方法
        addSingletonFactory(beanName, ()->
            getEarlyBeanReference(beanName, bean)
        );
        return bean;
    }

    protected static void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
        Assert.notNull(singletonFactory, "Singleton factory must not be null");
        if (!singletonObjects.containsKey(beanName)) {
            //三级缓存存入 ObjectFactory 待后续处理
            singletonFactories.put(beanName, singletonFactory);
            earlySingletonObjects.remove(beanName);
            registeredSingletons.add(beanName);
        }
    }

    /**
     * 此方法对应三级缓存中的 ObjectFactory
     * 是实例的在被代理后的处理方法
     * @param beanName
     * @param bean
     * @return
     */
    protected static Object getEarlyBeanReference(String beanName, Object bean) {
        Object exposedObject = bean;
        MyProxyBeanPostProcessor proxyBeanPostProcessor = new MyProxyBeanPostProcessor();
        return proxyBeanPostProcessor.getEarlyBeanReference(beanName, exposedObject);
    }
}
