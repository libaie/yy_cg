package com.ruoyi.yy.config;

import java.util.Set;
import java.util.HashSet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OCR 上传安全配置
 *
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "ocr.security")
public class OcrSecurityConfig
{
    /** 允许的文件扩展名（含点号，如 .jpg），默认仅允许常见图片格式 */
    private Set<String> allowedExtensions = new HashSet<>(Set.of(".jpg", ".jpeg", ".png"));

    /** 最大文件大小（字节），默认 10MB */
    private long maxFileSize = 10L * 1024L * 1024L;

    /** 文件保留时长（小时），默认 24 小时 */
    private int retentionHours = 24;

    public Set<String> getAllowedExtensions()
    {
        return allowedExtensions;
    }

    public void setAllowedExtensions(Set<String> allowedExtensions)
    {
        this.allowedExtensions = allowedExtensions;
    }

    public long getMaxFileSize()
    {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize)
    {
        this.maxFileSize = maxFileSize;
    }

    public int getRetentionHours()
    {
        return retentionHours;
    }

    public void setRetentionHours(int retentionHours)
    {
        this.retentionHours = retentionHours;
    }

    /**
     * 校验文件名是否为允许的扩展名（不区分大小写）
     *
     * @param originalFilename 原始文件名
     * @return true 表示允许上传
     */
    public boolean isAllowed(String originalFilename)
    {
        if (originalFilename == null || !originalFilename.contains("."))
        {
            return false;
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        for (String allowed : allowedExtensions)
        {
            if (allowed.equalsIgnoreCase(extension))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 将允许的扩展名集合转为 String[]（去掉前导点号，适配 FileUploadUtils 格式）
     */
    public String[] getAllowedExtensionsArray()
    {
        return allowedExtensions.stream()
                .map(ext -> ext.startsWith(".") ? ext.substring(1) : ext)
                .toArray(String[]::new);
    }
}
