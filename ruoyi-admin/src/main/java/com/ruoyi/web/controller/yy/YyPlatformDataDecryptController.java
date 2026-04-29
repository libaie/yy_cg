package com.ruoyi.web.controller.yy;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.yy.domain.YyPlatformApi;
import com.ruoyi.yy.domain.YyPlatformKeyVault;
import com.ruoyi.yy.dto.YyDataDecryPtDTO;
import com.ruoyi.yy.dto.YyDataIngestDTO;
import com.ruoyi.yy.service.IDataFusionService;
import com.ruoyi.yy.service.IYyPlatformApiService;
import com.ruoyi.yy.service.IYyPlatformService;
import com.ruoyi.yy.service.IYyPlatformKeyVaultService;
import com.ruoyi.common.utils.decrypt.DataDecryptUtil;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.core.domain.entity.SysDictData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/yy/platform/data")
public class YyPlatformDataDecryptController {

    private static final Logger log = LoggerFactory.getLogger(YyPlatformDataDecryptController.class);

    @Autowired
    private IYyPlatformService yyPlatformService;

    @Autowired
    private IYyPlatformKeyVaultService yyPlatformKeyVaultService;

    @Autowired
    private IDataFusionService dataFusionService;

    @Autowired
    private IYyPlatformApiService yyPlatformApiService;

    /** 主要做各平台数据解密 */

    /**
     * 数据解密接口
     * 解密后自动应用字段映射，返回原始数据 + 映射后的标准字段
     */
    @PreAuthorize("@ss.hasPermi('yy:client:access')")
    @PostMapping("/decrypt")
    public AjaxResult decryptData(@RequestBody YyDataDecryPtDTO requestBody) {
        String platformCode = requestBody.getPlatformCode();

        // 数据大小校验（5MB）
        int dataLen = 0;
        if (requestBody.getRawData() != null) dataLen = requestBody.getRawData().length();
        if (requestBody.getEncryptData() != null) dataLen = Math.max(dataLen, requestBody.getEncryptData().length());
        if (dataLen > 5 * 1024 * 1024) {
            return AjaxResult.error("数据大小超出 5MB 限制，当前 " + (dataLen / 1024 / 1024) + "MB");
        }

        // 根据 platformCode 获取平台
        YyPlatform platform = yyPlatformService.selectYyPlatformByCode(platformCode);
        if (platform == null) {
            return AjaxResult.error("平台不存在");
        }

        // ====== 解析 entryPath（优先级：前端传入 > apiCode 查库 > null） ======
        String entryPath = requestBody.getEntryPath();
        if ((entryPath == null || entryPath.isEmpty()) && requestBody.getApiCode() != null) {
            YyPlatformApi filter = new YyPlatformApi();
            filter.setPlatformId(platform.getPId());
            filter.setApiCode(requestBody.getApiCode());
            List<YyPlatformApi> apis = yyPlatformApiService.selectYyPlatformApiList(filter);
            if (!apis.isEmpty() && apis.get(0).getEntryPath() != null) {
                entryPath = apis.get(0).getEntryPath();
            }
        }



        Integer dataEncryptType = requestBody.getDataEncryptType();

        String responseData = requestBody.getResponseData();

        // 如果 dataEncryptType == 0 或 null，表示原始数据模式
        if (dataEncryptType == null || dataEncryptType == 0) {
            if (responseData != null && !responseData.isEmpty()) {
                requestBody.setRawData(responseData);
            }
            
        }

        // ====== 原始数据模式：跳过解密，直接映射 ======
        if (requestBody.isRawMode()) {
            String rawData = requestBody.getRawData();
            Map<String, Object> mapResult = dataFusionService.mapOnly(platformCode, rawData, entryPath);

            Object rawJson;
            try {
                rawJson = JSON.parse(rawData);
            } catch (Exception e) {
                rawJson = rawData;
            }

            // 根据 dataSource 决定是否返回原始数据
            if (requestBody.getDataSource() != null && requestBody.getDataSource() == 1) {
                mapResult.put("raw", rawJson);
            }
            
            return AjaxResult.success((String) mapResult.get("message"), mapResult);
        }

        // ====== 加密数据模式：解密 + 映射 ======
        String encryptData = requestBody.getEncryptData();
        

        // 如果有 responseData，从中提取加密数据
        if (responseData != null && !responseData.isEmpty()) {
            // 确定加密数据路径
            String encryptDataPath = requestBody.getEncryptDataPath();
            
            // 如果没有指定 encryptDataPath，从 API 配置中获取
            if ((encryptDataPath == null || encryptDataPath.isEmpty()) && requestBody.getApiCode() != null) {
                YyPlatformApi filter = new YyPlatformApi();
                filter.setPlatformId(platform.getPId());
                filter.setApiCode(requestBody.getApiCode());
                List<YyPlatformApi> apis = yyPlatformApiService.selectYyPlatformApiList(filter);
                if (!apis.isEmpty()) {
                    YyPlatformApi api = apis.get(0);
                    // 优先使用 encryptDataPath，其次使用 entryPath
                    if (api.getEncryptDataPath() != null && !api.getEncryptDataPath().isEmpty()) {
                        encryptDataPath = api.getEncryptDataPath();
                    } else if (api.getEntryPath() != null && !api.getEntryPath().isEmpty()) {
                        encryptDataPath = api.getEntryPath();
                    }
                }
            }
            
            // 如果仍然没有 encryptDataPath，使用默认路径 "data.o"（药师帮）
            if (encryptDataPath == null || encryptDataPath.isEmpty()) {
                encryptDataPath = "data.o";
            }
            
            // 从 responseData 中提取加密数据
            try {
                Object jsonObj = JSON.parse(requestBody.getResponseData());
                encryptData = extractValueByPath(jsonObj, encryptDataPath);
                if (encryptData == null || encryptData.isEmpty()) {
                    return AjaxResult.error("无法从响应数据中提取加密数据，路径: " + encryptDataPath);
                }
            } catch (Exception e) {
                return AjaxResult.error("解析响应数据失败: " + e.getMessage());
            }
        }

        // 如果仍然没有加密数据，返回错误
        if (encryptData == null || encryptData.isEmpty()) {
            return AjaxResult.error("缺少加密数据");
        }

        // 获取金库
        YyPlatformKeyVault vault = yyPlatformKeyVaultService.selectYyPlatformKeyVaultByPlatformId(platform.getPId());
        if (vault == null) {
            return AjaxResult.error("密钥配置不存在");
        }

        // 查询 yy_platform_encrypt_type 字典
        List<SysDictData> encryptTypeList = DictUtils.getDictCache("yy_platform_encrypt_type");
        if (encryptTypeList == null || encryptTypeList.isEmpty()) {
            return AjaxResult.error("字典配置不存在: yy_platform_encrypt_type");
        }
        Map<String, String> encryptTypeMap = encryptTypeList.stream()
            .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));

        // 获取对应的加密类型标签
        String encryptTypeLabel = encryptTypeMap.get(dataEncryptType.toString());
        if (encryptTypeLabel == null) {
            return AjaxResult.error("不支持的加密类型: " + dataEncryptType);
        }

        // 根据加密类型标签解密
        String decryptedData = null;
        try {
            switch (encryptTypeLabel) {
                case "AES-128-ECB":
                    if (vault.getSymmetricKey() == null) {
                        return AjaxResult.error("AES密钥未配置");
                    }
                    decryptedData = DataDecryptUtil.decryptAES128ECB(encryptData, vault.getSymmetricKey());
                    break;
                case "AES-128-CBC":
                    if (vault.getSymmetricKey() == null) {
                        return AjaxResult.error("AES密钥未配置");
                    }
                    if (vault.getSymmetricIv() == null) {
                        return AjaxResult.error("AES IV未配置");
                    }
                    decryptedData = DataDecryptUtil.decryptAES128CBC(encryptData, vault.getSymmetricKey(), vault.getSymmetricIv());
                    break;
                case "AES-128-GCM":
                    if (vault.getSymmetricKey() == null) {
                        return AjaxResult.error("AES密钥未配置");
                    }
                    if (vault.getSymmetricIv() == null) {
                        return AjaxResult.error("AES GCM IV未配置");
                    }
                    decryptedData = DataDecryptUtil.decryptAES128GCM(encryptData, vault.getSymmetricKey(), vault.getSymmetricIv());
                    break;
                case "DES-ECB":
                    if (vault.getSymmetricKey() == null) {
                        return AjaxResult.error("DES密钥未配置");
                    }
                    decryptedData = DataDecryptUtil.decryptDES(encryptData, vault.getSymmetricKey());
                    break;
                case "RSA-PRIVATE-KEY":
                    if (vault.getRsaPrivateKey() == null) {
                        return AjaxResult.error("RSA私钥未配置");
                    }
                    decryptedData = DataDecryptUtil.decryptRSA(encryptData, vault.getRsaPrivateKey());
                    break;
                case "RSA-PUBLIC-KEY":
                    if (vault.getRsaPublicKey() == null) {
                        return AjaxResult.error("RSA公钥未配置");
                    }
                    decryptedData = DataDecryptUtil.decryptRSAWithPublicKey(encryptData, vault.getRsaPublicKey());
                    break;
                case "SM4-ECB":
                    if (vault.getSymmetricKey() == null) {
                        return AjaxResult.error("SM4密钥未配置");
                    }
                    decryptedData = DataDecryptUtil.decryptSM4ECB(encryptData, vault.getSymmetricKey());
                    break;
                case "SM4-CBC":
                    if (vault.getSymmetricKey() == null) {
                        return AjaxResult.error("SM4密钥未配置");
                    }
                    if (vault.getSymmetricIv() == null) {
                        return AjaxResult.error("SM4 IV未配置");
                    }
                    decryptedData = DataDecryptUtil.decryptSM4CBC(encryptData, vault.getSymmetricKey(), vault.getSymmetricIv());
                    break;
                case "BASE64":
                    try {
                        byte[] decodedBytes = com.ruoyi.common.utils.sign.Base64.decode(encryptData);
                        if (decodedBytes != null) {
                            decryptedData = new String(decodedBytes, "UTF-8");
                        } else {
                            return AjaxResult.error("BASE64解码失败");
                        }
                    } catch (Exception e) {
                        return AjaxResult.error("BASE64解码失败: " + e.getMessage());
                    }
                    break;
                default:
                    return AjaxResult.error("不支持的加密类型: " + encryptTypeLabel);
            }
            // 应用字段映射
            Map<String, Object> mapResult = dataFusionService.mapOnly(platformCode, decryptedData, entryPath);

            // 数据落库 - 将标准化后的数据保存到数据库
            try {
                YyDataIngestDTO ingestDto = new YyDataIngestDTO();
                ingestDto.setPlatformCode(platformCode);
                ingestDto.setApiCode(requestBody.getApiCode());
                ingestDto.setEncryptData(decryptedData);
                ingestDto.setDataEncryptType(0); // 已解密
                ingestDto.setCollectedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                dataFusionService.ingest(ingestDto);
                mapResult.put("saved", true);
            } catch (Exception e) {
                log.warn("数据落库失败（不影响前端展示）: {}", e.getMessage());
                mapResult.put("saved", false);
            }

            // 解析解密后的 JSON
            Object rawJson;
            try {
                rawJson = JSON.parse(decryptedData);
            } catch (Exception e) {
                rawJson = decryptedData;
            }

            // 根据 dataSource 决定是否返回原始数据
            // dataSource = 1 表示服务后台，需要返回原始数据用于配置对比
            // dataSource = 0 或 null 表示用户前端，不返回原始数据（性能优化）
            if (requestBody.getDataSource() != null && requestBody.getDataSource() == 1) {
                mapResult.put("raw", rawJson);
            }

            return AjaxResult.success((String) mapResult.get("message"), mapResult);

        } catch (Exception e) {
            return AjaxResult.error("解密失败: " + e.getMessage());
        }
    }

    /**
     * 根据路径从 JSON 对象中提取值
     * @param obj JSON 对象
     * @param path 路径，如 "data.o" 或 "data.wholesales[0].name"
     * @return 提取的值（字符串）
     */
    private String extractValueByPath(Object obj, String path) {
        if (obj == null || path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = obj;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            // 处理数组索引，如 "wholesales[0]"
            if (part.contains("[") && part.endsWith("]")) {
                String arrayName = part.substring(0, part.indexOf("["));
                String indexStr = part.substring(part.indexOf("[") + 1, part.length() - 1);
                try {
                    int index = Integer.parseInt(indexStr);
                    if (current instanceof java.util.Map) {
                        current = ((java.util.Map) current).get(arrayName);
                    }
                    if (current instanceof java.util.List) {
                        java.util.List<?> list = (java.util.List<?>) current;
                        if (index >= 0 && index < list.size()) {
                            current = list.get(index);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                // 普通键值
                if (current instanceof java.util.Map) {
                    current = ((java.util.Map) current).get(part);
                } else {
                    return null;
                }
            }
        }

        // 将结果转换为字符串
        if (current == null) {
            return null;
        } else if (current instanceof String) {
            return (String) current;
        } else {
            return current.toString();
        }
    }
}
