package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyUserEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 用户行为事件Mapper接口
 */
public interface YyUserEventMapper {

    int insert(YyUserEvent event);

    List<YyUserEvent> selectByUserId(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    int countByEventType(
        @Param("eventType") String eventType,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
