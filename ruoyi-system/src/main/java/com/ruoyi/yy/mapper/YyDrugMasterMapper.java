package com.ruoyi.yy.mapper;

import com.ruoyi.yy.domain.YyDrugMaster;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 药品主数据Mapper接口
 */
public interface YyDrugMasterMapper {

    YyDrugMaster selectYyDrugMasterById(Long id);

    YyDrugMaster selectYyDrugMasterByDrugCode(String drugCode);

    YyDrugMaster selectYyDrugMasterByBarcode(String barcode);

    YyDrugMaster selectYyDrugMasterByApprovalNumber(String approvalNumber);

    /**
     * 按归一化通用名+规格查询候选药品（前缀匹配，走索引）
     */
    List<YyDrugMaster> selectYyDrugMasterCandidates(
        @Param("commonName") String commonName,
        @Param("specification") String specification
    );

    /**
     * 回退：全模糊匹配（不走索引，仅当前缀匹配无结果时使用）
     */
    List<YyDrugMaster> selectYyDrugMasterCandidatesFallback(
        @Param("commonName") String commonName,
        @Param("specification") String specification
    );

    /**
     * 批量按ID查询（消除N+1）
     */
    List<YyDrugMaster> selectYyDrugMasterByIds(@Param("ids") List<Long> ids);

    int insertYyDrugMaster(YyDrugMaster record);

    int updateYyDrugMaster(YyDrugMaster record);
}
