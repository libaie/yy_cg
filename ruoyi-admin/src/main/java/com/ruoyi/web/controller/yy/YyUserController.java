package com.ruoyi.web.controller.yy;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.yy.domain.YyUser;
import com.ruoyi.yy.service.IYyUserService;

import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysDictDataService;
import com.ruoyi.common.annotation.Anonymous;
import java.util.HashMap;
import java.util.Date;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户Controller
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
@RestController
@RequestMapping("/yy/user")
public class YyUserController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(YyUserController.class);

    @Autowired
    private IYyUserService yyUserService;

    @Autowired
    private ISysDictDataService dictDataService;

    /**
     * 查询用户列表
     */
    @PreAuthorize("@ss.hasPermi('yy:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyUser yyUser)
    {
        startPage();
        List<YyUser> list = yyUserService.selectYyUserList(yyUser);
        return getDataTable(list);
    }

    /**
     * 导出用户列表
     */
    @PreAuthorize("@ss.hasPermi('yy:user:export')")
    @Log(title = "用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, YyUser yyUser)
    {
        List<YyUser> list = yyUserService.selectYyUserList(yyUser);
        ExcelUtil<YyUser> util = new ExcelUtil<YyUser>(YyUser.class);
        util.exportExcel(response, list, "用户数据");
    }

    /**
     * 获取用户详细信息
     */
    @PreAuthorize("@ss.hasPermi('yy:user:query')")
    @GetMapping(value = "/{userId}")
    public AjaxResult getInfo(@PathVariable("userId") Long userId)
    {
        return success(yyUserService.selectYyUserByUserId(userId));
    }

    /**
     * 新增用户
     */
    @PreAuthorize("@ss.hasPermi('yy:user:add')")
    @Log(title = "用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YyUser yyUser)
    {
        return toAjax(yyUserService.insertYyUser(yyUser));
    }

    /**
     * 修改用户
     */
    @PreAuthorize("@ss.hasPermi('yy:user:edit')")
    @Log(title = "用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyUser yyUser)
    {
        return toAjax(yyUserService.updateYyUser(yyUser));
    }

    /**
     * 删除用户
     */
    @PreAuthorize("@ss.hasPermi('yy:user:remove')")
    @Log(title = "用户", businessType = BusinessType.DELETE)
	@DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        return toAjax(yyUserService.deleteYyUserByUserIds(userIds));
    }

    /**
     * 获取个人用户信息（包含会员信息）
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @GetMapping("/profile")
    public AjaxResult getProfile() {
        Long userId = SecurityUtils.getUserId();

        // 实时检查：会员到期则立即降级为普通会员（原子SQL，无锁表）
        YyUser checkedUser = yyUserService.downgradeIfExpired(userId);
        if (checkedUser == null) {
            return AjaxResult.error("用户不存在");
        }

        Long level = checkedUser.getMemberLevel() != null ? checkedUser.getMemberLevel() : 0L;

        // 使用框架内置方法获取字典标签
        String levelName = dictDataService.selectDictLabel("yy_member_tier_name", level.toString());
        if (StringUtils.isEmpty(levelName)) {
            levelName = dictDataService.selectDictLabel("yy_member_tier_name", "0");
        }

        AjaxResult ajax = AjaxResult.success();
        // 屏蔽敏感字段
        checkedUser.setPassword(null);
        ajax.put("user", checkedUser);
        ajax.put("memberLevelName", levelName);
        return ajax;
    }

    /**
     * 绑定推荐人
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @Log(title = "绑定推荐人", businessType = BusinessType.UPDATE)
    @PostMapping("/bindReferrer")
    public AjaxResult bindReferrer(@RequestBody Map<String, String> params) {
        String referrerCode = params.get("referrerCode");
        if (StringUtils.isEmpty(referrerCode)) {
            return AjaxResult.error("推荐码不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        return toAjax(yyUserService.bindReferrer(userId, referrerCode));
    }

    /**
     * 获取或生成我自己的推荐码
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @GetMapping("/myInviteCode")
    public AjaxResult getMyInviteCode() {
        Long userId = SecurityUtils.getUserId();
        String code = yyUserService.getOrGenerateInviteCode(userId);
        return AjaxResult.success("获取成功", code);
    }

    /**
     * 接收支付宝第三方支付平台的异步通知 (正式环境使用)
     * 这里以支付宝为例，展示验签逻辑
     */
    @Anonymous
    @Log(title = "支付宝回调", businessType = BusinessType.UPDATE)
    @PostMapping("/notify/alipay")
    public String handleAlipayNotify(HttpServletRequest request) {
        // 1. 获取支付平台回传的所有参数
        Map<String, String> params = convertRequestParamsToMap(request);
        
        // 2. 验签逻辑 (由于目前未引入 Alipay SDK，此处代码先注释掉)
        /*
        boolean signVerified = false;
        try {
            // ALIPAY_PUBLIC_KEY 需从配置或数据库获取
            // signVerified = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, "UTF-8", "RSA2");
        } catch (Exception e) {
            return "error";
        }
        
        if (!signVerified) {
            return "fail"; // 验签失败
        }
        */

        // 3. 验证业务数据一致性
        String orderNo = params.get("out_trade_no"); // 对应我们生成的 YY... 订单号
        String transactionId = params.get("trade_no"); // 对应支付宝生成的流水号
        String tradeStatus = params.get("trade_status"); // 对应交易状态

        // 只有交易成功才执行业务升级
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            int result = yyUserService.upgradeUserMember(orderNo, transactionId);
            return result > 0 ? "success" : "fail";
        }

        return "success"; // 即使状态不符也返回success，防止支付宝重复推送
    }

    /**
     * 接收微信支付第三方支付平台的异步通知 (正式环境使用)
     */
    @Anonymous
    @Log(title = "微信回调", businessType = BusinessType.UPDATE)
    @PostMapping("/notify/wechat")
    public Map<String, String> handleWechatNotify(HttpServletRequest request) {
        Map<String, String> resultMap = new HashMap<>();
        
        // 1. 验签与解密 (由于未引入 WeChat SDK，此处仅展示逻辑框架)
        /*
        try {
            // 微信 V3 采用 JSON 数据流，且通过 Header 传输签名
            // String serial = request.getHeader("WechatPay2-Serial");
            // String signature = request.getHeader("WechatPay2-Signature");
            // ... 使用官方 SDK 的 NotificationHandler 进行解密 ...
            
            // String orderNo = decryptData.get("out_trade_no");
            // String transactionId = decryptData.get("transaction_id");
            
            // yyUserService.upgradeUserMember(orderNo, transactionId);
            
            resultMap.put("code", "SUCCESS");
            resultMap.put("message", "成功");
        } catch (Exception e) {
            resultMap.put("code", "FAIL");
            resultMap.put("message", e.getMessage());
        }
        */

        resultMap.put("code", "SUCCESS");
        resultMap.put("message", "模拟回调成功");
        return resultMap;
    }

    /**
     * 将 HttpServletRequest 中的参数转换为 Map
     */
    private Map<String, String> convertRequestParamsToMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        return params;
    }

    /**
     * 管理员手动触发会员升级（仅限内部调试/管理使用）
     */
    @PreAuthorize("@ss.hasPermi('yy:admin:upgrade')")
    @Log(title = "管理员手动升级", businessType = BusinessType.UPDATE)
    @PostMapping("/upgrade")
    public AjaxResult upgrade(@RequestBody Map<String, Object> params) {
        if (params == null || params.get("orderNo") == null) {
            return AjaxResult.error("升级失败：请求参数缺失 (orderNo)");
        }
        
        try {
            String orderNo = params.get("orderNo").toString();
            String transactionId = params.get("transactionId") != null ? params.get("transactionId").toString() : "";
            
            int result = yyUserService.upgradeUserMember(orderNo, transactionId);
            if (result > 0) {
                return AjaxResult.success("升级成功：会员等级已更新");
            } else {
                return AjaxResult.error("升级失败：可能订单已失效、用户不存在或不支持降级购买");
            }
        } catch (Exception e) {
            return AjaxResult.error("升级处理异常：" + e.getMessage());
        }
    }
}
