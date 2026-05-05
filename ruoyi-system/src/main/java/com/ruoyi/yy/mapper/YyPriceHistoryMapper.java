package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyPriceHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 价格历史Mapper接口
 */
public interface YyPriceHistoryMapper {

    int insert(YyPriceHistory history);

    int batchInsert(List<YyPriceHistory> list);

    List<YyPriceHistory> selectByPlatformAndSku(
        @Param("sourcePlatform") String sourcePlatform,
        @Param("skuId") String skuId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    int deleteOlderThan(@Param("beforeTime") LocalDateTime beforeTime);
}
