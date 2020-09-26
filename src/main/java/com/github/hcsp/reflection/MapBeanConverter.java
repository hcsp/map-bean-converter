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
        Map<String, Object> map = new HashMap<>();
        Class beanClass = bean.getClass();
        Method[] methods = beanClass.getDeclaredMethods();
        for (Method method : methods) {
            String keyName = getKeyName(method);
            if (keyName == null) {
                continue;
            }
            map.put(keyName, getValue(method, bean));
        }
        return map;
    }

    private static String getKeyName(Method method) {
        String result = null;
        if (method.getParameterCount() != 0) {
            return null;
        }
        if (method.getName().startsWith("get")) {
            result = method.getName().substring(3);
        }
        if (method.getName().startsWith("is")) {
            result = method.getName().substring(2);
        }
        return result != null && !"".equals(result)
                ? String.valueOf(result.charAt(0)).toLowerCase() + result.substring(1)
                : null;
    }

    private static Object getValue(Method method, Object bean) {
        try {
            Object result = method.invoke(bean);
            if (method.getName().startsWith("is") && result.getClass() != Boolean.class) {
                return null;
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
        Object bean = null;
        try {
            bean = klass.getDeclaredConstructor().newInstance();
            for (String key : map.keySet()) {
                Object value = map.get(key);
                Method method = getMethod(klass, key, value);
                if (method != null) {
                    method.invoke(bean, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (T) bean;
    }

    public static Method getMethod(Class klass, String filedName, Object value) {
        String methodName = "set" + String.valueOf(filedName.charAt(0)).toUpperCase() + filedName.substring(1);
        try {
            return klass.getMethod(methodName, value.getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

        @Override public String toString() {
            return "DemoJavaBean{" + "id=" + id + ", name='" + name + '\'' + ", longName=" + isLongName() + '}';
        }
    }
}
