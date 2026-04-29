package com.ruoyi.yy.service;

import java.util.List;
import com.ruoyi.yy.domain.YyPlatformApi;

/**
 * 平台API配置Service接口
 * 
 * @author ruoyi
 * @date 2026-04-03
 */
public interface IYyPlatformApiService 
{
    /**
     * 查询平台API配置
     * 
     * @param apiId 平台API配置主键
     * @return 平台API配置
     */
    public YyPlatformApi selectYyPlatformApiByApiId(Long apiId);

    /**
     * 查询平台API配置列表
     * 
     * @param yyPlatformApi 平台API配置
     * @return 平台API配置集合
     */
    public List<YyPlatformApi> selectYyPlatformApiList(YyPlatformApi yyPlatformApi);

    /**
     * 新增平台API配置
     * 
     * @param yyPlatformApi 平台API配置
     * @return 结果
     */
    public int insertYyPlatformApi(YyPlatformApi yyPlatformApi);

    /**
     * 修改平台API配置
     * 
     * @param yyPlatformApi 平台API配置
     * @return 结果
     */
    public int updateYyPlatformApi(YyPlatformApi yyPlatformApi);

    /**
     * 批量删除平台API配置
     * 
     * @param apiIds 需要删除的平台API配置主键集合
     * @return 结果
     */
    public int deleteYyPlatformApiByApiIds(Long[] apiIds);

    /**
     * 删除平台API配置信息
     * 
     * @param apiId 平台API配置主键
     * @return 结果
     */
    public int deleteYyPlatformApiByApiId(Long apiId);
}
