package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class MapBeanConverter {
    // 传入一个遵守Java Bean约定的对象，读取它的所有属性，存储成为一个Map
    // 例如，对于一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 应当返回一个Map { id -> 1, name -> "ABC", longName -> false }
    // 提示：
    //  1. 读取传入参数bean的Class
    //  2. 通过反射获得它包含的所有名为getXXX/isXXX，且无参数的方法（即getter方法）
    //  3. 通过反射调用这些方法并将获得的值存储到Map中返回
    private static boolean isGetterMethod(String method, String keyword) {
        return method.startsWith(keyword) && method.length() > 3 && Character.isUpperCase(method.charAt(keyword.length()));
    }


    private static boolean isGetMethodWithoutParameter(Method method) {
        return method.getParameterCount() == 0 && isGetterMethod(method.getName(), "get") || isGetterMethod(method.getName(), "is");
    }

    private static String removeGetterPrefix(String getterName) {
        if (isGetterMethod(getterName, "get")) {
            return Character.toLowerCase(getterName.charAt(3)) + getterName.substring(4);
        } else if (isGetterMethod(getterName, "is")) {
            return Character.toLowerCase(getterName.charAt(2)) + getterName.substring(3);
        }
        // If the method name does not start with "get" or "is," it is not a getter and does not have a property key.
        return null;
    }

    public static Map<String, Object> beanToMap(Object bean) {
        Map<String, Object> ret = new HashMap<>();
        Class<?> myClass = bean.getClass();
        Method[] methods = myClass.getDeclaredMethods();
        Arrays.stream(methods).filter(MapBeanConverter::isGetMethodWithoutParameter).forEach(method -> {
            try {
                ret.put(removeGetterPrefix(method.getName()), method.invoke(bean));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
        return ret;
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
            map.forEach((key, value) -> {
                String setterName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
                try {
                    klass.getMethod(setterName, value.getClass()).invoke(bean, value);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return bean;
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
