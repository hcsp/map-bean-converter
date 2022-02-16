package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
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
        return Stream.of(bean.getClass().getDeclaredMethods())
                .filter(MapBeanConverter::isGetter)
                .collect(Collectors.toMap(MapBeanConverter::getFieldName,
                        method -> invokeGetter(bean, method)));
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        T bean;
        try {
            bean = klass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String mapKey = entry.getKey();
            Object mapValue = entry.getValue();
            Method method = getSetter(klass,mapKey);
            if (method != null) {
                invokeSetter(bean, mapValue, method);
            }
        }
        return bean;
    }

    private static <T> void invokeSetter(T bean, Object mapValue, Method method) {
        try {
            method.invoke(bean, mapValue);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static <T> Method getSetter(Class<T> klass,String key) {
        Method declaredMethod = null;
        try {
            declaredMethod = klass.getDeclaredMethod("set" + fieldNameToUpperCase(key), klass.getDeclaredField(key).getType());
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return declaredMethod;
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

    private static boolean isGetter(Method method) {
        String methodName = method.getName();

        return (methodName.startsWith("get") && methodName.length() > 3
                && method.getParameters().length == 0
        )
                || (methodName.startsWith("is") && methodName.length() > 2
                && Character.isUpperCase(methodName.charAt(2)));
    }

    private static String getFieldName(Method method) {
        return fieldNameToLowerCase(method.getName().split("is|get")[1]);
    }

    private static String fieldNameToLowerCase(String string) {
        return string.replace(string.charAt(0), Character.toLowerCase(string.charAt(0)));
    }
    private static String fieldNameToUpperCase(String string) {
        return string.replace(string.charAt(0), Character.toUpperCase(string.charAt(0)));
    }

    private static Object invokeGetter(Object bean, Method method) {
        try {
            return method.invoke(bean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException();
        }
    }

    public static class DemoJavaBean {
        private Integer id;
        private String name;
        private String privateField = "privateField";

        public int isolate() {
            System.out.println(privateField);
            return 0;
        }

        public String is() {
            return "";
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public String getName(int i) {
            return name + i;
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
