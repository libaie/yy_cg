package com.ruoyi.yy.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.yy.domain.YyUserPlatform;

/**
 * 用户平台关联Mapper接口
 * 
 * @author ruoyi
 * @date 2026-04-03
 */
public interface YyUserPlatformMapper 
{
    public YyUserPlatform selectYyUserPlatformById(Long id);

    public List<YyUserPlatform> selectYyUserPlatformList(YyUserPlatform yyUserPlatform);

    /** 根据用户ID和平台ID查询绑定记录 */
    public YyUserPlatform selectByUserIdAndPlatformId(@Param("userId") Long userId, @Param("platformId") Long platformId);

    /** 根据用户ID查询所有绑定记录 */
    public List<YyUserPlatform> selectByUserId(Long userId);

    /** 查询所有已绑定且有Token的记录（用于定时任务校验Token过期） */
    public List<YyUserPlatform> selectBoundWithToken();

    public int insertYyUserPlatform(YyUserPlatform yyUserPlatform);

    public int updateYyUserPlatform(YyUserPlatform yyUserPlatform);

    public int deleteYyUserPlatformById(Long id);

    public int deleteYyUserPlatformByIds(Long[] ids);
}
