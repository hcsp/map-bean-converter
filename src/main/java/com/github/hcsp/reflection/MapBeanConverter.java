package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MapBeanConverter {
    // 传入一个遵守Java Bean约定的对象，读取它的所有属性，存储成为一个Map
    // 例如，对于一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 应当返回一个Map { id -> 1, name -> "ABC", longName -> false }
    // 提示：
    //  1. 读取传入参数bean的Class
    //  2. 通过反射获得它包含的所有名为getXXX/isXXX，且无参数的方法（即getter方法）
    //  3. 通过反射调用这些方法并将获得的值存储到Map中返回
    public static Map<String, Object> beanToMap(Object bean) {
        Map<String, Object> hashMap = new HashMap<>();
        Class<?> clazz = bean.getClass();
        for (Method method : clazz.getMethods()) {
            if (isGet(method)) {
                javaBeanToMap(bean, hashMap, method, 3);
            }
            if (isIs(method)) {
                javaBeanToMap(bean, hashMap, method, 2);
            }
        }
        return hashMap;
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        T bean = newInstance(klass);
        for (Method method : klass.getMethods()) {
            if (isSet(method)) {
                String field = getJavaBeanField(method, 3);
                map.forEach((k, v) -> {
                    if (k.equals(field)) {
                        invoke(method, bean, v);
                    }
                });
            }
        }
        return bean;
    }

    private static boolean isIs(Method method) {
        String name = method.getName();
        if (name.startsWith("is") && name.length() > 2 && method.getParameterCount() == 0) {
            if (Character.isUpperCase(name.toCharArray()[2])) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGet(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && !"getClass".equals(name) && name.length() > 3 && method.getParameterCount() == 0) {
            if (Character.isUpperCase(name.toCharArray()[3])) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSet(Method method) {
        String name = method.getName();
        if (name.startsWith("set") && name.length() > 3) {
            if (Character.isUpperCase(name.toCharArray()[3])) {
                return true;
            }
        }
        return false;
    }

    private static void javaBeanToMap(Object bean, Map<String, Object> hashMap, Method method, int i) {
        hashMap.put(getJavaBeanField(method, i), invoke(method, bean));
    }

    private static String getJavaBeanField(Method method, int i) {
        char[] field = method.getName().substring(i).toCharArray();
        field[0] = Character.toLowerCase(field[0]);
        return String.valueOf(field);
    }

    private static Object invoke(Method method, Object bean, Object... args) {
        try {
            return method.invoke(bean, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T newInstance(Class<T> klass) {
        try {
            return klass.getConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
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
