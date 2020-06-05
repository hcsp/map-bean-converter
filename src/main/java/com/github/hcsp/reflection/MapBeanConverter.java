package com.github.hcsp.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MapBeanConverter {

    public static Map<String, Object> beanToMap(Object bean) throws IllegalArgumentException, SecurityException {
        Map<String, Object> result = new HashMap<>();
        Class<? extends Object> clazz = bean.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if ((methods[i].getParameterCount() == 0 && methods[i].getName().startsWith("get"))) {
                try {
                    result.put(Character.toLowerCase(methods[i].getName().charAt(3))+ methods[i].getName().substring(4), methods[i].invoke(bean));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if ((methods[i].getParameterCount() == 0 && methods[i].getName().startsWith("is")
                    && methods[i].getReturnType().equals(Boolean.TYPE))) {
                try {
                    result.put(Character.toLowerCase(methods[i].getName().charAt(2))+methods[i].getName().substring(3), methods[i].invoke(bean));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        T t = null;
        try {
            t = klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Field[] fileds = klass.getDeclaredFields();
        for (int i = 0; i < fileds.length; i++) {
            String key = fileds[i].getName();
            if (map.containsKey(key)) {
                fileds[i].setAccessible(true);
                try {
                    fileds[i].set(t, map.get(key));
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return t;
    }

    public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, InstantiationException {
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
            return "DemoJavaBean{" + "id=" + id + ", name='" + name + '\'' + ", longName=" + isLongName() + '}';
        }
    }
}
