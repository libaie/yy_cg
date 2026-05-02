package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyFusionReview;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 融合审核队列Mapper接口
 */
public interface YyFusionReviewMapper {

    YyFusionReview selectYyFusionReviewById(Long id);

    List<YyFusionReview> selectYyFusionReviewByStatus(@Param("status") String status);

    int insertYyFusionReview(YyFusionReview record);

    int updateYyFusionReviewStatus(
        @Param("id") Long id,
        @Param("status") String status,
        @Param("reviewerId") Long reviewerId,
        @Param("reviewNote") String reviewNote
    );
}
