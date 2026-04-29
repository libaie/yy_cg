package com.ruoyi.yy.service.impl;

import java.util.List;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.yy.mapper.YyMemberSubscriptionMapper;
import com.ruoyi.yy.mapper.YyMemberTierMapper;
import com.ruoyi.yy.mapper.YyUserMapper;
import com.ruoyi.yy.domain.YyMemberSubscription;
import com.ruoyi.yy.domain.YyMemberTier;
import com.ruoyi.yy.domain.YyUser;
import com.ruoyi.yy.service.IYyUserService;

/**
 * 用户Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
@Service
public class YyUserServiceImpl implements IYyUserService 
{
    private static final Logger log = LoggerFactory.getLogger(YyUserServiceImpl.class);

    @Autowired
    private YyUserMapper yyUserMapper;
    @Autowired
    private YyMemberTierMapper tierMapper;
    @Autowired
    private YyMemberSubscriptionMapper subscriptionMapper;


    /**
     * 查询用户
     * 
     * @param userId 用户主键
     * @return 用户
     */
    @Override
    public YyUser selectYyUserByUserId(Long userId)
    {
        return yyUserMapper.selectYyUserByUserId(userId);
    }

    @Override
    public YyUser selectYyUserByPhone(String phone) {
        return yyUserMapper.selectYyUserByPhone(phone);
    }

    /**
     * 查询用户列表
     * 
     * @param yyUser 用户
     * @return 用户
     */
    @Override
    public List<YyUser> selectYyUserList(YyUser yyUser)
    {
        return yyUserMapper.selectYyUserList(yyUser);
    }

    /**
     * 新增用户
     * 
     * @param yyUser 用户
     * @return 结果
     */
    @Override
    public int insertYyUser(YyUser yyUser)
    {
        yyUser.setCreateTime(DateUtils.getNowDate());
        return yyUserMapper.insertYyUser(yyUser);
    }

    /**
     * 修改用户
     * 
     * @param yyUser 用户
     * @return 结果
     */
    @Override
    public int updateYyUser(YyUser yyUser)
    {
        return yyUserMapper.updateYyUser(yyUser);
    }

    /**
     * 批量删除用户
     * 
     * @param userIds 需要删除的用户主键
     * @return 结果
     */
    @Override
    public int deleteYyUserByUserIds(Long[] userIds)
    {
        return yyUserMapper.deleteYyUserByUserIds(userIds);
    }

    /**
     * 删除用户信息
     * 
     * @param userId 用户主键
     * @return 结果
     */
    @Override
    public int deleteYyUserByUserId(Long userId)
    {
        return yyUserMapper.deleteYyUserByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int upgradeUserMember(String orderNo, String transactionId) {
        // 1. 获取订单记录
        YyMemberSubscription order = subscriptionMapper.selectYyMemberSubscriptionByOrderNo(orderNo);
        if (order == null) return 0;
        // 只处理"支付中"状态的订单
        if (order.getPayStatus() != 3) {
            log.warn("订单状态异常，无法升级: orderNo={}, payStatus={}", orderNo, order.getPayStatus());
            return 0;
        }

        // 2. 查询套餐和用户信息（带锁）
        YyMemberTier tier = tierMapper.selectYyMemberTierByTierId(order.getTierId());
        YyUser user = yyUserMapper.selectYyUserForUpdate(order.getUserId());
        if (tier == null || user == null) return 0;

        // 2.1 支付回调阶段再次核实限购（防极端并发下单场景）
        if (tier.getLimitCount() != null && tier.getLimitCount() > 0) {
            int boughtCount = subscriptionMapper.countUserTierOrders(order.getUserId(), order.getTierId());
            if (boughtCount >= tier.getLimitCount()) {
                order.setPayStatus(2); // 已取消/失效
                order.setRemark("达到限购上限，支付回调拦截");
                subscriptionMapper.updateYyMemberSubscription(order);
                return 0;
            }
        }

        Date now = new Date();
        Date oldExpireTime = user.getMemberExpireTime();
        Date newExpireTime;
        Long oldLevel = user.getMemberLevel();
        
        // 增加防御性判断：确保套餐定义的等级和天数不为空
        if (tier.getMemberLevel() == null || tier.getDurationDays() == null) {
            throw new ServiceException("套餐配置错误：会员等级或有效期不能为空");
        }
        Long newLevel = tier.getMemberLevel().longValue();

        // --- 核心业务规则逻辑 ---

        // 场景 A & B：第一次购买、过期、或同等级续费
        if (oldExpireTime != null && oldExpireTime.after(now)) {
            // 场景 C：同等级续费 -> 累加时长
            if (oldLevel != null && oldLevel.equals(newLevel)) {
                newExpireTime = DateUtils.addDays(oldExpireTime, tier.getDurationDays().intValue());
            } 
            // 场景 C：升级权益 -> 重新计算到期时间
            else if (oldLevel != null && oldLevel < newLevel) {
                newExpireTime = DateUtils.addDays(now, tier.getDurationDays().intValue());
            } 
            // 场景 E：降级购买 -> 不支持
            else {
                log.warn("🚫 降级购买被拒绝 - userId: {}, oldLevel: {}, newLevel: {}", 
                         user.getUserId(), oldLevel, newLevel);
                throw new ServiceException("不支持降级购买，当前会员等级高于所选套餐");
            }
        } 
        else {
            newExpireTime = DateUtils.addDays(now, tier.getDurationDays().intValue());
        }

        // 3. 更新用户信息
        YyUser updateUser = new YyUser();
        updateUser.setUserId(user.getUserId());
        updateUser.setMemberLevel(newLevel);
        updateUser.setMemberLevelId(tier.getTierId());
        updateUser.setMemberExpireTime(newExpireTime);
        if (user.getFirstMemberTime() == null) {
            updateUser.setFirstMemberTime(now);
        }
        updateUser.setLastPayTime(now);
        yyUserMapper.updateYyUser(updateUser);

        // 4. 清理该用户同类型的其他支付中订单（避免残留待支付订单）
        subscriptionMapper.cancelOtherPayingOrders(order.getUserId(), order.getTierId(), order.getSubId());

        // 5. 更新订单状态为已支付，并存入第三方流水号
        order.setPayStatus(1);
        order.setStartTime(now);
        order.setEndTime(newExpireTime);
        order.setTransactionId(transactionId);
        order.setPayTime(now);
        log.info("会员升级成功: userId={}, orderNo={}, level={}, expireTime={}", order.getUserId(), orderNo, newLevel, newExpireTime);
        return subscriptionMapper.updateYyMemberSubscription(order);
    }

    @Override
    public String login(String phone, String password, String code, String loginType) {
        return null;
    }

    @Override
    public boolean register(YyUser user, String code) {
        // 校验手机号是否已注册
        if (yyUserMapper.selectYyUserByPhone(user.getPhone()) != null) {
            throw new ServiceException("该手机号已注册");
        }

        // 校验注册时填写的推荐码是否合法
        if (StringUtils.isNotEmpty(user.getReferrerCode())) {
            YyUser referrer = yyUserMapper.selectYyUserByInviteCode(user.getReferrerCode());
            if (referrer == null) {
                throw new ServiceException("填写的推荐码无效");
            }
        }

        // 注意：密码加密由上层调用方 YyLoginService.register() 处理，此处不做重复加密

        // 生成该用户自己的唯一邀请码 (8位大写字母+数字，降低碰撞概率)
        String inviteCode = StringUtils.upperCase(IdUtils.fastSimpleUUID().substring(0, 8));
        int attempts = 0;
        while (yyUserMapper.selectYyUserByInviteCode(inviteCode) != null && attempts < 5) {
            inviteCode = StringUtils.upperCase(IdUtils.fastSimpleUUID().substring(0, 8));
            attempts++;
        }
        if (attempts >= 5) {
            throw new ServiceException("邀请码生成失败，请重试");
        }
        
        // 防御：生成的码恰好等于填的推荐码（概率极低但需拦截）
        if (inviteCode.equalsIgnoreCase(user.getReferrerCode())) {
             inviteCode = StringUtils.upperCase(IdUtils.fastSimpleUUID().substring(8, 16));
        }
        
        user.setInviteCode(inviteCode);
        user.setMemberLevel(0L);
        user.setRegTime(new Date());
        user.setCreateTime(new Date());
        return yyUserMapper.insertYyUser(user) > 0;
    }

    @Override
    public String getOrGenerateInviteCode(Long userId) {
        YyUser user = yyUserMapper.selectYyUserByUserId(userId);
        if (user == null) {
            throw new ServiceException("当前用户不存在");
        }
        
        // 如果已经有邀请码，直接返回
        if (StringUtils.isNotEmpty(user.getInviteCode())) {
            return user.getInviteCode();
        }

        // 如果是历史老用户没有邀请码，则生成并保存
        String newInviteCode = StringUtils.upperCase(IdUtils.fastSimpleUUID().substring(0, 8));
        int attempts = 0;
        while (yyUserMapper.selectYyUserByInviteCode(newInviteCode) != null && attempts < 5) {
            newInviteCode = StringUtils.upperCase(IdUtils.fastSimpleUUID().substring(0, 8));
            attempts++;
        }
        if (attempts >= 5) {
            throw new ServiceException("邀请码生成失败，请重试");
        }
        
        YyUser updateObj = new YyUser();
        updateObj.setUserId(userId);
        updateObj.setInviteCode(newInviteCode);
        yyUserMapper.updateYyUser(updateObj);
        
        return newInviteCode;
    }

    /**
     * 如果会员已到期则降级为普通会员（原子SQL，无锁表风险）
     * 
     * 使用带条件的 UPDATE 语句，只有条件匹配时才执行写入：
     * 1. SQL 本身原子性，不需要 FOR UPDATE
     * 2. 条件不满足时不会写入，不产生无意义行锁
     * 3. 与定时任务的批量 UPDATE 并发执行完全幂等
     * 
     * @param userId 用户ID
     * @return 用户对象
     */
    @Override
    public YyUser downgradeIfExpired(Long userId) {
        // 原子降级：只有到期的非普通会员才会被更新
        yyUserMapper.downgradeIfExpired(userId);
        return yyUserMapper.selectYyUserByUserId(userId);
    }

    @Override
    public int bindReferrer(Long userId, String referrerCode) {
        YyUser user = yyUserMapper.selectYyUserByUserId(userId);
        if (user == null) {
            throw new ServiceException("当前用户不存在");
        }
        if (StringUtils.isNotEmpty(user.getReferrerCode())) {
            throw new ServiceException("您已经绑定过推荐人，无法再次绑定");
        }
        
        // 确保获取到用户自身的邀请码（兼容老数据未生成邀请码的情况）
        String myInviteCode = user.getInviteCode();
        if (StringUtils.isEmpty(myInviteCode)) {
            myInviteCode = getOrGenerateInviteCode(userId);
        }

        if (referrerCode.equalsIgnoreCase(myInviteCode)) {
            throw new ServiceException("不能绑定自己的推荐码");
        }

        // 校验推荐码是否存在
        YyUser referrer = yyUserMapper.selectYyUserByInviteCode(referrerCode);
        if (referrer == null) {
            throw new ServiceException("无效的推荐码");
        }

        YyUser updateObj = new YyUser();
        updateObj.setUserId(userId);
        updateObj.setReferrerCode(referrerCode);
        return yyUserMapper.updateYyUser(updateObj);
    }

    /**
     * 原子增加用户余额
     * 
     * @param userId 用户ID
     * @param amount 增加的金额
     * @param reason 备注原因
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int incrementUserBalance(Long userId, java.math.BigDecimal amount, String reason) {
        if (userId == null || amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            log.warn("增加余额参数非法: userId={}, amount={}", userId, amount);
            return 0;
        }
        int rows = yyUserMapper.incrementUserBalance(userId, amount);
        log.info("用户余额增加: userId={}, amount={}, reason={}, rows={}", userId, amount, reason, rows);
        return rows;
    }
}
