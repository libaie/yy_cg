package com.ruoyi.framework.web.service;

import java.util.HashSet;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.yy.domain.YyUser;
import com.ruoyi.yy.mapper.YyUserMapper;
import com.ruoyi.yy.service.IYyUserService;

/**
 * 医药系统登录校验方法
 * 
 * @author ruoyi
 */
@Component
public class YyLoginService {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private IYyUserService yyUserService;

    @Autowired
    private YyUserMapper yyUserMapper;

    /**
     * 登录验证
     * 
     * @param phone 手机号
     * @param password 密码
     * @param code 验证码
     * @param loginType 登录类型 (password / sms)
     * @return 结果
     */
    public String login(String phone, String password, String code, String loginType) {
        // 登录时需要查询含密码的记录
        YyUser user = yyUserMapper.selectYyUserByPhoneWithPwd(phone);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        if ("sms".equals(loginType)) {
            validateSmsCode(phone, code);
        } else {
            if (!SecurityUtils.matchesPassword(password, user.getPassword())) {
                throw new ServiceException("用户密码不正确");
            }
        }

        // 构建 LoginUser 对象
        return tokenService.createToken(createLoginUser(user));
    }

    /**
     * 注册
     */
    public void register(YyUser user, String code) {
        String phone = user.getPhone();
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(user.getPassword())) {
            throw new ServiceException("手机号和密码不能为空");
        }
        
        validateSmsCode(phone, code);

        if (yyUserService.selectYyUserByPhone(phone) != null) {
            throw new ServiceException("手机号已注册");
        }

        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        if (!yyUserService.register(user, code)) {
            throw new ServiceException("注册失败，请联系管理员");
        }
    }

    /**
     * 模拟发送短信验证码
     */
    public String sendSmsCode(String phone) {
        // 生成6位随机码
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        // 存入缓存 5分钟有效
        redisCache.setCacheObject(getSmsCacheKey(phone), code, 5, TimeUnit.MINUTES);
        // 实际开发中此处调用短信接口
        System.out.println("【医药系统】短信验证码已发送至 " + phone + "，验证码为：" + code);
        return code;
    }

    /**
     * 校验短信验证码
     */
    private void validateSmsCode(String phone, String code) {
        String verifyKey = getSmsCacheKey(phone);
        String captcha = redisCache.getCacheObject(verifyKey);
        if (captcha == null) {
            throw new ServiceException("验证码已过期");
        }
        if (!code.equals(captcha)) {
            throw new ServiceException("验证码错误");
        }
        redisCache.deleteObject(verifyKey);
    }

    private String getSmsCacheKey(String phone) {
        return "yy_sms_code:" + phone;
    }

    /**
     * 将 YyUser 适配为框架标准的 LoginUser
     */
    private LoginUser createLoginUser(YyUser yyUser) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(yyUser.getUserId());
        sysUser.setUserName(yyUser.getPhone()); // 以手机号作为用户名
        sysUser.setNickName(yyUser.getNickName());
        sysUser.setPhonenumber(yyUser.getPhone());
        sysUser.setPassword(yyUser.getPassword());
        sysUser.setStatus("0"); // 正常
        
        // 医药系统普通用户专属权限，用于与后台管理员隔离
        Set<String> permissions = new HashSet<>();
        permissions.add("yy:client:access"); 
        
        LoginUser loginUser = new LoginUser(sysUser, permissions);
        loginUser.setUserId(yyUser.getUserId());
        return loginUser;
    }
}
