package com.ruoyi.yy.mapper;

import java.util.List;
import com.ruoyi.yy.domain.YyUser;

/**
 * 用户Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
public interface YyUserMapper 
{
    /**
     * 查询用户
     * 
     * @param userId 用户主键
     * @return 用户
     */
    public YyUser selectYyUserByUserId(Long userId);

    /**
     * 根据手机号查询用户
     * 
     * @param phone 手机号
     * @return 用户
     */
    public YyUser selectYyUserByPhone(String phone);

    /**
     * 根据手机号查询用户（含密码字段，仅用于登录校验）
     * 
     * @param phone 手机号
     * @return 用户
     */
    public YyUser selectYyUserByPhoneWithPwd(String phone);

    /**
     * 根据邀请码查询用户
     * 
     * @param inviteCode 邀请码
     * @return 用户
     */
    public YyUser selectYyUserByInviteCode(String inviteCode);

    /**
     * 排他锁查询用户（防并发覆盖过期时间）
     * 
     * @param userId 用户主键
     * @return 用户
     */
    public YyUser selectYyUserForUpdate(Long userId);

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
     * 原子降级：仅到期的非普通会员才会被更新
     */
    public int downgradeIfExpired(Long userId);

    /**
     * 批量降级过期会员
     */
    public int batchDowngradeExpiredMembers();

    /**
     * 删除用户
     * 
     * @param userId 用户主键
     * @return 结果
     */
    public int deleteYyUserByUserId(Long userId);

    /**
     * 批量删除用户
     * 
     * @param userIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteYyUserByUserIds(Long[] userIds);

    /**
     * 原子增加用户余额
     * 
     * @param userId 用户ID
     * @param amount 增加的金额
     * @return 影响行数
     */
    public int incrementUserBalance(Long userId, java.math.BigDecimal amount);
}
