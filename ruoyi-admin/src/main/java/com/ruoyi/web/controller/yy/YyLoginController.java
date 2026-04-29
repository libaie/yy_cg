package com.ruoyi.web.controller.yy;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.YyLoginService;
import com.ruoyi.yy.domain.YyUser;
import com.ruoyi.common.annotation.Anonymous;

/**
 * 医药系统登录注册Controller
 * 
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/yy")
public class YyLoginController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(YyLoginController.class);

    @Autowired
    private YyLoginService yyLoginService;

    /**
     * 发送短信验证码
     */
    @Anonymous
    @PostMapping("/captcha")
    public AjaxResult getSmsCode(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        if (StringUtils.isEmpty(phone)) {
            return AjaxResult.error("手机号不能为空");
        }
        String code = yyLoginService.sendSmsCode(phone);
        log.info("📱 模拟短信验证码 - 手机号: {}, 验证码: {}", phone, code);
        return AjaxResult.success("验证码已发送");
    }

    /**
     * 注册接口
     */
    @Anonymous
    @PostMapping("/register")
    public AjaxResult register(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        String password = params.get("password");
        String code = params.get("code");
        String nickName = params.get("nickName");
        String referrerCode = params.get("referrerCode");

        YyUser user = new YyUser();
        user.setPhone(phone);
        user.setPassword(password);
        user.setNickName(nickName);
        user.setReferrerCode(referrerCode);

        yyLoginService.register(user, code);
        return AjaxResult.success("注册成功");
    }

    /**
     * 登录接口 (支持密码登录和验证码登录)
     */
    @Anonymous
    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        String password = params.get("password");
        String code = params.get("code");
        String loginType = params.get("loginType"); // "password" or "sms"

        if (StringUtils.isEmpty(loginType)) {
            loginType = "password";
        }

        AjaxResult ajax = AjaxResult.success();
        String token = yyLoginService.login(phone, password, code, loginType);
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }
}
