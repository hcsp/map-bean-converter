package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        Method[] methods = bean.getClass().getDeclaredMethods();
        Map<String, Object> result = new HashMap<>();
        Arrays.stream(methods).filter(MapBeanConverter::isGetterMethods).forEach(method -> {
            try {
                result.put(getFormatFiledFromSetterName(method.getName()), method.invoke(bean));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        return result;
    }

    private static String getFormatFiledFromSetterName(String name) {
        String filedName = getFiledFromSetterName(name);
        return filedName.substring(0, 1).toLowerCase() + filedName.substring(1);
    }

    private static String getFiledFromSetterName(String name) {
        if (name.startsWith("is")) {
            return name.substring(2);
        } else {
            return name.substring(3);
        }
    }

    public static boolean isGetterMethods(Method method) {
        return isParameterlessMethod(method) && isStartsWithGetOrIs(method) && isCamelCase(method);
    }

    private static boolean isParameterlessMethod(Method method) {
        return method.getParameterCount() == 0;
    }

    private static boolean isStartsWithGetOrIs(Method method) {
        return method.getName().startsWith("get") || method.getName().startsWith("is");
    }
    private static boolean isCamelCase(Method method) {
        String filedName = getFiledFromSetterName(method.getName());
        return isUpperCase(filedName.charAt(0));
    }

    public static boolean isUpperCase(char c) {
        return c >=65 && c <= 90;
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
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        Method[] methods = klass.getDeclaredMethods();
        T finalT = t;
        Arrays.stream(methods).filter(method -> method.getName().startsWith("set")).forEach(method -> {
            try {
                method.invoke(finalT, map.get(method.getName().substring(3).toLowerCase()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        return t;
    }

    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
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
