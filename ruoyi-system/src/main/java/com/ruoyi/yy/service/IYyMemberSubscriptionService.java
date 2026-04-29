package com.ruoyi.yy.service;

import java.util.List;
import java.math.BigDecimal;
import com.ruoyi.yy.domain.YyMemberSubscription;

/**
 * 会员订阅订单Service接口
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public interface IYyMemberSubscriptionService 
{
    /**
     * 查询会员订阅订单
     * 
     * @param subId 会员订阅订单主键
     * @return 会员订阅订单
     */
    public YyMemberSubscription selectYyMemberSubscriptionBySubId(Long subId);

    /**
     * 查询会员订阅订单列表
     * 
     * @param yyMemberSubscription 会员订阅订单
     * @return 会员订阅订单集合
     */
    public List<YyMemberSubscription> selectYyMemberSubscriptionList(YyMemberSubscription yyMemberSubscription);

    /**
     * 新增会员订阅订单
     * 
     * @param yyMemberSubscription 会员订阅订单
     * @return 结果
     */
    public int insertYyMemberSubscription(YyMemberSubscription yyMemberSubscription);

    /**
     * 修改会员订阅订单
     * 
     * @param yyMemberSubscription 会员订阅订单
     * @return 结果
     */
    public int updateYyMemberSubscription(YyMemberSubscription yyMemberSubscription);

    /**
     * 创建预付款订单（医药助手前端用）
     *
     * <p>创建时会检查：
     * 1. 套餐是否存在且已上架
     * 2. 该用户是否已存在同类型待支付订单（防止重复下单）
     * 3. 如果用户当前在会员有效期内，检查是否允许"降级"到本套餐
     *
     * @param userId 用户ID
     * @param tierId 套餐ID
     * @return 新建的预付款订单
     * @throws com.ruoyi.common.exception.ServiceException 校验失败时抛出
     */
    public YyMemberSubscription createPreOrder(Long userId, Long tierId);

    /**
     * 删除会员订阅订单信息
     * 
     * @param subId 会员订阅订单主键
     * @return 结果
     */
    public int deleteYyMemberSubscriptionBySubId(Long subId);
    
    /**
     * 批量删除会员订阅订单
     * 
     * @param subIds 需要删除的会员订阅订单主键集合
     * @return 结果
     */
    public int deleteYyMemberSubscriptionBySubIds(Long[] subIds);
}
