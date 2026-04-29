package com.ruoyi.yy.service;

import java.util.List;
import com.ruoyi.yy.domain.YyUserPlatform;

/**
 * 用户平台关联Service接口
 * 
 * @author ruoyi
 * @date 2026-04-03
 */
public interface IYyUserPlatformService 
{
    /**
     * 查询用户平台关联
     * 
     * @param id 用户平台关联主键
     * @return 用户平台关联
     */
    public YyUserPlatform selectYyUserPlatformById(Long id);

    /**
     * 查询用户平台关联列表
     * 
     * @param yyUserPlatform 用户平台关联
     * @return 用户平台关联集合
     */
    public List<YyUserPlatform> selectYyUserPlatformList(YyUserPlatform yyUserPlatform);

    /**
     * 新增用户平台关联
     * 
     * @param yyUserPlatform 用户平台关联
     * @return 结果
     */
    public int insertYyUserPlatform(YyUserPlatform yyUserPlatform);

    /**
     * 修改用户平台关联
     * 
     * @param yyUserPlatform 用户平台关联
     * @return 结果
     */
    public int updateYyUserPlatform(YyUserPlatform yyUserPlatform);

    /**
     * 批量删除用户平台关联
     * 
     * @param ids 需要删除的用户平台关联主键集合
     * @return 结果
     */
    public int deleteYyUserPlatformByIds(Long[] ids);

    /**
     * 删除用户平台关联信息
     * 
     * @param id 用户平台关联主键
     * @return 结果
     */
    public int deleteYyUserPlatformById(Long id);
}
