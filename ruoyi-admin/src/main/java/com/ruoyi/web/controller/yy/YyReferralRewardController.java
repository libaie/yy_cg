package com.ruoyi.web.controller.yy;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.yy.domain.YyReferralReward;
import com.ruoyi.yy.service.IYyReferralRewardService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 推荐奖励记录Controller
 */
@RestController
@RequestMapping("/yy/referral/reward")
public class YyReferralRewardController extends BaseController
{
    @Autowired
    private IYyReferralRewardService yyReferralRewardService;

    @PreAuthorize("@ss.hasPermi('yy:reward:list')")
    @GetMapping("/list")
    public TableDataInfo list(YyReferralReward reward) {
        startPage();
        List<YyReferralReward> list = yyReferralRewardService.selectYyReferralRewardList(reward);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('yy:reward:query')")
    @GetMapping(value = "/{id}")
    public YyReferralReward getInfo(@PathVariable("id") Long id) {
        return yyReferralRewardService.selectYyReferralRewardById(id);
    }
}
