package com.ruoyi.yy.job;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.yy.config.OcrSecurityConfig;

/**
 * OCR 临时文件清理任务
 * 每天凌晨 2 点执行，删除超过保留时限的 OCR 上传文件
 *
 * @author ruoyi
 */
@Component
public class OcrFileCleanupJob
{
    private static final Logger log = LoggerFactory.getLogger(OcrFileCleanupJob.class);

    @Value("${ruoyi.profile}")
    private String profile;

    @Autowired
    private OcrSecurityConfig ocrSecurityConfig;

    /**
     * 每天凌晨 2 点清理过期 OCR 文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredOcrFiles()
    {
        String ocrDir = profile + "/ocr";
        File dir = new File(ocrDir);
        if (!dir.exists() || !dir.isDirectory())
        {
            log.debug("OCR 目录不存在，跳过清理: {}", ocrDir);
            return;
        }

        int retentionHours = ocrSecurityConfig.getRetentionHours();
        Instant cutoff = Instant.now().minus(retentionHours, ChronoUnit.HOURS);
        int deletedCount = 0;

        File[] files = dir.listFiles();
        if (files != null)
        {
            for (File file : files)
            {
                if (file.isFile())
                {
                    try
                    {
                        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        if (attrs.lastModifiedTime().toInstant().isBefore(cutoff))
                        {
                            if (file.delete())
                            {
                                deletedCount++;
                            }
                            else
                            {
                                log.warn("无法删除过期 OCR 文件: {}", file.getAbsolutePath());
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("清理 OCR 文件时发生异常: {}", file.getAbsolutePath(), e);
                    }
                }
            }
        }

        if (deletedCount > 0)
        {
            log.info("OCR 文件清理完成，共删除 {} 个过期文件（保留 {} 小时），目录: {}",
                    deletedCount, retentionHours, ocrDir);
        }
    }
}
