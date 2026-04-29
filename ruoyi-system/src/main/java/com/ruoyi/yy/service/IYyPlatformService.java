package com.ruoyi.yy.service;

import java.util.List;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.yy.domain.YyUserPlatform;

/**
 * 平台信息Service接口
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public interface IYyPlatformService 
{
    public YyPlatform selectYyPlatformByPId(Long pId);

    public YyPlatform selectYyPlatformByCode(String platformCode);

    public List<YyPlatform> selectYyPlatformList(YyPlatform yyPlatform);

    /** 获取所有启用的平台（含API配置），前端比价用 */
    public List<YyPlatform> selectActivePlatformsWithApis();

    public List<YyPlatform> selectActivePlatformsWithApis(Long userId);

    public int insertYyPlatform(YyPlatform yyPlatform);

    public int updateYyPlatform(YyPlatform yyPlatform);

    public int deleteYyPlatformByPIds(Long[] pIds);

    public int deleteYyPlatformByPId(Long pId);

    /** 绑定平台（保存Token） */
    public int bindPlatform(Long userId, YyUserPlatform bindInfo);

    /** 解绑平台（清除Token） */
    public int unbindPlatform(Long userId, Long platformId);

    /** 同步Token到后端 */
    public int syncToken(Long userId, Long platformId, String token, String tokenExpireTime, String lastLoginTime, Integer loginType);

    /** 获取当前用户的平台绑定列表 */
    public List<YyUserPlatform> selectMyPlatformList(Long userId);

    /** 获取指定平台的Token（采集用） */
    public YyUserPlatform getPlatformToken(Long userId, Long platformId);

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
     * @param userId 用户ID
     * @param platform 查询条件
     * @return 平台信息集合
     */
    public List<YyPlatform> selectYyPlatformListByUserId(Long userId, YyPlatform platform);
}
