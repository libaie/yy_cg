package com.ruoyi.yy.service.impl;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.yy.mapper.YyPlatformMapper;
import com.ruoyi.yy.mapper.YyPlatformApiMapper;
import com.ruoyi.yy.mapper.YyUserPlatformMapper;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.yy.domain.YyPlatformApi;
import com.ruoyi.yy.domain.YyUserPlatform;
import com.ruoyi.yy.service.IYyPlatformService;

/**
 * 平台信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-29
 */
@Service
public class YyPlatformServiceImpl implements IYyPlatformService 
{
    @Autowired
    private YyPlatformMapper yyPlatformMapper;

    @Autowired
    private YyPlatformApiMapper yyPlatformApiMapper;

    @Autowired
    private YyUserPlatformMapper yyUserPlatformMapper;

    @Override
    public YyPlatform selectYyPlatformByPId(Long pId)
    {
        return yyPlatformMapper.selectYyPlatformByPId(pId);
    }

    @Override
    public YyPlatform selectYyPlatformByCode(String platformCode)
    {
        return yyPlatformMapper.selectYyPlatformByCode(platformCode);
    }

    @Override
    public List<YyPlatform> selectYyPlatformList(YyPlatform yyPlatform)
    {
        return yyPlatformMapper.selectYyPlatformList(yyPlatform);
    }

    /**
     * 获取所有启用的平台及其启用的API配置（不带Token）
     */
    @Override
    public List<YyPlatform> selectActivePlatformsWithApis()
    {
        return selectActivePlatformsWithApis(null);
    }

    /**
     * 获取所有启用的平台及其启用的API配置，并注入当前用户的Token（如果已绑定）
     */
    @Override
    public List<YyPlatform> selectActivePlatformsWithApis(Long userId)
    {
        YyPlatform query = new YyPlatform();
        query.setIsActive(1);
        List<YyPlatform> platforms = yyPlatformMapper.selectYyPlatformList(query);

        // 如果传了userId，查询该用户所有平台绑定记录
        java.util.Map<Long, YyUserPlatform> bindMap = new java.util.HashMap<>();
        if (userId != null) {
            List<YyUserPlatform> binds = yyUserPlatformMapper.selectByUserId(userId);
            if (binds != null) {
                for (YyUserPlatform up : binds) {
                    bindMap.put(up.getPlatformId(), up);
                }
            }
        }

        for (YyPlatform p : platforms) {
            YyPlatformApi apiQuery = new YyPlatformApi();
            apiQuery.setPlatformId(p.getPId());
            apiQuery.setIsActive(1);
            List<YyPlatformApi> apis = yyPlatformApiMapper.selectYyPlatformApiList(apiQuery);
            p.setApis(apis);

            // 注入用户绑定信息（Token不脱敏，采集用）
            if (userId != null && bindMap.containsKey(p.getPId())) {
                YyUserPlatform up = bindMap.get(p.getPId());
                p.setBindStatus(up.getBindStatus());
                p.setLoginStatus(up.getLoginStatus());
                p.setTokenExpireTime(up.getTokenExpireTime());
                p.setPlatformUsername(up.getPlatformUsername());
                p.setPlatformNickname(up.getPlatformNickname());
                p.setBindTime(up.getBindTime());
                p.setLastLoginTime(up.getLastLoginTime());
                p.setLastSyncTime(up.getLastSyncTime());
                // 不在查询中隐式覆盖loginStatus，仅由syncToken和定时任务控制
            }
        }
        return platforms;
    }

    @Override
    public int insertYyPlatform(YyPlatform yyPlatform)
    {
        yyPlatform.setCreateTime(DateUtils.getNowDate());
        return yyPlatformMapper.insertYyPlatform(yyPlatform);
    }

    @Override
    public int updateYyPlatform(YyPlatform yyPlatform)
    {
        return yyPlatformMapper.updateYyPlatform(yyPlatform);
    }

    @Override
    public int deleteYyPlatformByPIds(Long[] pIds)
    {
        return yyPlatformMapper.deleteYyPlatformByPIds(pIds);
    }

    @Override
    public int deleteYyPlatformByPId(Long pId)
    {
        return yyPlatformMapper.deleteYyPlatformByPId(pId);
    }

    /**
     * 绑定平台（保存Token）
     */
    @Override
    public int bindPlatform(Long userId, YyUserPlatform bindInfo)
    {
        YyUserPlatform existing = yyUserPlatformMapper.selectByUserIdAndPlatformId(userId, bindInfo.getPlatformId());
        Date now = DateUtils.getNowDate();

        if (existing != null) {
            // 已存在记录，更新
            existing.setToken(bindInfo.getToken());
            existing.setTokenExpireTime(bindInfo.getTokenExpireTime());
            existing.setPlatformUsername(bindInfo.getPlatformUsername());
            existing.setPlatformNickname(bindInfo.getPlatformNickname());
            existing.setBindStatus(1);
            existing.setLoginStatus(1);
            existing.setBindTime(now);
            existing.setLastSyncTime(now);
            return yyUserPlatformMapper.updateYyUserPlatform(existing);
        } else {
            // 新增记录
            YyUserPlatform record = new YyUserPlatform();
            record.setUserId(userId);
            record.setPlatformId(bindInfo.getPlatformId());
            record.setToken(bindInfo.getToken());
            record.setTokenExpireTime(bindInfo.getTokenExpireTime());
            record.setPlatformUsername(bindInfo.getPlatformUsername());
            record.setPlatformNickname(bindInfo.getPlatformNickname());
            record.setBindStatus(1);
            record.setLoginStatus(1);
            record.setBindTime(now);
            record.setLastLoginTime(now);
            record.setLastSyncTime(now);
            record.setCreateTime(now);
            return yyUserPlatformMapper.insertYyUserPlatform(record);
        }
    }

    /**
     * 解绑平台（清除Token）
     */
    @Override
    public int unbindPlatform(Long userId, Long platformId)
    {
        YyUserPlatform existing = yyUserPlatformMapper.selectByUserIdAndPlatformId(userId, platformId);
        if (existing == null) {
            return 0;
        }
        existing.setToken(null);
        existing.setTokenExpireTime(null);
        existing.setBindStatus(0);
        existing.setLoginStatus(0);
        return yyUserPlatformMapper.updateYyUserPlatform(existing);
    }

    /**
     * 同步Token到后端（loginType: 1=登录, 0=登出）
     */
    @Override
    public int syncToken(Long userId, Long platformId, String token, String tokenExpireTime, String lastLoginTime, Integer loginType)
    {
        YyUserPlatform existing = yyUserPlatformMapper.selectByUserIdAndPlatformId(userId, platformId);
        Date now = DateUtils.getNowDate();
        Date loginTime = null;
        if (lastLoginTime != null && lastLoginTime.length() > 0) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                loginTime = java.util.Date.from(java.time.LocalDateTime.parse(lastLoginTime, formatter).atZone(java.time.ZoneId.systemDefault()).toInstant());
            } catch (Exception e) {
                loginTime = now;
            }
        } else {
            loginTime = now;
        }

        if (loginType != null && loginType == 0) {
            // === 登出 ===
            if (existing != null) {
                existing.setToken(null);
                existing.setTokenExpireTime(null);
                existing.setLoginStatus(0);
                existing.setLastSyncTime(now);
                return yyUserPlatformMapper.updateYyUserPlatform(existing);
            }
            return 0;
        }

        // === 登录 ===
        Date expireTime = null;
        if (tokenExpireTime != null && tokenExpireTime.length() > 0) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                expireTime = java.util.Date.from(java.time.LocalDateTime.parse(tokenExpireTime, formatter).atZone(java.time.ZoneId.systemDefault()).toInstant());
            } catch (Exception e) {
                // 忽略解析错误
            }
        }

        if (existing != null) {
            existing.setToken(token);
            existing.setTokenExpireTime(expireTime);
            existing.setLoginStatus(1);
            existing.setBindStatus(1);
            existing.setLastLoginTime(loginTime);
            existing.setLastSyncTime(now);
            if (existing.getBindTime() == null) {
                existing.setBindTime(now);
            }
            return yyUserPlatformMapper.updateYyUserPlatform(existing);
        } else {
            YyUserPlatform record = new YyUserPlatform();
            record.setUserId(userId);
            record.setPlatformId(platformId);
            record.setToken(token);
            record.setTokenExpireTime(expireTime);
            record.setBindStatus(1);
            record.setLoginStatus(1);
            record.setBindTime(now);
            record.setLastLoginTime(loginTime);
            record.setLastSyncTime(now);
            record.setCreateTime(now);
            return yyUserPlatformMapper.insertYyUserPlatform(record);
        }
    }

    /**
     * 获取当前用户的平台绑定列表
     */
    @Override
    public List<YyUserPlatform> selectMyPlatformList(Long userId)
    {
        List<YyUserPlatform> list = yyUserPlatformMapper.selectByUserId(userId);
        // Token脱敏
        for (YyUserPlatform up : list) {
            if (up.getToken() != null && up.getToken().length() > 8) {
                up.setToken(up.getToken().substring(0, 4) + "***已脱敏***");
            }
        }
        return list;
    }

    /**
     * 获取指定平台的Token（采集用，不脱敏）
     */
    @Override
    public YyUserPlatform getPlatformToken(Long userId, Long platformId)
    {
        return yyUserPlatformMapper.selectByUserIdAndPlatformId(userId, platformId);
    }

    /**
     * 根据用户ID查询用户绑定的平台
     */
    @Override
    public List<YyPlatform> selectPlatformsByUserId(Long userId)
    {
        return yyPlatformMapper.selectPlatformsByUserId(userId);
    }

    /**
     * 根据用户ID和平台条件查询平台列表
     */
    @Override
    public List<YyPlatform> selectYyPlatformListByUserId(Long userId, YyPlatform platform)
    {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("userId", userId);
        params.put("platform", platform);
        return yyPlatformMapper.selectYyPlatformListByUserId(params);
    }
}
