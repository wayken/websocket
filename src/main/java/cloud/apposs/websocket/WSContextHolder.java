package cloud.apposs.websocket;

import cloud.apposs.ioc.BeanFactory;
import cloud.apposs.ioc.BeansException;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.websocket.annotation.Order;
import cloud.apposs.websocket.util.Orders;

import java.util.List;

/**
 * WS上下文持有者，提供全局访问WS相关资源的入口，包括配置加载，对象注入等功能
 */
@Component
public class WSContextHolder {
    private final WSConfig configuration;

    private final BeanFactory beanFactory;

    public WSContextHolder(WSConfig configuration, BeanFactory beanFactory) {
        this.configuration = configuration;
        this.beanFactory = beanFactory;
    }

    public WSConfig getConfiguration() {
        return configuration;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public <T> T getBean(Class<T> beanClass) throws BeansException {
        return beanFactory.getBean(beanClass);
    }

    /**
     * 根据父类类型获取最近一个实现的子类对象
     */
    public <T> T getBeanHierarchy(Class<T> beanType) throws BeansException {
        return beanFactory.getBeanHierarchy(beanType);
    }

    /**
     * 根据父类类型获取所有实现的子类对象，
     * 同时对列表进行{@link Order}注解排序
     */
    public <T> List<T> getBeanHierarchyList(Class<T> beanType) throws BeansException {
        // 获取实现类列表
        List<T> beanList = beanFactory.getBeanHierarchyList(beanType);
        // 对实现类进行Order注解排序，方便定义调用次序
        Orders.sortByOrderAnnotation(beanList);
        return beanList;
    }
}
