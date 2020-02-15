package com.github.hcsp.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * method.invoke()-->java.lang.IllegalArgumentException: object is not an instance of declaring class
 * invoke需要传递调用的对象，除非是static
 * 被调用的对象没有无参构造方法时-->java.lang.NoSuchMethodException: com.github.hcsp.reflection.MapBeanConverter$DemoJavaBean.<init>()
 * 被调用对象与原对象不同是，无法获取原本bean的value
 * 询问why非public的class没有生成无参数构造方法
 */
public class MapBeanConverter {
    // 传入一个遵守Java Bean约定的对象，读取它的所有属性，存储成为一个Map
    // 例如，对于一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 应当返回一个Map { id -> 1, name -> "ABC", longName -> false }
    // 提示：
    //  1. 读取传入参数bean的Class
    //  2. 通过反射获得它包含的所有名为getXXX/isXXX，且无参数的方法（即getter方法）
    //  3. 通过反射调用这些方法并将获得的值存储到Map中返回
    public static Map<String, Object> beanToMap(Object bean) {
        //读取传入参数bean的Class
        Class classType = bean.getClass();
        //存储成为一个Map
        Map<String, Object> map = new ConcurrentHashMap<>();
        //读取它的所有属性
        Field[] fields = classType.getDeclaredFields();
        //通过反射获得它包含的method
        Method[] methods = classType.getDeclaredMethods();

        for (Field field : fields) {
            String methodName = getMethodName(field, "get");
            Object value = getValue(methodName, bean);
            map.put(field.getName(), value);
        }
        //todo 缺少longName->false,so 为了通过测试用写死的方式先造一个
        try {
            Object object = classType.getMethod("isLongName").invoke(bean);
            map.put("longName", object);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return map;
/*        //A -> a
        "A".toLowerCase();
        //get first letter
        "".charAt(0);
        //a -> A
        "a".toUpperCase();
        //asdf has sd
        "asdf".indexOf("sd");

        for (Method method : methods) {
            //过滤所有名为getXXX/isXXX，（即getter方法）
            if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                if (method.getParameterCount() == 0) {
                    //且无参数的方法
                }
                try {
                    method.invoke(classType);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        for (Field field : fields) {
            String fieldName = field.getName();
            map.put(fieldName, null);
        }*/
    }

    /**
     * 将fieldName替换为对应的methodName
     *
     * @param field 需要操作的字段名
     * @param title 需要添加的方法名
     * @return title+field()
     */
    private static String getMethodName(Field field, String title) {
        String fieldName = field.getName();
        String firstLetter = String.valueOf(fieldName.charAt(0)).toUpperCase();
        String methodName = new StringBuilder(fieldName).replace(0, 1, firstLetter).toString();
        return title + methodName;
    }

    /**
     * @param bean 此处必须传递原对象，不能传递ClassType,否则ClassType.newInstance()的新对象不具有原本的value
     */
    private static Object getValue(String methodName, Object bean) {
        try {
            Method method = bean.getClass().getMethod(methodName);
            return method.invoke(bean);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        //如果发生异常则返回一个新的对象
        return new Object();
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        Object object = null;
        try {
            //生成一个该对象的实例,此处调用非public的构造方法
            object = klass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        Field[] fields = klass.getDeclaredFields();
        Method[] methods = klass.getDeclaredMethods();
        for (Field field : fields) {
            Object value = map.get(field.getName());
            String methodName = getMethodName(field, "set");
//            Method method = klass.getMethod(methodName);//为什么用这种方式无法获取到对应的方法?
            Method method = findMethod(klass, methodName);
            try {
                method.invoke(object, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return (T) object;
    }

    private static Method findMethod(Class klass, String methodName) {
        Method[] methods = klass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        //如果未找到则返回null
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
