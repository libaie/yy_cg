package com.ruoyi.yy.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.yy.mapper.YyUserPlatformMapper;
import com.ruoyi.yy.domain.YyUserPlatform;
import com.ruoyi.yy.service.IYyUserPlatformService;

/**
 * 用户平台关联Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-04-03
 */
@Service
public class YyUserPlatformServiceImpl implements IYyUserPlatformService 
{
    @Autowired
    private YyUserPlatformMapper yyUserPlatformMapper;

    /**
     * 查询用户平台关联
     * 
     * @param id 用户平台关联主键
     * @return 用户平台关联
     */
    @Override
    public YyUserPlatform selectYyUserPlatformById(Long id)
    {
        return yyUserPlatformMapper.selectYyUserPlatformById(id);
    }

    /**
     * 查询用户平台关联列表
     * 
     * @param yyUserPlatform 用户平台关联
     * @return 用户平台关联
     */
    @Override
    public List<YyUserPlatform> selectYyUserPlatformList(YyUserPlatform yyUserPlatform)
    {
        return yyUserPlatformMapper.selectYyUserPlatformList(yyUserPlatform);
    }

    /**
     * 新增用户平台关联
     * 
     * @param yyUserPlatform 用户平台关联
     * @return 结果
     */
    @Override
    public int insertYyUserPlatform(YyUserPlatform yyUserPlatform)
    {
        yyUserPlatform.setCreateTime(DateUtils.getNowDate());
        return yyUserPlatformMapper.insertYyUserPlatform(yyUserPlatform);
    }

    /**
     * 修改用户平台关联
     * 
     * @param yyUserPlatform 用户平台关联
     * @return 结果
     */
    @Override
    public int updateYyUserPlatform(YyUserPlatform yyUserPlatform)
    {
        yyUserPlatform.setUpdateTime(DateUtils.getNowDate());
        return yyUserPlatformMapper.updateYyUserPlatform(yyUserPlatform);
    }

    /**
     * 批量删除用户平台关联
     * 
     * @param ids 需要删除的用户平台关联主键
     * @return 结果
     */
    @Override
    public int deleteYyUserPlatformByIds(Long[] ids)
    {
        return yyUserPlatformMapper.deleteYyUserPlatformByIds(ids);
    }

    /**
     * 删除用户平台关联信息
     * 
     * @param id 用户平台关联主键
     * @return 结果
     */
    @Override
    public int deleteYyUserPlatformById(Long id)
    {
        return yyUserPlatformMapper.deleteYyUserPlatformById(id);
    }
}
