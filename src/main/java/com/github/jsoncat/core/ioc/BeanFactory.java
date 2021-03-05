package com.github.jsoncat.core.ioc;

import com.github.jsoncat.annotation.ioc.Component;
import com.github.jsoncat.annotation.springmvc.RestController;
import com.github.jsoncat.common.util.ReflectionUtil;
import com.github.jsoncat.core.config.ConfigurationFactory;
import com.github.jsoncat.core.config.ConfigurationManager;
import com.github.jsoncat.exception.DoGetBeanException;
import com.github.jsoncat.factory.ClassFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description 创建 bean 的工厂
 * @Author Goodenough
 * @Date 2021/3/5 20:34
 */
public final class BeanFactory {

    //ioc 容器
    public static final Map<String, Object> BEANS = new ConcurrentHashMap<>(128);

    //key -> className    value -> beanNames
    public static final Map<String, String[]> SINGLE_BEAN_NAMES_TYPE_MAP = new ConcurrentHashMap<>(128);

    public static void loadBeans(){
        Set<Class<?>> componentClasses = ClassFactory.CLASS.get(Component.class);
        for (Class<?> aClass : componentClasses) {
            String beanName = BeanHelper.getBeanName(aClass);
            Object instance = ReflectionUtil.newInstance(aClass);
            BEANS.put(beanName, instance);
        }
        Set<Class<?>> restControllerClasses = ClassFactory.CLASS.get(RestController.class);
        for (Class<?> aClass : restControllerClasses) {
            Object instance = ReflectionUtil.newInstance(aClass);
            BEANS.put(aClass.getName(), instance);
        }
        BEANS.put(ConfigurationManager.class.getName(), new ConfigurationManager(ConfigurationFactory.getConfig()));
    }


    /**
     *从 ioc 容器获取 bean
     * @param type bean的类型
     * @return T bean的实例
     */
    public static <T> T getBean(Class<T> type) {
        String[] beanNames = getBeanNamesForType(type);
        if (beanNames.length == 0)
            throw new DoGetBeanException("not found bean implement, the bean : " + type.getName());
        Object beanInstance = BEANS.get(beanNames[0]);
        if (!type.isInstance(beanInstance))
            throw new DoGetBeanException("not found bean implement, the bean : " + type.getName());
        return type.cast(beanInstance);
    }

    /**
     * 根据类型获取 beanNames
     * @param type 目标类型
     * @return String[] beanNames
     */
    private static String[] getBeanNamesForType(Class<?> type){
        String className = type.getName();
        String[] beanNames = SINGLE_BEAN_NAMES_TYPE_MAP.get(className);
        if (beanNames == null) {
            List<String> beanNamesList = new ArrayList<>();
            for (Map.Entry<String, Object> beanEntry : BEANS.entrySet()) {
                Class<?> beanClass = beanEntry.getValue().getClass();
                if (type.isInterface()) {
                    Class<?>[] interfaces = beanClass.getInterfaces();
                    for (Class<?> c : interfaces) {
                        if (type.getName().equals(c.getName())) {
                            beanNamesList.add(beanEntry.getKey());
                            break;
                        }
                    }
                } else if (beanClass.isAssignableFrom(type)) {
                    beanNamesList.add(beanEntry.getKey());
                }
            }
            beanNames = beanNamesList.toArray(new String[0]);
            SINGLE_BEAN_NAMES_TYPE_MAP.put(className, beanNames);
        }
        return beanNames;
    }
}
