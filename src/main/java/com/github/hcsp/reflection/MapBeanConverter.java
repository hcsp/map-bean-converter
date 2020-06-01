package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MapBeanConverter {
    // 传入一个遵守Java Bean约定的对象，读取它的所有属性，存储成为一个Map
    // 例如，对于一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 应当返回一个Map { id -> 1, name -> "ABC", longName -> false }
    // 提示：
    //  1. 读取传入参数bean的Class
    //  2. 通过反射获得它包含的所有名为getXXX/isXXX，且无参数的方法（即getter方法）
    //  3. 通过反射调用这些方法并将获得的值存储到Map中返回
    static final String GETTER_NAME_REGEX = "(get|is)([A-Z].*)";
    static final String SETTER_NAME_REGEX = "set([A-Z].*)";

    public static Map<String, Object> beanToMap(Object bean) {
        Class klass = bean.getClass();
        final Method[] methods = klass.getDeclaredMethods();
        return Arrays.stream(methods)
                .filter(MapBeanConverter::isJavaBeanGetter)
                .collect(Collectors.toMap(
                        method -> getRegexTargetValue(method.getName(), GETTER_NAME_REGEX, 2),
                        method -> {
                            try {
                                return method.invoke(bean);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                ));
    }

    public static String lowerCaseFirstLetter(String string) {
        return Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }

    public static String getRegexTargetValue(String getterMethodName, String regex, int groupIndex) {
        final Pattern getterNamePattern = Pattern.compile(regex);
        final Matcher matcher = getterNamePattern.matcher(getterMethodName);
        if (!matcher.matches()) {
            return null;
//            throw new RuntimeException("错误的方法名称" + getterMethodName);
        }
        return lowerCaseFirstLetter(matcher.group(groupIndex));
    }

    public static boolean isPublicMethod(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    public static boolean isJavaBeanGetter(Method method) {
        if (!isPublicMethod(method)) {
            return false;
        }
        boolean isNoParams = method.getParameterCount() == 0;
        if (!isNoParams) {
            return false;
        }
        return getRegexTargetValue(method.getName(), GETTER_NAME_REGEX, 2) != null;
    }

    public static boolean isJavaBeanSetter(Method method) {
        if (!isPublicMethod(method)) {
            return false;
        }
        boolean isNotSingleParam = method.getParameterCount() == 1;
        if (!isNotSingleParam) {
            return false;
        }
        return getRegexTargetValue(method.getName(), SETTER_NAME_REGEX, 1) != null;
    }

    public static Method getFieldSetterMethod(String fieldName, String fieldClassName, Map<String, Method> setters) {
        final Method method = setters.get(fieldName);
        if (method == null) {
            return null;
        }
        boolean isParamsTypeEquals = method.getParameterTypes()[0].getName().equals(fieldClassName);
        if (isParamsTypeEquals) {
            return method;
        }
        return null;
    }

    public static Map<String, Method> getJavaBeanAllSetters(Class klass) {
        return Arrays.stream(klass.getDeclaredMethods())
                .filter(MapBeanConverter::isJavaBeanSetter)
                .collect(Collectors.toMap(
                        method -> lowerCaseFirstLetter(getRegexTargetValue(method.getName(), SETTER_NAME_REGEX, 1)),
                        method -> method
                ));
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
        final Map<String, Method> javaBeanSetters = getJavaBeanAllSetters(klass);
        try {
            object = klass.getConstructor().newInstance();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object fieldValue = entry.getValue();
                Method method = getFieldSetterMethod(entry.getKey(), fieldValue.getClass().getName(), javaBeanSetters);
                if (method == null) {
                    continue;
                }
                method.invoke(object, fieldValue);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return (T) object;
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
