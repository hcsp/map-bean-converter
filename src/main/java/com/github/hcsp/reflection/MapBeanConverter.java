package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        Class classObj = bean.getClass();
        Method[] methods = classObj.getDeclaredMethods();

        Map<String, Object> javaBeanMap = new HashMap<>();
        List<Method> methodList = Arrays.asList(methods);

        methodList.forEach(method -> {
            String methodName = method.getName();

            try {
                if (methodName.startsWith("get") || methodName.startsWith("is")) {
                    Object fieldValue = method.invoke(bean);
                    String fieldName = firstLetterLowercase(getFieldNameFromBeanMethod(methodName));
                    javaBeanMap.put(fieldName, fieldValue);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        return javaBeanMap;
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        T instance = null;
        try {
            instance = klass.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                klass.getMethod(getJavaBeanMethodName(fieldName, "set"), value.getClass())
                        .invoke(instance, value);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return instance;
    }

    private static String firstLetterUppercase(String fieldName) {
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    private static String firstLetterLowercase(String fieldName) {
        return fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
    }

    private static String getJavaBeanMethodName(String fieldName, String fieldAction) {
        return fieldAction + firstLetterUppercase(fieldName);
    }

    private static String getFieldNameFromBeanMethod(String methodName) {
        if (methodName.startsWith("get") || methodName.startsWith("set")) {
            return methodName.substring(3);
        } else if (methodName.startsWith("is")) {
            return methodName.substring(2);
        } else {
            throw new RuntimeException("method " + methodName + " is not Java Bean method");
        }
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        DemoJavaBean bean = new DemoJavaBean();
        bean.setId(100);
        bean.setName("AAAAAAAAAAAAAAAAAAA");
        System.out.println(beanToMap(bean));

        Map<String, Object> map = new HashMap<>();
        map.put("id", 123);
        map.put("name", "ABCDEFG");
        System.out.println(mapToBean(DemoJavaBean.class, map));
    }

    static class DemoJavaBean {
        Integer id;
        String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isLongName() {
            return name != null && name.length() > 10;
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
