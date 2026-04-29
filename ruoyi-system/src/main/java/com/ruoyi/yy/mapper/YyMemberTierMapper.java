package com.ruoyi.yy.mapper;

import java.util.List;
import com.ruoyi.yy.domain.YyMemberTier;

/**
 * 会员套餐配置Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public interface YyMemberTierMapper 
{
    /**
     * 查询会员套餐配置
     * 
     * @param tierId 会员套餐配置主键
     * @return 会员套餐配置
     */
    public YyMemberTier selectYyMemberTierByTierId(Long tierId);

    /**
     * 查询会员套餐配置列表
     * 
     * @param yyMemberTier 会员套餐配置
     * @return 会员套餐配置集合
     */
    public List<YyMemberTier> selectYyMemberTierList(YyMemberTier yyMemberTier);

    /**
     * 新增会员套餐配置
     * 
     * @param yyMemberTier 会员套餐配置
     * @return 结果
     */
    public int insertYyMemberTier(YyMemberTier yyMemberTier);

    /**
     * 修改会员套餐配置
     * 
     * @param yyMemberTier 会员套餐配置
     * @return 结果
     */
    public int updateYyMemberTier(YyMemberTier yyMemberTier);

    /**
     * 删除会员套餐配置
     * 
     * @param tierId 会员套餐配置主键
     * @return 结果
     */
    public int deleteYyMemberTierByTierId(Long tierId);

    /**
     * 批量删除会员套餐配置
     * 
     * @param tierIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteYyMemberTierByTierIds(Long[] tierIds);
}
