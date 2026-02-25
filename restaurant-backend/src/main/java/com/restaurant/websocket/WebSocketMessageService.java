package com.restaurant.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.order.dto.OrderDetailDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketMessageService {

    private final KitchenWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * 通知后厨新订单
     */
    public void notifyNewOrder(OrderDetailDTO orderDetail) {
        try {
            WebSocketMessage message = new WebSocketMessage();
            message.setType("NEW_ORDER");
            message.setData(orderDetail);
            
            String json = objectMapper.writeValueAsString(message);
            webSocketHandler.broadcast(json);
            log.info("发送新订单通知到后厨");
        } catch (Exception e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    /**
     * 通知订单状态更新
     */
    public void notifyOrderStatus(Long orderId, Integer status) {
        try {
            WebSocketMessage message = new WebSocketMessage();
            message.setType("ORDER_STATUS");
            message.setData(new StatusUpdate(orderId, status));
            
            String json = objectMapper.writeValueAsString(message);
            webSocketHandler.broadcast(json);
        } catch (Exception e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    /**
     * 通知菜品状态更新
     */
    public void notifyItemStatus(Long itemId, Integer status) {
        try {
            WebSocketMessage message = new WebSocketMessage();
            message.setType("ITEM_STATUS");
            message.setData(new ItemStatusUpdate(itemId, status));
            
            String json = objectMapper.writeValueAsString(message);
            webSocketHandler.broadcast(json);
        } catch (Exception e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    // 内部消息类
    @lombok.Data
    public static class WebSocketMessage {
        private String type;
        private Object data;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class StatusUpdate {
        private Long orderId;
        private Integer status;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ItemStatusUpdate {
        private Long itemId;
        private Integer status;
    }
}
