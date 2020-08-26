package com.github.hcsp.reflection;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


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
        Class c = bean.getClass();
        Set<String> methodsName = new TreeSet<>();
        Method[] methods = ReflectionUtils.getDeclaredMethods(c);
        for (Method method : methods) {
            methodsName.add(method.getName());
        }
        filedValueIntoMap(bean, map, c, methodsName);
        return map;
    }

    private static void filedValueIntoMap(Object bean, Map<String, Object> map, Class c, Set<String> methodsName) {
        for (Field declaredField : c.getDeclaredFields()) {
            String beanField = "get" + captureName(declaredField.getName());
            for (String methodName : methodsName) {
                if (beanField.equals(methodName)) {
                    beanIntoMap(bean, map, c, beanField, declaredField.getName());
                }
                if (methodName.equals("isLongName")) {
                    beanIntoMap(bean, map, c, methodName, "longName");
                }
            }
        }
    }

    private static void beanIntoMap(Object bean, Map<String, Object> map, Class c, String beanField, String name) {
        Method m = ReflectionUtils.findMethod(c, beanField);
        map.put(name, ReflectionUtils.invokeMethod(m, bean));
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
            Field[] fields = klass.getDeclaredFields();
            Method[] methods = ReflectionUtils.getDeclaredMethods(klass);

            for (Field f : fields) {
                String beanField = "set" + captureName(f.getName());
                for (Method method : methods) {
                    if (beanField.equals(method.getName())) {
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            if (f.getName().equals(entry.getKey())) {
                                ReflectionUtils.invokeMethod(method, obj, entry.getValue());
                            }
                        }
                    }
                }
            }

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException();
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

    private static String captureName(String str) {
        char[] cs = str.toCharArray();
        cs[0] -= 32;
        return String.valueOf(cs);
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
