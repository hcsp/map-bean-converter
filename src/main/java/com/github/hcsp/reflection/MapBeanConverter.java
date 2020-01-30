package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class MapBeanConverter {
    // 传入一个遵守Java Bean约定的对象，读取它的所有属性，存储成为一个Map
    // 例如，对于一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 应当返回一个Map { id -> 1, name -> "ABC", longName -> false }
    // 提示：
    //  1. 读取传入参数bean的Class
    //  2. 通过反射获得它包含的所有名为getXXX/isXXX，且无参数的方法（即getter方法）
    //  3. 通过反射调用这些方法并将获得的值存储到Map中返回
    public static Map<String, Object> beanToMap(Object bean) {
        Map<String, Object> fieldsOfBean = new HashMap<>();
        Stream.of(bean.getClass().getDeclaredMethods())
                .filter(MapBeanConverter::isGetterMethod)
                .forEach(method -> getterFieldsToMap(bean, fieldsOfBean, method));
        return fieldsOfBean;
    }

    private static void getterFieldsToMap(Object bean, Map<String, Object> fieldsOfBean, Method method) {
        try {
            if (method.getName().startsWith("get")) {
                fieldsOfBean.put(method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4), method.invoke(bean));
            } else {
                fieldsOfBean.put(method.getName().substring(2, 3).toLowerCase() + method.getName().substring(3), method.invoke(bean));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
           throw new RuntimeException(e);
        }
    }

    private static boolean isGetterMethod(Method method) {
        return method.getName().startsWith("is") || method.getName().startsWith("get");
    }


    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        T obj;
        try {
            obj = klass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        map.forEach((K, V) -> setFieldOfBean(obj, setMethod(K), V));
        return obj;
    }

    private static String setMethod(String k) {
        return "set" + k.substring(0, 1).toUpperCase() + k.substring(1);
    }

    private static void setFieldOfBean(Object bean, String method, Object param) {
        try {
            bean.getClass().getMethod(method, param.getClass()).invoke(bean, param);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DemoJavaBean bean = new DemoJavaBean();
        bean.setId(100);
        bean.setName("AAAAAAAAAAAAAAAAAAA");
        System.out.println(beanToMap(bean));

        Map<String, Object> map = new HashMap<>();
        map.put("id", 123);
        map.put("name", "ABCDEFG");
        System.out.println(mapToBean(DemoJavaBean.class, map));
    }

    public static class DemoJavaBean {
        Integer id;
        String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isLongName() {
            return name.length() > 10;
        }

        @Override
        public String toString() {
            return "DemoJavaBean{"
                    + "id="
                    + id
                    + ", name='"
                    + name
                    + '\''
                    + ", longName="
                    + isLongName()
                    + '}';
        }
    }
}
