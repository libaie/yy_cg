package com.ruoyi.web.controller.yy;

import java.math.BigDecimal;
import java.util.List;
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
import com.ruoyi.yy.domain.YyMemberSubscription;
import com.ruoyi.yy.domain.YyMemberTier;
import com.ruoyi.yy.service.IYyMemberSubscriptionService;
import com.ruoyi.yy.service.IYyMemberTierService;
import com.ruoyi.yy.service.IYyReferralRewardService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * 会员订阅订单Controller
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
@RestController
@RequestMapping("/yy/subscription")
public class YyMemberSubscriptionController extends BaseController
{
    @Autowired
    private IYyMemberSubscriptionService yyMemberSubscriptionService;

    @Autowired
    private IYyMemberTierService yyMemberTierService;

    @Autowired
    private IYyReferralRewardService yyReferralRewardService;

    /**
     * 创建预付款订单（医药助手前端用）
     * 检查是否存在同类型支付中订单，有则提示，无则创建
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping("/preorder")
    public AjaxResult createPreOrder(@RequestBody Map<String, Long> params)
    {
        Long tierId = params.get("tierId");
        if (tierId == null) {
            return error("套餐ID不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        try {
            YyMemberSubscription order = yyMemberSubscriptionService.createPreOrder(userId, tierId);
            Map<String, Object> data = new HashMap<>();
            data.put("orderNo", order.getOrderNo());
            data.put("payExpireTime", order.getPayExpireTime());
            return success(data);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /**
     * 查询当前登录用户的订阅订单列表（医药助手前端用）
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @GetMapping("/myList")
    public TableDataInfo myList(YyMemberSubscription yyMemberSubscription)
    {
        startPage();
        yyMemberSubscription.setUserId(SecurityUtils.getUserId());
        List<YyMemberSubscription> list = yyMemberSubscriptionService.selectYyMemberSubscriptionList(yyMemberSubscription);
        return getDataTable(list);
    }

    /**
     * 查询会员订阅订单列表（支持管理员 yy:subscription:list 和普通用户 yy:client:access）
     * 普通用户自动限定只查询自己的订单
     */
    @PreAuthorize("@ss.hasAnyPermi('yy:client:access,yy:subscription:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyMemberSubscription yyMemberSubscription)
    {
        startPage();
        // 如果用户只有 yy:client:access 权限（无 yy:subscription:list），强制限定只查自己
        if (!SecurityUtils.hasPermi("yy:subscription:list") && SecurityUtils.hasPermi("yy:client:access")) {
            yyMemberSubscription.setUserId(SecurityUtils.getUserId());
        }
        List<YyMemberSubscription> list = yyMemberSubscriptionService.selectYyMemberSubscriptionList(yyMemberSubscription);
        return getDataTable(list);
    }

    /**
     * 导出会员订阅订单列表
     */
    @PreAuthorize("@ss.hasPermi('yy:subscription:export')")
    @Log(title = "会员订阅订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, YyMemberSubscription yyMemberSubscription)
    {
        List<YyMemberSubscription> list = yyMemberSubscriptionService.selectYyMemberSubscriptionList(yyMemberSubscription);
        ExcelUtil<YyMemberSubscription> util = new ExcelUtil<YyMemberSubscription>(YyMemberSubscription.class);
        util.exportExcel(response, list, "会员订阅订单数据");
    }

    /**
     * 获取会员订阅订单详细信息（支持管理员 yy:subscription:query 和普通用户 yy:client:access）
     * 普通用户只能查看自己的订单
     */
    @PreAuthorize("@ss.hasAnyPermi('yy:client:access,yy:subscription:query')")
    @GetMapping(value = "/{subId}")
    public AjaxResult getInfo(@PathVariable("subId") Long subId)
    {
        YyMemberSubscription subscription = yyMemberSubscriptionService.selectYyMemberSubscriptionBySubId(subId);
        if (subscription == null) {
            return error("订单不存在");
        }
        // 普通用户只能查看自己的订单
        if (!SecurityUtils.hasPermi("yy:subscription:query") && SecurityUtils.hasPermi("yy:client:access")) {
            if (!subscription.getUserId().equals(SecurityUtils.getUserId())) {
                return error("无权查看该订单");
            }
        }
        return success(subscription);
    }

    /**
     * 修改会员订阅订单（仅允许修改业务可调字段）
     * 仅管理员可操作，仅允许修改：会员开始时间、会员到期时间、支付状态、支付超时时间
     * 
     * 当管理员将支付状态改为"已支付"时，自动处理推荐奖励。
     */
    @PreAuthorize("@ss.hasPermi('yy:subscription:edit')")
    @Log(title = "会员订阅订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YyMemberSubscription yyMemberSubscription)
    {
        if (yyMemberSubscription.getSubId() == null) {
            return error("订阅记录ID不能为空");
        }

        // 检查是否将状态改为"已支付"(payStatus=1)，如果是则先查询订单信息
        boolean becamePaid = false;
        Long userId = null;
        Long tierId = null;
        BigDecimal price = null;

        if (yyMemberSubscription.getPayStatus() != null && yyMemberSubscription.getPayStatus() == 1) {
            YyMemberSubscription existing = yyMemberSubscriptionService.selectYyMemberSubscriptionBySubId(yyMemberSubscription.getSubId());
            if (existing != null) {
                // 只有从"未支付"或"支付中"变为"已支付"时才触发推荐奖励
                if (existing.getPayStatus() == null || existing.getPayStatus() == 0 || existing.getPayStatus() == 3) {
                    becamePaid = true;
                    userId = existing.getUserId();
                    tierId = existing.getTierId();

                    // 查套餐价格
                    try {
                        YyMemberTier tier = yyMemberTierService.selectYyMemberTierByTierId(tierId);
                        if (tier != null && tier.getPrice() != null) {
                            price = tier.getPrice();
                        }
                    } catch (Exception e) {
                        // 不影响主流程
                    }
                }
            }
        }

        // 只允许更新业务可调字段，其他字段置空防止篡改
        YyMemberSubscription updateData = new YyMemberSubscription();
        updateData.setSubId(yyMemberSubscription.getSubId());
        updateData.setStartTime(yyMemberSubscription.getStartTime());
        updateData.setEndTime(yyMemberSubscription.getEndTime());
        updateData.setPayStatus(yyMemberSubscription.getPayStatus());
        updateData.setPayExpireTime(yyMemberSubscription.getPayExpireTime());
        updateData.setPayTime(yyMemberSubscription.getPayTime());
        
        int result = yyMemberSubscriptionService.updateYyMemberSubscription(updateData);

        // 订单更新成功后，处理推荐奖励（异步，不阻塞主流程）
        if (becamePaid && userId != null) {
            final Long finalUserId = userId;
            final Long finalTierId = tierId;
            final BigDecimal finalPrice = price;
            final Long finalSubId = yyMemberSubscription.getSubId();
            new Thread(() -> {
                try {
                    yyReferralRewardService.processReferralReward(
                        finalUserId, finalTierId,
                        finalPrice != null ? finalPrice : BigDecimal.ZERO,
                        finalSubId);
                } catch (Exception e) {
                    // 推荐奖励失败不影响订单本身
                    logger.error("推荐奖励处理异常, subId={}", finalSubId, e);
                }
            }).start();
        }

        return toAjax(result);
    }

    /**
     * 删除会员订阅订单
     */
    @PreAuthorize("@ss.hasPermi('yy:subscription:remove')")
    @Log(title = "会员订阅订单", businessType = BusinessType.DELETE)
	@DeleteMapping("/{subIds}")
    public AjaxResult remove(@PathVariable Long[] subIds)
    {
        return toAjax(yyMemberSubscriptionService.deleteYyMemberSubscriptionBySubIds(subIds));
    }
}
