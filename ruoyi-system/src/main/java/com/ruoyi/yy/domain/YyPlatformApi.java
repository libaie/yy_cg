package com.ruoyi.yy.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 平台API配置对象 yy_platform_api
 * 
 * @author ruoyi
 * @date 2026-04-03
 */
public class YyPlatformApi extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** API ID */
    private Long apiId;

    /** 平台ID */
    @Excel(name = "平台ID")
    private Long platformId;

    /** API名称 */
    @Excel(name = "API名称")
    private String apiName;

    /** API编码 */
    @Excel(name = "API编码")
    private String apiCode;

    /** API地址 */
    @Excel(name = "API地址")
    private String apiUrl;

    /** 请求方法 */
    @Excel(name = "请求方法")
    private String apiMethod;

    /** 请求Content-Type */
    @Excel(name = "Content-Type")
    private String contentType;

    /** 数据加密 */
    @Excel(name = "数据加密", dictType = "yy_platform_encrypt_type")
    private Integer dataEncrypt;

    /** 额外请求头（JSON格式） */
    @Excel(name = "请求头")
    private String headers;

    /** 请求体构建函数（JS字符串） */
    @Excel(name = "构建函数")
    private String buildBody;

    /** 查询参数模板（JSON格式） */
    @Excel(name = "查询参数")
    private String queryParams;

    /** 数据标准化函数（JS字符串） */
    @Excel(name = "标准化函数")
    private String normalize;

    /** 响应类型 */
    @Excel(name = "响应类型")
    private String responseType;

    /** 分页字段名 */
    @Excel(name = "分页字段名")
    private String pageField;

    /** 每页数量字段名 */
    @Excel(name = "页数字段名")
    private String pageSizeField;

    /** 默认每页数量 */
    @Excel(name = "默认每页数量")
    private Integer defaultPageSize;

    /** API类型 */
    @Excel(name = "API类型")
    private String apiType;

    /** 数据入口路径，如 data.wholesales */
    @Excel(name = "数据入口路径")
    private String entryPath;

    /** 状态：0禁用 1启用 */
    @Excel(name = "状态")
    private Integer isActive;

    /** 排序权重 */
    @Excel(name = "排序权重")
    private Integer sortOrder;

    /** 是否使用页面签名: 0-否 1-是 */
    @Excel(name = "使用页面签名")
    private Integer usePageSign;

    /** 签名脚本代码（如 waf.js 内容） */
    @Excel(name = "签名脚本")
    private String signScript;

    /** 加密数据路径，如 data.o，用于从完整响应中提取加密数据 */
    @Excel(name = "加密数据路径")
    private String encryptDataPath;

    /** 备注说明 */
    @Excel(name = "备注")
    private String remark;

    public Long getApiId() { return apiId; }
    public void setApiId(Long apiId) { this.apiId = apiId; }

    public Long getPlatformId() { return platformId; }
    public void setPlatformId(Long platformId) { this.platformId = platformId; }

    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }

    public String getApiCode() { return apiCode; }
    public void setApiCode(String apiCode) { this.apiCode = apiCode; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getApiMethod() { return apiMethod; }
    public void setApiMethod(String apiMethod) { this.apiMethod = apiMethod; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getDataEncrypt() { return dataEncrypt; }
    public void setDataEncrypt(Integer dataEncrypt) { this.dataEncrypt = dataEncrypt; }

    public String getHeaders() { return headers; }
    public void setHeaders(String headers) { this.headers = headers; }

    public String getBuildBody() { return buildBody; }
    public void setBuildBody(String buildBody) { this.buildBody = buildBody; }

    public String getQueryParams() { return queryParams; }
    public void setQueryParams(String queryParams) { this.queryParams = queryParams; }

    public String getNormalize() { return normalize; }
    public void setNormalize(String normalize) { this.normalize = normalize; }

    public String getResponseType() { return responseType; }
    public void setResponseType(String responseType) { this.responseType = responseType; }

    public String getPageField() { return pageField; }
    public void setPageField(String pageField) { this.pageField = pageField; }

    public String getPageSizeField() { return pageSizeField; }
    public void setPageSizeField(String pageSizeField) { this.pageSizeField = pageSizeField; }

    public Integer getDefaultPageSize() { return defaultPageSize; }
    public void setDefaultPageSize(Integer defaultPageSize) { this.defaultPageSize = defaultPageSize; }

    public String getApiType() { return apiType; }
    public void setApiType(String apiType) { this.apiType = apiType; }

    public String getEntryPath() { return entryPath; }
    public void setEntryPath(String entryPath) { this.entryPath = entryPath; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getUsePageSign() { return usePageSign; }
    public void setUsePageSign(Integer usePageSign) { this.usePageSign = usePageSign; }

    public String getSignScript() { return signScript; }
    public void setSignScript(String signScript) { this.signScript = signScript; }

    public String getEncryptDataPath() { return encryptDataPath; }
    public void setEncryptDataPath(String encryptDataPath) { this.encryptDataPath = encryptDataPath; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("apiId", getApiId())
            .append("platformId", getPlatformId())
            .append("apiName", getApiName())
            .append("apiCode", getApiCode())
            .append("apiUrl", getApiUrl())
            .append("apiMethod", getApiMethod())
            .append("dataEncrypt", getDataEncrypt())
            .append("apiType", getApiType())
            .append("isActive", getIsActive())
            .append("usePageSign", getUsePageSign())
            .append("signScript", getSignScript())
            .toString();
    }
}
