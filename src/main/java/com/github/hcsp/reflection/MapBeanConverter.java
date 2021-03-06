package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        Class cls = bean.getClass();
        Map<String, Object> result = new HashMap<>();
        List<Method> methods = getGetterMethods(cls);
        for (Method method : methods) {
            try {
                String fieldName = getFieldName(method);
                if (fieldName != null) {
                    fieldName = Character.toLowerCase(fieldName.charAt(0)) +
                            fieldName.substring(1);
                }
                Object fieldValue = method.invoke(bean, (Object[]) null);
                result.put(fieldName, fieldValue);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static String getFieldName(Method method) {
        String parameterName = method.getName();
        if (parameterName.startsWith("is")) {
            return parameterName.substring(2);
        } else if (parameterName.startsWith("get")) {
            return parameterName.substring(3);
        }
        return null;
    }

    private static List<Method> getGetterMethods(Class cls) {
        return Arrays.stream(cls.getDeclaredMethods()).
                filter(MapBeanConverter::isTargetGetterMethod).
                filter(Method -> (Method.getParameterTypes().length == 0)).
                collect(Collectors.toList());
    }

    private static boolean isTargetGetterMethod(Method Method) {
        String name = Method.getName();
        if (name.startsWith("get")) {
            return true;
        } else if (name.startsWith("is") && name.length() > 2) {
            return name.charAt(2) >= 'A' && name.charAt(2) <= 'Z';
        } else {
            return false;
        }
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建class对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        try {
            T instance = klass.getConstructor().newInstance();
            List<Method> Methods = getSetterMethods(klass);
            for (Method method : Methods) {
                String fieldName = method.getName().substring(3).toLowerCase(Locale.ROOT);
                Object value = map.get(fieldName);
                method.invoke(instance, value);
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static <T> List<Method> getSetterMethods(Class<T> klass) {
        return Arrays.stream(klass.getDeclaredMethods())
                .filter(MapBeanConverter::isTargetSetterMethods)
                .collect(Collectors.toList());
    }

    private static boolean isTargetSetterMethods(Method method) {
        return method.getName().startsWith("set");
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
