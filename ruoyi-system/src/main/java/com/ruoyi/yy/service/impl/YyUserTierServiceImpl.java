package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyMemberSubscription;
import com.ruoyi.yy.domain.YyMemberTier;
import com.ruoyi.yy.mapper.YyMemberSubscriptionMapper;
import com.ruoyi.yy.mapper.YyMemberTierMapper;
import com.ruoyi.yy.service.IYyUserTierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YyUserTierServiceImpl implements IYyUserTierService {

    private static final Logger log = LoggerFactory.getLogger(YyUserTierServiceImpl.class);

    @Autowired
    private YyMemberSubscriptionMapper subscriptionMapper;

    @Autowired
    private YyMemberTierMapper tierMapper;

    @Override
    public int getUserTierLevel(Long userId) {
        try {
            YyMemberSubscription query = new YyMemberSubscription();
            query.setUserId(userId);
            query.setPayStatus(1);
            List<YyMemberSubscription> subs = subscriptionMapper.selectYyMemberSubscriptionList(query);
            if (subs == null || subs.isEmpty()) return 0;
            YyMemberSubscription sub = subs.get(0);
            YyMemberTier tier = tierMapper.selectYyMemberTierByTierId(sub.getTierId());
            return tier != null ? tier.getMemberLevel() : 0;
        } catch (Exception e) {
            log.warn("Failed to get user tier level for userId={}", userId, e);
            return 0;
        }
    }
}
