package com.ruoyi.yy.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.yy.mapper.YyMemberTierMapper;
import com.ruoyi.yy.mapper.YyMemberSubscriptionMapper;
import com.ruoyi.yy.domain.YyMemberTier;
import com.ruoyi.yy.service.IYyMemberTierService;

/**
 * 会员套餐配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
@Service
public class YyMemberTierServiceImpl implements IYyMemberTierService 
{
    @Autowired
    private YyMemberTierMapper yyMemberTierMapper;

    @Autowired
    private YyMemberSubscriptionMapper subscriptionMapper;

    /**
     * 查询会员套餐配置
     * 
     * @param tierId 会员套餐配置主键
     * @return 会员套餐配置
     */
    @Override
    public YyMemberTier selectYyMemberTierByTierId(Long tierId)
    {
        return yyMemberTierMapper.selectYyMemberTierByTierId(tierId);
    }

    /**
     * 查询会员套餐配置列表
     * 
     * @param yyMemberTier 会员套餐配置
     * @return 会员套餐配置
     */
    @Override
    public List<YyMemberTier> selectYyMemberTierList(YyMemberTier yyMemberTier)
    {
        return yyMemberTierMapper.selectYyMemberTierList(yyMemberTier);
    }

    /**
     * 新增会员套餐配置
     * 
     * @param yyMemberTier 会员套餐配置
     * @return 结果
     */
    @Override
    public int insertYyMemberTier(YyMemberTier yyMemberTier)
    {
        syncTierName(yyMemberTier);
        yyMemberTier.setCreateTime(DateUtils.getNowDate());
        return yyMemberTierMapper.insertYyMemberTier(yyMemberTier);
    }

    /**
     * 修改会员套餐配置
     * 
     * @param yyMemberTier 会员套餐配置
     * @return 结果
     */
    @Override
    public int updateYyMemberTier(YyMemberTier yyMemberTier)
    {
        syncTierName(yyMemberTier);
        return yyMemberTierMapper.updateYyMemberTier(yyMemberTier);
    }

    /**
     * 根据会员等级自动同步套餐名称（从字典表获取）
     */
    private void syncTierName(YyMemberTier tier) {
        if (tier.getMemberLevel() != null) {
            String dictLabel = DictUtils.getDictLabel("yy_member_tier_name", String.valueOf(tier.getMemberLevel()));
            if (dictLabel != null && !dictLabel.isEmpty()) {
                tier.setTierName(dictLabel);
            }
        }
    }

    /**
     * 批量删除会员套餐配置（含关联数据校验）
     * 
     * @param tierIds 需要删除的会员套餐配置主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteYyMemberTierByTierIds(Long[] tierIds)
    {
        for (Long tierId : tierIds) {
            int refCount = subscriptionMapper.countByTierId(tierId);
            if (refCount > 0) {
                throw new ServiceException("套餐(ID:" + tierId + ")已被" + refCount + "条订阅记录关联，无法删除");
            }
        }
        return yyMemberTierMapper.deleteYyMemberTierByTierIds(tierIds);
    }

    /**
     * 删除会员套餐配置信息（含关联数据校验）
     * 
     * @param tierId 会员套餐配置主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteYyMemberTierByTierId(Long tierId)
    {
        int refCount = subscriptionMapper.countByTierId(tierId);
        if (refCount > 0) {
            throw new ServiceException("套餐(ID:" + tierId + ")已被" + refCount + "条订阅记录关联，无法删除");
        }
        return yyMemberTierMapper.deleteYyMemberTierByTierId(tierId);
    }
}
