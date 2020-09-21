package com.github.hcsp.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
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
        Class<?> clazz = bean.getClass();

        List<String> methods = getGetterMethodsNameWithoutBrackets(clazz);

        List<String> fieldList = getDeclaredFieldsAndConvertIntoList(clazz);

        return filterAttributeRelatedMethodsAndReflect(bean, clazz, methods, fieldList);
    }

    private static Map<String, Object> filterAttributeRelatedMethodsAndReflect(Object bean, Class<?> clazz, List<String> methods, List<String> fieldList) {
        Map<String, Object> result = new HashMap<>();
        for (String method : methods) {
            String str = method.startsWith("get") ? method.substring(3) : method.substring(2);
            for (String field : fieldList) {
                if (str.toLowerCase().contains(field)) {
                    String key = replaceFirstLetterToLowerCase(str);
                    Object obj;
                    try {
                        obj = clazz.getMethod(method).invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                    result.put(key, obj);
                }
            }
        }
        return result;
    }

    private static String replaceFirstLetterToLowerCase(String str) {
        String firstStr = str.substring(0, 1);
        return str.replace(firstStr, firstStr.toLowerCase());
    }

    private static List<String> getDeclaredFieldsAndConvertIntoList(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        return Arrays.stream(fields).map(Field::getName).collect(Collectors.toList());
    }

    private static List<String> getGetterMethodsNameWithoutBrackets(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> (method.getName().startsWith("get") | method.getName().startsWith("is"))
                        && method.getName().length() > 2 && method.getParameterCount() == 0)
                .map(Method::getName)
                .collect(Collectors.toList());
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        T t;
        try {
            t = klass.getConstructor().newInstance();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String methodName = "set" + Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1);
                klass.getMethod(methodName, entry.getValue().getClass()).invoke(t, entry.getValue());
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    public static void main(String[] args) {
        DemoJavaBean bean = new DemoJavaBean();
        bean.setId(100);
        bean.setName("AAAAAAAAAAAAAAAAAAA");
        System.out.println(beanToMap(bean)); //bean是静态内部类对象

        Map<String, Object> map = new HashMap<>();
        map.put("id", 123);
        map.put("name", "ABCDEFG");
        System.out.println(mapToBean(DemoJavaBean.class, map));
    }

    public static class DemoJavaBean {
        private Integer id;
        private String name;
        private String privateField = "privateField";

        public DemoJavaBean() {
        }

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
