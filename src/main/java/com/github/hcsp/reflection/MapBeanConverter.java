package com.github.hcsp.reflection;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
                .filter(MapBeanConverter::isGetterMethod)
                .collect(Collectors.toMap(MapBeanConverter::getFieldName, method -> {
                    try {
                        return method.invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException();
                    }
                }));

    }

    private static boolean isGetterMethod(Method method) {
        String methodName = method.getName();
        //判断是否空参
        if (method.getParameterCount() != 0) {
            return false;
        }
        //判断方法名是否以is/get开头
        return (methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3)))
                || (methodName.startsWith("is") && methodName.length() > 2 && Character.isUpperCase(methodName.charAt(2)));
    }

    private static String getFieldName(Method method) {
        String unCapitalizedMethodName = method.getName().split("get|is")[1];

        return Character.toLowerCase(unCapitalizedMethodName.charAt(0)) + unCapitalizedMethodName.substring(1);

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
            T bean = klass.getDeclaredConstructor().newInstance();
            map.forEach((key, value) -> {
                Method setter = setterMethod(klass, key);
                if (setter != null) {
                    try {
                        setter.invoke(bean, value);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            });
            return bean;
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;


    }

    private static Method setterMethod(Class klass, String key) {
        String methodName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        try {
            return klass.getDeclaredMethod(methodName, klass.getDeclaredField(key).getType());
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            e.printStackTrace();
            return null;
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
