package com.smart.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.community.entity.RepairOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

    @Select("<script>" +
            "SELECT r.*, d.name AS device_name, d.device_code, d.location AS device_location " +
            "FROM repair_order r LEFT JOIN device d ON r.device_id = d.id " +
            "<where>" +
            "<if test='status != null and status != \"\"'> AND r.status = #{status}</if>" +
            "<if test='level != null and level != \"\"'> AND r.level = #{level}</if>" +
            "<if test='deviceId != null'> AND r.device_id = #{deviceId}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (r.order_code LIKE CONCAT('%',#{keyword},'%') OR r.reporter LIKE CONCAT('%',#{keyword},'%') OR d.name LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "</where>" +
            "ORDER BY FIELD(r.status,'PENDING','PROCESSING','COMPLETED','VERIFIED'), r.create_time DESC" +
            "</script>")
    IPage<RepairOrder> selectOrderPage(Page<RepairOrder> page,
                                       @Param("status") String status,
                                       @Param("level") String level,
                                       @Param("deviceId") Long deviceId,
                                       @Param("keyword") String keyword);

    /**
     * 近 months 个月的工单趋势（按状态分组计数）
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, status, COUNT(*) AS cnt " +
            "FROM repair_order WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL #{months} MONTH) " +
            "GROUP BY month, status ORDER BY month")
    List<Map<String, Object>> selectTrend(@Param("months") int months);

    /**
     * 近 months 个月维修费用合计（按完成/验收时间）
     */
    @Select("SELECT DATE_FORMAT(IFNULL(finish_time, create_time), '%Y-%m') AS month, SUM(IFNULL(cost,0)) AS amount " +
            "FROM repair_order WHERE status IN ('COMPLETED','VERIFIED') AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{months} MONTH) " +
            "GROUP BY month ORDER BY month")
    List<Map<String, Object>> selectCostTrend(@Param("months") int months);

    /**
     * 今日新增报修数
     */
    @Select("SELECT COUNT(*) FROM repair_order WHERE DATE(create_time) = CURDATE()")
    long countToday();

    /**
     * 各状态工单数量统计
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM repair_order GROUP BY status")
    List<Map<String, Object>> selectStatusCount();
}
