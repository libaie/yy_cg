package com.ruoyi.yy.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.yy.domain.YyMemberSubscription;

/**
 * 会员订阅订单Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public interface YyMemberSubscriptionMapper 
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
     * 删除会员订阅订单
     * 
     * @param subId 会员订阅订单主键
     * @return 结果
     */
    public int deleteYyMemberSubscriptionBySubId(Long subId);

    /**
     * 批量删除会员订阅订单
     * 
     * @param subIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteYyMemberSubscriptionBySubIds(Long[] subIds);

    /**
     * 超时自动取消订单 (MySQL)
     * 
     * @param timeoutMinutes 超时分钟数
     * @return 影响行数
     */
    public int cancelTimeoutOrders(int timeoutMinutes);

    /**
     * 根据订单号查询订阅记录
     * 
     * @param orderNo 订单号
     * @return 订阅记录
     */
    public YyMemberSubscription selectYyMemberSubscriptionByOrderNo(String orderNo);

    /**
     * 统计用户购买某个套餐成功的次数
     * 
     * @param userId 用户ID
     * @param tierId 套餐ID
     * @return 购买次数
     */
    public int countUserTierOrders(@Param("userId") Long userId, @Param("tierId") Long tierId);

    /**
     * 统计关联某个套餐的订阅记录数（用于删除套餐前的校验）
     * 
     * @param tierId 套餐ID
     * @return 订阅记录数
     */
    public int countByTierId(Long tierId);

    /**
     * 查询用户对某套餐是否存在支付中的订单
     * 
     * @param userId 用户ID
     * @param tierId 套餐ID
     * @return 支付中订单数量
     */
    public int selectCountPayingByUserAndTier(@Param("userId") Long userId, @Param("tierId") Long tierId);

    /**
     * 原子插入预付款订单：仅当不存在同用户同套餐的支付中订单时才插入
     * 
     * @param yyMemberSubscription 订单对象
     * @return 受影响行数（0=存在冲突未插入, 1=插入成功）
     */
    public int insertIfNoPaying(YyMemberSubscription yyMemberSubscription);

    /**
     * 取消用户同类型的所有支付中订单（支付成功后清理）
     * 
     * @param userId 用户ID
     * @param tierId 套餐ID
     * @param excludeSubId 排除的订单ID（当前已支付的订单）
     * @return 受影响行数
     */
    public int cancelOtherPayingOrders(@Param("userId") Long userId, @Param("tierId") Long tierId, @Param("excludeSubId") Long excludeSubId);
}
