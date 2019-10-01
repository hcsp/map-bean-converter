package com.github.hcsp.reflection;

import com.google.common.collect.Maps;

import java.lang.reflect.InvocationTargetException;
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
        HashMap<String, Object> result = Maps.newHashMap();
        for (Method method : bean.getClass().getDeclaredMethods()) {
            if (isGetter(method)) {
                Object returnValue = invokeMethod(bean, method);
                String fieldName = getFieldNameByGetterAndSetter(method);
                result.put(fieldName, returnValue);
            }
        }
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
        T instance = newInstanc(klass);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Method setter = getSetterByFieldName(klass, entry);
            invokeMethod(instance, setter, entry.getValue());
        }
        return instance;
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

    /**
     * 功能描述：根据Getter或Setter方法返回字段名
     *
     * @param method 方法
     * @return java.lang.String
     * @author wu.yue
     * @date 2019/10/1 16:08
     */
    private static String getFieldNameByGetterAndSetter(Method method) {
        if (method.getName().startsWith("get") || method.getName().startsWith("set")) {
            return lowerCaseFirstLetter(method.getName().substring(3));
        } else {
            return lowerCaseFirstLetter(method.getName().substring(2));
        }
    }

    /**
     * 功能描述：调用实例对象的Getter或Setter方法
     *
     * @param object     实例对象
     * @param method     方法
     * @param parameters 方法参数列表
     * @return java.lang.Object
     * @author wu.yue
     * @date 2019/10/1 16:07
     */
    private static Object invokeMethod(Object object, Method method, Object... parameters) {
        Object returnValue = null;
        try {
            returnValue = method.invoke(object, parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Method Invoke Exception: ", e);
        }
        return returnValue;
    }

    /**
     * 功能描述：判断是否是Getter方法
     *
     * @param method 方法
     * @return boolean
     * @author wu.yue
     * @date 2019/10/1 16:07
     */
    private static boolean isGetter(Method method) {
        if (method.getParameterCount() != 0) {
            return false;
        }
        return method.getName().startsWith("get") || method.getName().startsWith("is");
    }

    /**
     * 功能描述：根据key-value对返回对应Class中的Setter方法
     *
     * @param klass Class对象
     * @param entry key-value对
     * @return java.lang.reflect.Method
     * @author wu.yue
     * @date 2019/10/1 16:06
     */
    private static <T> Method getSetterByFieldName(Class<T> klass, Map.Entry entry) {
        Method setter = null;
        try {
            setter = klass.getDeclaredMethod("set" + upperCaseFirstLetter(entry.getKey().toString()), entry.getValue().getClass());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return setter;
    }

    /**
     * 功能描述：返回实例，而不用处理异常
     *
     * @param klass 实例的Class对象
     * @return T
     * @author wu.yue
     * @date 2019/10/1 16:05
     */
    private static <T> T newInstanc(Class<T> klass) {
        T instance = null;
        try {
            instance = klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to instance: ", e);
        }
        return instance;
    }

    /**
     * 功能描述：首字母大写
     * ps: 原本以为是很简单很普遍的功能，但是在网上找了半天还是没找到别人写好的轮子...
     *
     * @param string 原字符串
     * @return java.lang.String
     * @author wu.yue
     * @date 2019/10/1 16:03
     */
    private static String upperCaseFirstLetter(String string) {
        char[] chars = string.toCharArray();
        chars[0] -= 32;
        return String.valueOf(chars);
    }

    /**
     * 功能描述：首字母小写
     * ps: 原本以为是很简单很普遍的功能，但是在网上找了半天还是没找到别人写好的轮子...
     *
     * @param string 原字符串
     * @return java.lang.String
     * @author wu.yue
     * @date 2019/10/1 16:04
     */
    private static String lowerCaseFirstLetter(String string) {
        char[] chars = string.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
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
