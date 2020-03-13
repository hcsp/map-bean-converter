package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
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
        Map<String, Object> map = new HashMap<>();
        Class<?> clazz = bean.getClass();
        Stream.of(clazz.getMethods()).
                filter(MapBeanConverter::isGetterMethods).
                forEach(method -> putBeanPropertiesToMap(method, map, bean));
        return map;
    }

    private static void putBeanPropertiesToMap(Method method, Map<String, Object> map, Object bean) {
        String key = isBooleanMethods(method) ? makeFirstCharLower(method.getName().substring(2)) : makeFirstCharLower(method.getName().substring(3));
        try {
            Object value = method.invoke(bean);
            map.put(key, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isBooleanMethods(Method method) {
        return method.getName().startsWith("is");
    }

    private static boolean isGetterMethods(Method method) {
        return method.getParameterCount() == 0 && (method.getName().startsWith("get") || (method.getName().startsWith("is") && method.getReturnType() == boolean.class));
    }

    private static String makeFirstCharLower(String string) {
        return Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }

    private static String makeFirstCharUpper(String string) {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        T obj;
        try {
            obj = klass.getConstructor().newInstance();
            map.forEach((key, value) -> {
                try {
                    klass.getMethod("set" + makeFirstCharUpper(key), value.getClass()).invoke(obj, value);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return obj;
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
}
