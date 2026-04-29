package com.ruoyi.task;

import java.util.List;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.ruoyi.yy.mapper.YyMemberSubscriptionMapper;
import com.ruoyi.yy.mapper.YyUserPlatformMapper;
import com.ruoyi.yy.mapper.YyPlatformMapper;
import com.ruoyi.yy.domain.YyUserPlatform;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 医药系统定时任务
 * 
 * @author ruoyi
 */
@Component("yyTask")
public class YyTask {

    @Autowired
    private YyMemberSubscriptionMapper subscriptionMapper;

    @Autowired
    private YyUserPlatformMapper yyUserPlatformMapper;

    @Autowired
    private YyPlatformMapper yyPlatformMapper;

    /**
     * 会员订单超时关闭任务
     * 
     * @param timeoutMinutes 超时分钟数，例如 30
     */
    public void cancelTimeoutOrders(String timeoutMinutes) {
        int minutes = 30; // 默认30分钟
        if (StringUtils.isNotEmpty(timeoutMinutes)) {
            try {
                minutes = Integer.parseInt(timeoutMinutes);
            } catch (NumberFormatException e) {
                // 如果格式不对，回退到默认
            }
        }
        int rows = subscriptionMapper.cancelTimeoutOrders(minutes);
        System.out.println("定时任务执行成功：本次共清理并取消了 [" + rows + "] 条超时的待支付会员订单。");
    }

    /**
     * 校验已绑定平台Token的有效性
     * 通过实际访问平台页面判断Token是否过期
     * 过期后清空Token并将loginStatus设为0
     */
    public void checkPlatformToken() {
        List<YyUserPlatform> list = yyUserPlatformMapper.selectBoundWithToken();
        if (list == null || list.isEmpty()) {
            System.out.println("PlatformToken校验：无已绑定记录，跳过。");
            return;
        }
        int expiredCount = 0;
        int skipCount = 0;
        RestTemplate rest = new RestTemplate();
        for (YyUserPlatform up : list) {
            YyPlatform platform = yyPlatformMapper.selectYyPlatformByPId(up.getPlatformId());
            if (platform == null) {
                continue;
            }
            String checkUrl = platform.getPlatformHomeUrl();
            if (checkUrl == null || checkUrl.isEmpty()) {
                checkUrl = platform.getPlatformLoginUrl();
            }
            if (checkUrl == null || checkUrl.isEmpty()) {
                continue;
            }
            boolean isValid = probeTokenValidity(rest, up, platform, checkUrl);
            if (!isValid) {
                up.setToken(null);
                up.setTokenExpireTime(null);
                up.setLoginStatus(0);
                yyUserPlatformMapper.updateYyUserPlatform(up);
                expiredCount++;
                System.out.println("Token已过期 - userId: " + up.getUserId() + 
                    ", platform: " + platform.getPlatformName());
            } else {
                skipCount++;
            }
        }
        System.out.println("PlatformToken校验完成：有效 " + skipCount + " 条，过期清理 " + expiredCount + " 条。");
    }

    /**
     * 探测Token有效性：带上Token请求平台页面
     * 返回 true=有效，false=过期
     */
    private boolean probeTokenValidity(RestTemplate rest, YyUserPlatform up, YyPlatform platform, String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            String storageType = platform.getTokenStorageType();
            if ("cookie".equalsIgnoreCase(storageType)) {
                String cookieName = platform.getTokenKey() != null ? platform.getTokenKey() : "Token";
                headers.set("Cookie", cookieName + "=" + up.getToken());
            } else {
                String headerName = platform.getTokenKey() != null ? platform.getTokenKey() : "Authorization";
                headers.set(headerName, up.getToken());
            }

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, request, String.class);
            int statusCode = response.getStatusCode().value();
            if (statusCode == 401 || statusCode == 403) {
                return false;
            }
            return true;
        } catch (RestClientException e) {
            // 网络异常不视为Token过期
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
