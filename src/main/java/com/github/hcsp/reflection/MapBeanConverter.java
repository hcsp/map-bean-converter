package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MapBeanConverter {
    // 传入一个遵守Java Bean约定的对象，读取它的所有属性，存储成为一个Map
    // 例如，对于一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 应当返回一个Map { id -> 1, name -> "ABC", longName -> false }
    // 提示：
    //  1. 读取传入参数bean的Class
    //  2. 通过反射获得它包含的所有名为getXXX/isXXX，且无参数的方法（即getter方法）
    //  3. 通过反射调用这些方法并将获得的值存储到Map中返回
    public static Map<String, Object> beanToMap(Object bean) {
        Method[] methods = bean.getClass().getDeclaredMethods();
        return Arrays.stream(methods)
                .filter(MapBeanConverter::isGetMethod)
                .collect(Collectors.toMap(
                        MapBeanConverter::getProperty, method
                                -> getValue(bean, method)));
    }


    private static boolean isGetMethod(Method method) {
        return method.getParameterCount() == 0 &&
                (isStartWithWord(method.getName(), "get")
                        || isStartWithWord(method.getName(), "is"));
    }

    private static boolean isStartWithWord(String name, String word) {
        return name.startsWith(word) && (name.length() > word.length()) &&
                Character.isUpperCase(name.substring(word.length()).charAt(0));
    }

    private static String getProperty(Method method) {
        String methodname = method.getName();
        if (methodname.startsWith("get")) {
            return methodname.substring(3, 4).toLowerCase() + methodname.substring(4);
        } else {
            return methodname.substring(2, 3).toLowerCase() + methodname.substring(3);
        }
    }

    private static Object getValue(Object property, Method method) {
        try {
            return method.invoke(property);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException();
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
        T bean;
        try {
            bean = klass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException();
        }
        Method[] methods = klass.getDeclaredMethods();
        for (Method method : methods) {
            String methodname = method.getName();
            if (methodname.startsWith("set") && methodname.length() > 3) {
                String property =
                        methodname.substring(3, 4).toLowerCase() + methodname.substring(4);
                Object value = map.get(property);
                try {
                    method.invoke(bean, value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException();
                }
            }
        }
        return bean;
    }

    public static void main(String[] args) {
        DemoJavaBean bean = new DemoJavaBean();
        bean.setId(100);
        bean.setName("AAAAAAAAAAAAAAAAAAA");
        System.out.println(beanToMap(bean));

        MapBeanConverter.DemoJavaBean bean1 = new MapBeanConverter.DemoJavaBean();
        bean1.setId(100);
        bean1.setName("BBBBBBBBBBBBB");
        System.out.println(MapBeanConverter.beanToMap(bean1).get("longName"));

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
