package com.ruoyi.yy.service;

import com.ruoyi.yy.domain.YyDrugMaster;
import java.util.List;

/**
 * 药品主数据Service接口
 */
public interface IYyDrugMasterService {

    YyDrugMaster selectById(Long id);

    YyDrugMaster selectByDrugCode(String drugCode);

    YyDrugMaster selectByBarcode(String barcode);

    YyDrugMaster selectByApprovalNumber(String approvalNumber);

    List<YyDrugMaster> selectCandidates(String commonName, String specification);

    List<YyDrugMaster> selectCandidatesFallback(String commonName, String specification);

    List<YyDrugMaster> selectByIds(List<Long> ids);

    int insert(YyDrugMaster record);

    int updateById(YyDrugMaster record);
}
