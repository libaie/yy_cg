package com.ruoyi.yy.service.impl;

import java.util.List;
import java.util.Date;
import java.util.Calendar;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.yy.mapper.YyMemberSubscriptionMapper;
import com.ruoyi.yy.mapper.YyMemberTierMapper;
import com.ruoyi.yy.mapper.YyUserMapper;
import com.ruoyi.yy.domain.YyMemberSubscription;
import com.ruoyi.yy.domain.YyMemberTier;
import com.ruoyi.yy.domain.YyUser;
import com.ruoyi.yy.service.IYyMemberSubscriptionService;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 会员订阅订单Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
@Service
public class YyMemberSubscriptionServiceImpl implements IYyMemberSubscriptionService 
{
    private static final Logger log = LoggerFactory.getLogger(YyMemberSubscriptionServiceImpl.class);

    /** 预付款订单等待支付超时时间（分钟），参考微信/支付宝默认30分钟 */
    private static final int PAY_TIMEOUT_MINUTES = 30;

    @Autowired
    private YyMemberSubscriptionMapper yyMemberSubscriptionMapper;

    @Autowired
    private YyMemberTierMapper yyMemberTierMapper;

    @Autowired
    private YyUserMapper yyUserMapper;

    /**
     * 查询会员订阅订单
     * 
     * @param subId 会员订阅订单主键
     * @return 会员订阅订单
     */
    @Override
    public YyMemberSubscription selectYyMemberSubscriptionBySubId(Long subId)
    {
        return yyMemberSubscriptionMapper.selectYyMemberSubscriptionBySubId(subId);
    }

    /**
     * 查询会员订阅订单列表
     * 
     * @param yyMemberSubscription 会员订阅订单
     * @return 会员订阅订单
     */
    @Override
    public List<YyMemberSubscription> selectYyMemberSubscriptionList(YyMemberSubscription yyMemberSubscription)
    {
        return yyMemberSubscriptionMapper.selectYyMemberSubscriptionList(yyMemberSubscription);
    }

    /**
     * 新增会员订阅订单
     * 
     * @param yyMemberSubscription 会员订阅订单
     * @return 结果
     */
    @Override
    public int insertYyMemberSubscription(YyMemberSubscription yyMemberSubscription)
    {
        yyMemberSubscription.setCreateTime(DateUtils.getNowDate());
        return yyMemberSubscriptionMapper.insertYyMemberSubscription(yyMemberSubscription);
    }

    /**
     * 修改会员订阅订单
     * 
     * @param yyMemberSubscription 会员订阅订单
     * @return 结果
     */
    @Override
    public int updateYyMemberSubscription(YyMemberSubscription yyMemberSubscription)
    {
        return yyMemberSubscriptionMapper.updateYyMemberSubscription(yyMemberSubscription);
    }

    /**
     * 批量删除会员订阅订单
     * 
     * @param subIds 需要删除的会员订阅订单主键
     * @return 结果
     */
    @Override
    public int deleteYyMemberSubscriptionBySubIds(Long[] subIds)
    {
        return yyMemberSubscriptionMapper.deleteYyMemberSubscriptionBySubIds(subIds);
    }

    /**
     * 删除会员订阅订单信息
     * 
     * @param subId 会员订阅订单主键
     * @return 结果
     */
    @Override
    public int deleteYyMemberSubscriptionBySubId(Long subId)
    {
        return yyMemberSubscriptionMapper.deleteYyMemberSubscriptionBySubId(subId);
    }

    /**
     * 创建预付款订单
     * 
     * 业务规则：
     * 1. 套餐必须存在且已上架
     * 2. 如果已存在同类型的支付中订单，直接返回提示（优先级最高）
     * 3. 如果用户当前在会员有效期内，只能订阅同等级或更高等级的套餐（禁止降级）
     * 4. 如果用户已过期或普通会员，可以订阅任意套餐
     * 
     * @param userId 用户ID
     * @param tierId 套餐ID
     * @return 新建的预付款订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public YyMemberSubscription createPreOrder(Long userId, Long tierId)
    {
        // 1. 校验套餐是否存在且已上架
        YyMemberTier tier = yyMemberTierMapper.selectYyMemberTierByTierId(tierId);
        if (tier == null) {
            throw new ServiceException("会员套餐不存在");
        }
        if (tier.getIsActive() == null || tier.getIsActive() != 1) {
            throw new ServiceException("该会员套餐暂未上架");
        }

        // 2. 检查是否存在同类型的支付中订单（优先级最高，有则直接提示）
        int payingCount = yyMemberSubscriptionMapper.selectCountPayingByUserAndTier(userId, tierId);
        if (payingCount > 0) {
            throw new ServiceException("已存在该套餐的待支付订单，请先完成支付或等待超时后重试");
        }

        // 3. 降级检查：如果用户当前在会员有效期内，只能升级或续费同等级/更高等级
        YyUser user = yyUserMapper.selectYyUserByUserId(userId);
        if (user != null && user.getMemberLevel() != null && user.getMemberLevel() > 0) {
            boolean isActive = user.getMemberExpireTime() != null && user.getMemberExpireTime().after(new Date());
            if (isActive) {
                Long currentLevel = user.getMemberLevel();
                Integer targetLevel = tier.getMemberLevel();
                if (targetLevel != null && currentLevel != null && targetLevel.longValue() < currentLevel.longValue()) {
                    String currentLevelName = getMemberLevelName(currentLevel);
                    String targetLevelName = getMemberLevelName(targetLevel != null ? targetLevel.longValue() : null);
                    throw new ServiceException("您当前是" + currentLevelName + "会员，有效期至"
                        + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, user.getMemberExpireTime())
                        + "，不能订阅更低等级的" + targetLevelName + "套餐，请等待当前会员到期后再操作");
                }
            }
        }

        // 4. 生成预付款订单
        YyMemberSubscription order = new YyMemberSubscription();
        order.setUserId(userId);
        order.setTierId(tierId);
        order.setPayStatus(3); // 支付中

        // 5. 计算支付超时时间（当前时间 + 30分钟）
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, PAY_TIMEOUT_MINUTES);
        order.setPayExpireTime(cal.getTime());

        // 6. 生成订单号
        String orderNo = "YY" + DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS, new Date()) + IdUtils.fastUUID().substring(0, 8);
        order.setOrderNo(orderNo);
        order.setCreateTime(DateUtils.getNowDate());

        // 7. 原子插入：双重保险，防并发重复下单（即使第2步检查过，并发请求仍可能同时通过）
        int rows = yyMemberSubscriptionMapper.insertIfNoPaying(order);
        if (rows == 0) {
            throw new ServiceException("已存在该套餐的待支付订单，请先完成支付或等待超时后重试");
        }
        log.info("预付款订单创建成功: userId={}, tierId={}, orderNo={}, 超时时间={}", userId, tierId, orderNo, order.getPayExpireTime());
        return order;
    }

    /**
     * 根据会员等级数字获取中文名称（从字典 yy_member_tier_name 获取）
     */
    private String getMemberLevelName(Long level) {
        if (level == null) return "未知";
        String label = DictUtils.getDictLabel("yy_member_tier_name", level.toString());
        return StringUtils.isNotEmpty(label) ? label : "未知";
    }

    /**
     * 定时任务：每分钟扫描超时的支付中订单，批量更新为未支付
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void cancelExpiredOrders()
    {
        int rows = yyMemberSubscriptionMapper.cancelTimeoutOrders(0);
        if (rows > 0) {
            log.info("定时任务：已将 {} 个超时的支付中订单批量更新为未支付", rows);
        }
    }

    /**
     * 定时任务：每5分钟扫描过期会员，批量降级为普通会员
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void downgradeExpiredMembers()
    {
        int rows = yyUserMapper.batchDowngradeExpiredMembers();
        if (rows > 0) {
            log.info("定时任务：已将 {} 个过期会员批量降级为普通会员", rows);
        }
    }
}
