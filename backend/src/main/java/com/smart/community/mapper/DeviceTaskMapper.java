package com.smart.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.community.entity.DeviceTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DeviceTaskMapper extends BaseMapper<DeviceTask> {

    /**
     * 任务分页查询（联查设备信息），status 为空时按日期动态判定逾期
     */
    @Select("<script>" +
            "SELECT t.id, t.task_code, t.task_type, t.device_id, t.plan_date, t.executor, " +
            "CASE WHEN t.status = 'PENDING' AND t.plan_date &lt; #{today} THEN 'OVERDUE' ELSE t.status END AS status, " +
            "t.result, t.check_time, t.location, t.photo, t.check_items, t.remark, t.create_time, " +
            "d.name AS device_name, d.device_code, d.location AS device_location, d.type AS device_type " +
            "FROM device_task t LEFT JOIN device d ON t.device_id = d.id " +
            "<where>" +
            "<if test='type != null and type != \"\"'> AND t.task_type = #{type}</if>" +
            "<if test='status != null and status != \"\" and status != \"OVERDUE\"'> AND t.status = #{status}</if>" +
            "<if test='status == \"OVERDUE\"'> AND (t.status = 'OVERDUE' OR (t.status = 'PENDING' AND t.plan_date &lt; #{today}))</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (t.task_code LIKE CONCAT('%',#{keyword},'%') OR d.name LIKE CONCAT('%',#{keyword},'%') OR d.device_code LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test='deviceId != null'> AND t.device_id = #{deviceId}</if>" +
            "<if test='planDate != null'> AND t.plan_date = #{planDate}</if>" +
            "</where>" +
            "ORDER BY CASE WHEN t.status = 'PENDING' AND t.plan_date &lt; #{today} THEN 0 " +
            "WHEN t.status = 'OVERDUE' THEN 1 WHEN t.status = 'PENDING' THEN 2 " +
            "WHEN t.status = 'COMPLETED' THEN 3 ELSE 4 END, " +
            "CASE WHEN t.status = 'COMPLETED' THEN t.check_time END DESC, t.plan_date" +
            "</script>")
    IPage<DeviceTask> selectTaskPage(Page<DeviceTask> page,
                                     @Param("type") String type,
                                     @Param("status") String status,
                                     @Param("keyword") String keyword,
                                     @Param("deviceId") Long deviceId,
                                     @Param("planDate") LocalDate planDate,
                                     @Param("today") LocalDate today);

    /**
     * 将已过期未执行任务落库为 OVERDUE（供刷新接口调用）
     */
    @Update("UPDATE device_task SET status = 'OVERDUE' WHERE status = 'PENDING' AND plan_date < #{today}")
    int markOverdue(@Param("today") LocalDate today);

    /**
     * 某设备最近一条任务（用于按周期生成计划）
     */
    @Select("SELECT * FROM device_task WHERE device_id = #{deviceId} AND task_type = #{type} ORDER BY plan_date DESC LIMIT 1")
    DeviceTask selectLatest(@Param("deviceId") Long deviceId, @Param("type") String type);

    /**
     * 指定日期范围内已有任务数量（防重复生成）
     */
    @Select("SELECT COUNT(*) FROM device_task WHERE device_id = #{deviceId} AND task_type = #{type} AND plan_date = #{date}")
    long countByDate(@Param("deviceId") Long deviceId, @Param("type") String type, @Param("date") LocalDate date);

    /**
     * 巡检完成率统计：近 N 天内任务数与完成数（按日）
     */
    @Select("SELECT plan_date AS date, COUNT(*) AS total, SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS done " +
            "FROM device_task WHERE task_type = #{type} AND plan_date >= #{from} " +
            "GROUP BY plan_date ORDER BY plan_date")
    List<Map<String, Object>> selectCompletion(@Param("type") String type, @Param("from") LocalDate from);
}
