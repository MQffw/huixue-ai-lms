package com.itheima.ai.service;

import com.itheima.config.ChatClientConfig;
import com.itheima.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 定时 Agent 服务（Phase 7）
 * 1. 每周一 8:00 生成教务周报
 * 2. 每天 9:00 检查违纪异常
 */
@Slf4j
@Component
public class ScheduledAgentService {

    @Autowired
    private ChatClientConfig chatClientConfig;

    @Autowired
    private ViolationLogMapper violationLogMapper;

    @Autowired
    private EmploymentMapper employmentMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Scheduled(cron = "0 0 8 * * MON")
    public void generateWeeklyReport() {
        log.info("===== 开始生成教务周报 =====");
        try {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(7);
            LocalDate monthStart = today.withDayOfMonth(1);

            StringBuilder data = new StringBuilder();
            data.append("周期：").append(weekStart).append("至").append(today).append("\n");

            // 违纪数据
            try {
                List<Map<String, Object>> violationStats = violationLogMapper.countByType(
                        weekStart.format(DATE_FMT), today.format(DATE_FMT));
                long total = violationStats.stream().mapToLong(r -> ((Number) r.get("count")).longValue()).sum();
                data.append("违纪次数：").append(total).append("\n");
            } catch (Exception e) { data.append("违纪数据获取失败\n"); }

            // 就业数据
            try {
                List<Map<String, Object>> empStats = employmentMapper.getEmploymentStatsByClazz();
                for (Map<String, Object> row : empStats) {
                    data.append(row.get("clazz_name")).append("就业")
                        .append(row.get("employed_count")).append("人\n");
                }
            } catch (Exception e) { data.append("就业数据获取失败\n"); }

            String prompt = "请根据以下数据生成教务周报：\n" + data;
            ChatClient chatClient = chatClientConfig.getChatClient("longcat");
            String report = chatClient.prompt().user(prompt).call().content();

            log.info("===== 教务周报 ====\n{}", report);
        } catch (Exception e) {
            log.error("教务周报生成失败", e);
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void checkViolationAnomaly() {
        log.info("===== 违纪异常检查 =====");
        try {
            LocalDate today = LocalDate.now();
            LocalDate thisWeekStart = today.minusDays(7);
            LocalDate lastWeekStart = today.minusDays(14);

            List<Map<String, Object>> thisWeekStats = violationLogMapper.countByType(
                    thisWeekStart.format(DATE_FMT), today.format(DATE_FMT));
            List<Map<String, Object>> lastWeekStats = violationLogMapper.countByType(
                    lastWeekStart.format(DATE_FMT), today.minusDays(7).format(DATE_FMT));

            long thisWeekTotal = thisWeekStats.stream()
                    .mapToLong(row -> ((Number) row.get("count")).longValue()).sum();
            long lastWeekTotal = lastWeekStats.stream()
                    .mapToLong(row -> ((Number) row.get("count")).longValue()).sum();

            double growthRate = lastWeekTotal > 0 ? (double) thisWeekTotal / lastWeekTotal : 0;
            if (growthRate > 1.5) {
                log.warn("⚠ 违纪异常增长！本周{}次 vs 上周{}次，增长率{}%",
                        thisWeekTotal, lastWeekTotal, String.format("%.0f", (growthRate - 1) * 100));
            } else {
                log.info("违纪情况正常：本周{}次，上周{}次", thisWeekTotal, lastWeekTotal);
            }
        } catch (Exception e) {
            log.error("违纪异常检查失败", e);
        }
    }
}
