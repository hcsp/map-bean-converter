package com.github.hcsp.reflection;

import java.lang.reflect.Field;
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
        Map<String, Object> map = new HashMap<>();
        String fieldName;
        String key = null;
        try {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.getName().toLowerCase().contains("get")
                        && method.getParameterCount() == 0) {
                    map.put(method.getName().substring(3).toLowerCase(),
                            method.invoke(bean));
                } else if (method.getName().toLowerCase().contains("is")
                        && method.getParameterCount() == 0) {
                    key = lowerCase(key, method);
                    map.put(key, method.invoke(bean));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
        return map;
    }

    private static String lowerCase(String key, Method method) {
        char[] array = method.getName().substring(2).toCharArray();
        if (array[0] >= 'A' && array[0] <= 'Z') {
            array[0] += 32;
            key = String.valueOf(array);
        }
        return key;
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        try {
            T newInstance = klass.getConstructor().newInstance();
            for (String attribute : map.keySet()) {
                for (Method method : klass.getMethods()) {
                    if (method.getName().contains("set") &&
                            method.getName().toLowerCase().contains(attribute)) {
                        method.invoke(newInstance, map.get(attribute));
                    }
                }
            }
            return newInstance;
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
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
