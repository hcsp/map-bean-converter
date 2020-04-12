package com.github.hcsp.reflection;

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
        try {
            Class clazz = bean.getClass();
            Map<String, Object> result = new HashMap<>();
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (!isGetterMethod(method)) {
                    continue;
                }

                String key = getPropertyName(method.getName());
                Object returnValue = method.invoke(bean);
                if (key == null || returnValue == null) {
                    continue;
                }

                result.put(key, returnValue);
            }
            return result;
        } catch (Exception e) {
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
        try {
            T bean = klass.getConstructor().newInstance();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String name = "set" + capitalizePropertyName(key);
                Method method = klass.getDeclaredMethod(name, value.getClass());
                if (method == null) {
                    continue;
                }
                method.invoke(bean, value);
            }
            return bean;
        } catch (Exception e) {
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

    private static String capitalizePropertyName(final String s) {
        if (s.length() == 0) {
            return s;
        }

        final char[] chars = s.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    private static String camelizePropertyName(final String s) {
        if (s.length() == 0) {
            return s;
        }

        final char[] chars = s.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private static String getPropertyName(final String name) {
        if (name.startsWith("is")) {
            return camelizePropertyName(name.substring(2));
        } else if (name.startsWith("get")) {
            return camelizePropertyName(name.substring(3));
        }
        return null;
    }

    private static boolean isGetterMethod(Method method) {
        if (method.getParameterCount() != 0) {
            return false;
        }

        String name = method.getName();
        if (name.startsWith("set")) {
            return false;
        }

        if (name.startsWith("is")) {
            Class returnType = method.getReturnType();
            return returnType.getName().toLowerCase().equals("boolean");
        }
        return true;
    }
}
