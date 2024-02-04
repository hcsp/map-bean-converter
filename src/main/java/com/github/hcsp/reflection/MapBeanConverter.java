package com.github.hcsp.reflection;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

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
            //获取bean的类对象clazz
            Class<?> clazz = bean.getClass();
            //通过getDeclaredFields()、getDeclaredMethods()获取该类的所有成员变量和方法
            Field[] fields = clazz.getDeclaredFields();
            Method[] methods = clazz.getDeclaredMethods();
            //方法创建一个空的HashMap对象，用于保存成员变量名和对应的值
            Map<String, Object> map = new HashMap<>();
            for (Field field : fields) {
                //将该成员变量设置为可访问（即使是私有成员变量也可以访问）
                field.setAccessible(true);
                //然后获取成员变量的名字 name 和值 value，并将它们放入 map 中
                String name = field.getName();
                Object value = field.get(bean);
                map.put(name, value);
                //如果方法的名字是 “isLongName”，则通过 method.invoke(bean) 调用该方法，并将返回值放入 map 中的 “longName” 键对应的位置
                for (Method method : methods) {
                    if (method.getName().equals("isLongName")) {
                        map.put("longName", method.invoke(bean));
                    }
                }
            }
            return map;
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
        T t = null;
        try {
            t = klass.getConstructor().newInstance();
            Method[] methods = klass.getDeclaredMethods();
            Set<Map.Entry<String, Object>> entries = map.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                String key = entry.getKey();
                Object value = entry.getValue();
                for (Method method : methods) {
                    if (method.getName().equals(("set") + firstUpperCase(key))) {
                        method.invoke(t, value);
                    }
                }
            }
            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String firstUpperCase(String key) {
        char[] chars = key.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
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
