package com.chat.task;

import com.chat.mapper.MessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 消息定时清理任务
 * 每天凌晨 3 点删除超过 retention-days 天的消息，减轻数据库压力
 */
@Component
public class MessageCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(MessageCleanupTask.class);

    private final MessageMapper messageMapper;

    @Value("${message.retention-days:7}")
    private int retentionDays;

    public MessageCleanupTask(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 每天凌晨 3:00 执行清理
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldMessages() {
        log.info("===== 开始清理 {} 天前的历史消息 =====", retentionDays);
        try {
            int deleted = messageMapper.deleteMessagesOlderThan(retentionDays);
            log.info("===== 清理完成，共删除 {} 条过期消息 =====", deleted);
        } catch (Exception e) {
            log.error("清理过期消息时发生异常", e);
        }
    }
}
