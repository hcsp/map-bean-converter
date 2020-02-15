package com.github.hcsp.reflection;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MapBeanConverterTest {
    @Test
    public void test() {
        MapBeanConverter.DemoJavaBean bean = new MapBeanConverter.DemoJavaBean();
        bean.setId(100);
        bean.setName("BBBBBBBBBBBBB");
        Map<String, Object> resultMap = MapBeanConverter.beanToMap(bean);

        Assertions.assertEquals(100, resultMap.get("id"));
        Assertions.assertEquals("BBBBBBBBBBBBB", resultMap.get("name"));
        Assertions.assertEquals(true, resultMap.get("longName"));
        Assertions.assertNull(resultMap.get("olate"));

        Map<String, Object> map = new HashMap<>();
        map.put("id", 456);
        map.put("name", "12345");
        MapBeanConverter.DemoJavaBean resultBean =
                MapBeanConverter.mapToBean(MapBeanConverter.DemoJavaBean.class, map);

        Assertions.assertEquals(456, resultBean.getId());
        Assertions.assertEquals("12345", resultBean.getName());
    }
}
