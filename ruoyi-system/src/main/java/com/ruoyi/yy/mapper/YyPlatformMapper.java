package com.ruoyi.yy.mapper;

import java.util.List;
import com.ruoyi.yy.domain.YyPlatform;

/**
 * 平台信息Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public interface YyPlatformMapper 
{
    /**
     * 查询平台信息
     * 
     * @param pId 平台信息主键
     * @return 平台信息
     */
    public YyPlatform selectYyPlatformByPId(Long pId);

    /**
     * 根据平台编码查询平台信息
     * 
     * @param platformCode 平台编码
     * @return 平台信息
     */
    public YyPlatform selectYyPlatformByCode(String platformCode);

    /**
     * 查询平台信息列表
     * 
     * @param yyPlatform 平台信息
     * @return 平台信息集合
     */
    public List<YyPlatform> selectYyPlatformList(YyPlatform yyPlatform);

    /**
     * 新增平台信息
     * 
     * @param yyPlatform 平台信息
     * @return 结果
     */
    public int insertYyPlatform(YyPlatform yyPlatform);

    /**
     * 修改平台信息
     * 
     * @param yyPlatform 平台信息
     * @return 结果
     */
    public int updateYyPlatform(YyPlatform yyPlatform);

    /**
     * 删除平台信息
     * 
     * @param pId 平台信息主键
     * @return 结果
     */
    public int deleteYyPlatformByPId(Long pId);

    /**
     * 批量删除平台信息
     * 
     * @param pIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteYyPlatformByPIds(Long[] pIds);

    /**
     * 根据用户ID查询用户绑定的平台
     *
     * @param userId 用户ID
     * @return 平台信息集合
     */
    public List<YyPlatform> selectPlatformsByUserId(Long userId);

    /**
     * 根据用户ID和平台条件查询平台列表
     *
     * @param params 查询参数（包含platform对象和userId）
     * @return 平台信息集合
     */
    public List<YyPlatform> selectYyPlatformListByUserId(java.util.Map<String, Object> params);
}
