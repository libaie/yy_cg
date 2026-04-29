package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyPlatformActivity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 平台活动 Mapper
 */
public interface YyPlatformActivityMapper {
    
    /**
     * 查询平台活动
     */
    YyPlatformActivity selectYyPlatformActivityById(Long id);
    
    /**
     * 按编码查询
     */
    YyPlatformActivity selectByCode(
        @Param("platformCode") String platformCode,
        @Param("activityCode") String activityCode
    );
    
    /**
     * 查询所有活动
     */
    List<YyPlatformActivity> selectAll(@Param("platformCode") String platformCode);
    
    /**
     * 查询有效活动
     */
    List<YyPlatformActivity> selectActive(@Param("platformCode") String platformCode);
    
    /**
     * 按类型查询有效活动
     */
    List<YyPlatformActivity> selectActiveByPlatformAndType(
        @Param("platformCode") String platformCode,
        @Param("activityType") String activityType
    );
    
    /**
     * 插入活动
     */
    int insertYyPlatformActivity(YyPlatformActivity yyPlatformActivity);
    
    /**
     * 更新活动
     */
    int updateYyPlatformActivity(YyPlatformActivity yyPlatformActivity);
    
    /**
     * 删除活动
     */
    int deleteYyPlatformActivityById(Long id);
    
    /**
     * 批量删除
     */
    int deleteYyPlatformActivityByIds(Long[] ids);
}
