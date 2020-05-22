package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
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

        Map<String, Object> result = new HashMap<>();
        Class klass = bean.getClass();

        Arrays.stream(klass.getDeclaredMethods())
                .filter(m -> {
                    StringBuilder name = new StringBuilder(m.getName()); // 使用 StringBuilder 防止待会CharAt时数组越界
                    // 过滤有参方法
                    if (m.getParameters().length != 0) {
                        return false;
                    }
                    // 过滤非 get 或 is 开头, 并且其后跟随非大写字母的方法
                    if (name.toString().startsWith("get") || name.toString().startsWith("is")) {
                        return Character.isUpperCase(name.charAt(name.toString().startsWith("get") ? 3 : 2));
                    } else {
                        return false;
                    }
                })
                .forEach(m -> {
                    String name = m.getName();
                    String roughName = name.substring(name.startsWith("get") ? 3 : 2);
                    try {
                        result.put(
                                Character.toLowerCase(roughName.charAt(0)) + roughName.substring(1),
                                m.invoke(bean)
                        );
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                });

        return result;
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {

        T result = null;
        try {
            result = klass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        T finalResult = result;
        map.forEach((name, value) -> {
            // 方法2: 使用反射获取setter
            String methodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
            try {
                klass.getMethod(methodName, value.getClass()).invoke(finalResult, value);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });

        return result;
    }

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
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
