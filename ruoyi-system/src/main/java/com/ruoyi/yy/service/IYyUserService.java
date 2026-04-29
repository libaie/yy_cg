package com.ruoyi.yy.service;

import java.util.List;
import com.ruoyi.yy.domain.YyUser;

/**
 * 用户Service接口
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public interface IYyUserService 
{
    /**
     * 查询用户
     * 
     * @param userId 用户主键
     * @return 用户
     */
    public YyUser selectYyUserByUserId(Long userId);

    /**
     * 查询用户列表
     * 
     * @param yyUser 用户
     * @return 用户集合
     */
    public List<YyUser> selectYyUserList(YyUser yyUser);

    /**
     * 新增用户
     * 
     * @param yyUser 用户
     * @return 结果
     */
    public int insertYyUser(YyUser yyUser);

    /**
     * 修改用户
     * 
     * @param yyUser 用户
     * @return 结果
     */
    public int updateYyUser(YyUser yyUser);

    /**
     * 批量删除用户
     * 
     * @param userIds 需要删除的用户主键集合
     * @return 结果
     */
    public int deleteYyUserByUserIds(Long[] userIds);

    /**
     * 删除用户信息
     * 
     * @param userId 用户主键
     * @return 结果
     */
    public int deleteYyUserByUserId(Long userId);

    /**
     * 会员升级处理 (支付回调触发)
     * @param orderNo 订单号
     * @param transactionId 第三方支付流水号
     * @return 结果
     */
    public int upgradeUserMember(String orderNo, String transactionId);

    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户
     */
    public YyUser selectYyUserByPhone(String phone);

    /**
     * 登录校验 (手机号+密码 或 手机号+验证码)
     * @param phone 手机号
     * @param password 密码 (可选)
     * @param code 验证码 (可选)
     * @param loginType 登录类型: password / sms
     * @return Token
     */
    public String login(String phone, String password, String code, String loginType);

    /**
     * 用户注册
     * @param user 用户信息
     * @param code 验证码
     * @return 结果
     */
    public boolean register(YyUser user, String code);

    /**
     * 绑定推荐人
     * @param userId 当前用户ID
     * @param referrerCode 推荐人的邀请码
     * @return 结果
     */
    public int bindReferrer(Long userId, String referrerCode);

    /**
     * 获取或生成当前用户的邀请码
     * @param userId 当前用户ID
     * @return 邀请码
     */
    public String getOrGenerateInviteCode(Long userId);

    /**
     * 如果会员已到期则降级为普通会员（带排他锁防竞态）
     * @param userId 用户ID
     * @return 用户对象（降级后）
     */
    public YyUser downgradeIfExpired(Long userId);

    /**
     * 原子增加用户余额
     * @param userId 用户ID
     * @param amount 增加的金额
     * @param reason 备注原因
     * @return 影响行数
     */
    public int incrementUserBalance(Long userId, java.math.BigDecimal amount, String reason);
}
