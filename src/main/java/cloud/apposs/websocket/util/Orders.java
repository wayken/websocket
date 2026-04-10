package cloud.apposs.websocket.util;

import cloud.apposs.websocket.annotation.Order;

import java.util.Collections;
import java.util.List;

public final class Orders {
    /**
     * 根据Order注解进行列表的排序
     *
     * @param compareList 需要排序的列表，列表元素的类上需要有Order注解
     */
    public static <T> void sortByOrderAnnotation(List<T> compareList) {
        Collections.sort(compareList, (object1, object2) -> {
            Order order1 = object1.getClass().getAnnotation(Order.class);
            Order order2 = object2.getClass().getAnnotation(Order.class);
            int order1Value = order1 == null ? 0 : order1.value();
            int order2Value = order2 == null ? 0 : order2.value();
            return order1Value - order2Value;
        });
    }
}
