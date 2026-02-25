package com.restaurant.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.order.entity.Order;
import com.restaurant.report.dto.TableStatsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    
    @Select("SELECT * FROM orders WHERE table_id = #{tableId} AND status < 4 ORDER BY created_at DESC LIMIT 1")
    Order selectCurrentOrderByTable(Long tableId);
    
    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM orders WHERE DATE(created_at) = CURDATE() AND status = 4")
    BigDecimal selectTodayRevenue();
    
    @Select("SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE() AND status = 4")
    Long selectTodayOrderCount();
    
    @Select("SELECT * FROM orders WHERE status IN (1, 2, 3) ORDER BY created_at ASC")
    List<Order> selectActiveOrders();
    
    @Select("SELECT t.table_no as tableNo, t.name as tableName, COUNT(o.id) as orderCount, SUM(o.pay_amount) as totalAmount " +
            "FROM restaurant_table t LEFT JOIN orders o ON t.id = o.table_id AND DATE(o.created_at) = CURDATE() AND o.status = 4 " +
            "WHERE t.is_deleted = 0 GROUP BY t.id, t.table_no, t.name")
    List<TableStatsDTO> selectTableStats();
}
