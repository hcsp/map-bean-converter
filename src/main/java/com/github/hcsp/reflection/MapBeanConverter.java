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
        Map<String, Object> result = Arrays.stream(bean.getClass().getDeclaredMethods())
                .filter(method -> isGetterMethod(method.getName()))
                .collect(Collectors.toMap(MapBeanConverter::getFieldName, method -> getMethodValue(bean, method)));
        return result;
    }

    public static boolean isGetterMethod(String methodName) {
        return methodName.startsWith("is") || methodName.startsWith("get");
    }

    public static String getFieldName(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("is")) {
            return lowCaseFirstLetter(methodName.substring(2));
        } else {
            return lowCaseFirstLetter(methodName.substring(3));
        }
    }

    public static String lowCaseFirstLetter(String fieldName) {
        String firstLetter = fieldName.substring(0, 1).toLowerCase();
        String remainName = fieldName.substring(1);
        return firstLetter + remainName;
    }

    public static Object getMethodValue(Object bean, Method method) {
        try {
            method.setAccessible(true);
            return method.invoke(bean);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }


    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        Object obj = null;
        try {
            obj = klass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Object finalObj = obj;
        Arrays.stream(klass.getDeclaredMethods()).forEach(method -> {
            if (isSetterMethod(method.getName())) {
                try {
                    method.invoke(finalObj, map.get(getFieldName(method)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return (T) finalObj;
    }

    public static boolean isSetterMethod(String methodName) {
        return methodName.startsWith("set");
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

    static class DemoJavaBean {
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
