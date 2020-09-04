package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
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
        Class<?> clazz = bean.getClass();
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> isGetterOrIsMethodOfBean(clazz, method))
                .collect(Collectors.toMap(MapBeanConverter::getFiledNameFromGetterOrIsMethod, method -> invokeGetterOrIsMethod(bean, method)));
    }

    private static boolean isGetterOrIsMethodOfBean(Class<?> clazz, Method method) {
        return method.getParameterCount() == 0
                && (isGetterMethod(clazz, method) || isIsMethod(method));
    }

    private static boolean isIsMethod(Method method) {
        return method.getName().matches("^is.+") && method.getReturnType().equals(boolean.class);
    }

    private static boolean isGetterMethod(Class<?> clazz, Method method) {
        return method.getName().matches("^get.+")
                && Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> method.getReturnType().equals(field.getType()) && getFiledNameFromGetterOrIsMethod(method).equals(field.getName()));
    }

    private static String getFiledNameFromGetterOrIsMethod(Method method) {
        return makeFirstCharToLowercase(method.getName().split("get|is")[1]);
    }

    private static String makeFirstCharToLowercase(String source) {
        return isUppercaseLetter(source.charAt(0)) ? Character.toLowerCase(source.charAt(0)) + source.substring(1) : source;
    }

    private static boolean isUppercaseLetter(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    private static Object invokeGetterOrIsMethod(Object bean, Method method) {
        try {
            return method.invoke(bean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        T bean = genInstanceFromClass(klass);
        Arrays.stream(klass.getDeclaredMethods())
                .filter(method -> isSetterMethodOfBean(klass, method))
                .forEach(method -> invokeSetterMethod(bean, method, map));
        return bean;
    }

    private static <T> T genInstanceFromClass(Class<T> klass) {
        T bean;
        try {
            bean = klass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return bean;
    }

    private static boolean isSetterMethodOfBean(Class<?> clazz, Method method) {
        return method.getName().matches("^set.+")
                && method.getParameterCount() == 1
                && Stream.of(clazz.getDeclaredFields()).anyMatch(field -> method.getReturnType().equals(void.class) && method.getParameterTypes()[0].equals(field.getType()) && getFiledNameFromSetterMethod(method).equals(field.getName()));
    }

    private static String getFiledNameFromSetterMethod(Method method) {
        return makeFirstCharToLowercase(method.getName().split("set")[1]);
    }

    private static void invokeSetterMethod(Object bean, Method method, Map<String, Object> map) {
        try {
            method.invoke(bean, map.get(getFiledNameFromSetterMethod(method)));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
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
