package com.redis.demo.config;

import com.redis.demo.core.event.DomainEvent;
import com.redis.demo.core.event.DomainEventReceiver;
import com.redis.demo.core.event.DomainEventReceiver.DomainEventMessage;
import com.redis.demo.ddd.DomainEventListener;
import com.redis.demo.exceptions.ArgumentCheckFailException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.redis.demo.utils.StreamUtil.toStream;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.springframework.aop.framework.AopProxyUtils.ultimateTargetClass;
import static org.springframework.core.MethodIntrospector.selectMethods;

@Slf4j
@Configuration
@ConfigurationProperties(value = "domain.event.enable")
public class PulsarDomainEventReceiverConfig implements BeanPostProcessor, ApplicationRunner {

    private final Map<InvokerHandler, DomainEventListener> listenerMap = new ConcurrentHashMap<>();
    private final DomainEventReceiver domainEventReceiver;

    public PulsarDomainEventReceiverConfig(DomainEventReceiver domainEventReceiver){
        this.domainEventReceiver = domainEventReceiver;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        Class<?> targetClass = ultimateTargetClass(bean);
        Map<Method, DomainEventListener> annotatedMethods = selectMethods(
                targetClass,
                (MethodIntrospector.MetadataLookup<DomainEventListener>) method -> AnnotatedElementUtils.getMergedAnnotation(method, DomainEventListener.class)
        );

        annotatedMethods
                .forEach((key, value) ->{
                    listenerMap.put(new InvokerHandler(bean, key, value), value);
                });

        return bean;
    }

    private boolean hasRunOnce = false;

    @Override
    public void run(ApplicationArguments args){
        if (hasRunOnce) return;
        hasRunOnce = true;
        if (listenerMap.isEmpty()){
            log.info("not DomainEventListener found!");
            return;
        }
        log.info("processing domain event listeners...");
        listenerMap.forEach((k, v) -> {
            if (isNoneBlank(v.description())) {
                log.info("正在注册事件回调:[{}]:[{}]:[{}]", v.server(), v.subscriberName(), v.description());
            }
            domainEventReceiver.registerCallback(v.server(), v.subscriberName(), k);
        });
    }

    //isAssignableFrom有什么作用 可以修改的？
    static class InvokerHandler implements DomainEventReceiver.EventReceiver {
        private final Object bean;
        private final Method method;
        private final Set<Class<?>> types = new HashSet<>();

        InvokerHandler(Object bean, Method method, DomainEventListener listener){
            int count = method.getParameterCount();
            if (count != 1) {
                throw new ArgumentCheckFailException("", "消息接收参数只能为1个");
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            if (!DomainEvent.class.isAssignableFrom(parameterType)) {
                throw new ArgumentCheckFailException("", "接收参数必须为DomainEvent");
            }
            if (listener.events().length == 0){
                types.add(parameterType);
            }else {
                toStream(listener.events())
                        .forEach(types::add);
            }

            this.bean = bean;
            this.method = method;
            method.setAccessible(true);
        }

        private boolean match(DomainEventMessage msg) {
            for (Class<?> t : types) {
                if (msg.isType(t)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        @SneakyThrows
        public void onMessage(DomainEventMessage message) {
            if (match(message)) {
                DomainEvent event = message.getEvent();
                log.info("received event:{} {}", message.getDebugMeta(), event);
                method.invoke(bean, event);
            }
        }
    }
}
