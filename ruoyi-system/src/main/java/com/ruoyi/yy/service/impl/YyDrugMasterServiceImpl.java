package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyDrugMaster;
import com.ruoyi.yy.mapper.YyDrugMasterMapper;
import com.ruoyi.yy.service.IYyDrugMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

/**
 * 药品主数据Service业务层处理
 */
@Service
public class YyDrugMasterServiceImpl implements IYyDrugMasterService {

    @Autowired
    private YyDrugMasterMapper drugMasterMapper;

    @Override
    public YyDrugMaster selectById(Long id) {
        return drugMasterMapper.selectYyDrugMasterById(id);
    }

    @Override
    public YyDrugMaster selectByDrugCode(String drugCode) {
        return drugMasterMapper.selectYyDrugMasterByDrugCode(drugCode);
    }

    @Override
    public YyDrugMaster selectByBarcode(String barcode) {
        return drugMasterMapper.selectYyDrugMasterByBarcode(barcode);
    }

    @Override
    public YyDrugMaster selectByApprovalNumber(String approvalNumber) {
        return drugMasterMapper.selectYyDrugMasterByApprovalNumber(approvalNumber);
    }

    @Override
    public List<YyDrugMaster> selectCandidates(String commonName, String specification) {
        if ((commonName == null || commonName.isEmpty()) && (specification == null || specification.isEmpty())) {
            return Collections.emptyList();
        }
        return drugMasterMapper.selectYyDrugMasterCandidates(commonName, specification);
    }

    @Override
    public List<YyDrugMaster> selectCandidatesFallback(String commonName, String specification) {
        if ((commonName == null || commonName.isEmpty()) && (specification == null || specification.isEmpty())) {
            return Collections.emptyList();
        }
        return drugMasterMapper.selectYyDrugMasterCandidatesFallback(commonName, specification);
    }

    @Override
    public List<YyDrugMaster> selectByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return drugMasterMapper.selectYyDrugMasterByIds(ids);
    }

    @Override
    public int insert(YyDrugMaster record) {
        return drugMasterMapper.insertYyDrugMaster(record);
    }

    @Override
    public int updateById(YyDrugMaster record) {
        return drugMasterMapper.updateYyDrugMaster(record);
    }
}
