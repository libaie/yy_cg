package com.ruoyi.yy.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.yy.mapper.YyPlatformApiMapper;
import com.ruoyi.yy.domain.YyPlatformApi;
import com.ruoyi.yy.service.IYyPlatformApiService;

/**
 * 平台API配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-04-03
 */
@Service
public class YyPlatformApiServiceImpl implements IYyPlatformApiService 
{
    @Autowired
    private YyPlatformApiMapper yyPlatformApiMapper;

    /**
     * 查询平台API配置
     * 
     * @param apiId 平台API配置主键
     * @return 平台API配置
     */
    @Override
    public YyPlatformApi selectYyPlatformApiByApiId(Long apiId)
    {
        return yyPlatformApiMapper.selectYyPlatformApiByApiId(apiId);
    }

    /**
     * 查询平台API配置列表
     * 
     * @param yyPlatformApi 平台API配置
     * @return 平台API配置
     */
    @Override
    public List<YyPlatformApi> selectYyPlatformApiList(YyPlatformApi yyPlatformApi)
    {
        return yyPlatformApiMapper.selectYyPlatformApiList(yyPlatformApi);
    }

    /**
     * 新增平台API配置
     * 
     * @param yyPlatformApi 平台API配置
     * @return 结果
     */
    @Override
    public int insertYyPlatformApi(YyPlatformApi yyPlatformApi)
    {
        yyPlatformApi.setCreateTime(DateUtils.getNowDate());
        return yyPlatformApiMapper.insertYyPlatformApi(yyPlatformApi);
    }

    /**
     * 修改平台API配置
     * 
     * @param yyPlatformApi 平台API配置
     * @return 结果
     */
    @Override
    public int updateYyPlatformApi(YyPlatformApi yyPlatformApi)
    {
        yyPlatformApi.setUpdateTime(DateUtils.getNowDate());
        return yyPlatformApiMapper.updateYyPlatformApi(yyPlatformApi);
    }

    /**
     * 批量删除平台API配置
     * 
     * @param apiIds 需要删除的平台API配置主键
     * @return 结果
     */
    @Override
    public int deleteYyPlatformApiByApiIds(Long[] apiIds)
    {
        return yyPlatformApiMapper.deleteYyPlatformApiByApiIds(apiIds);
    }

    /**
     * 删除平台API配置信息
     * 
     * @param apiId 平台API配置主键
     * @return 结果
     */
    @Override
    public int deleteYyPlatformApiByApiId(Long apiId)
    {
        return yyPlatformApiMapper.deleteYyPlatformApiByApiId(apiId);
    }
}
