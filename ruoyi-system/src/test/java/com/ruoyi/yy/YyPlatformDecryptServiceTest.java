package com.ruoyi.yy;

import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.yy.domain.YyPlatform;
import com.ruoyi.yy.domain.YyPlatformKeyVault;
import com.ruoyi.yy.mapper.YyPlatformKeyVaultMapper;
import com.ruoyi.yy.service.IYyPlatformService;
import com.ruoyi.yy.service.impl.YyPlatformDecryptServiceImpl;
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YyPlatformDecryptServiceTest {

    @Mock
    private IYyPlatformService platformService;

    @Mock
    private YyPlatformKeyVaultMapper vaultMapper;

    @InjectMocks
    private YyPlatformDecryptServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void decrypt_base64() {
        // 准备平台
        YyPlatform platform = new YyPlatform();
        platform.setPId(1L);
        when(platformService.selectYyPlatformByCode("TEST")).thenReturn(platform);

        // 准备密钥金库（BASE64 不需要实际密钥）
        YyPlatformKeyVault vault = new YyPlatformKeyVault();
        when(vaultMapper.selectYyPlatformKeyVaultByPlatformId(1L)).thenReturn(vault);

        // 准备字典数据：映射 dataEncryptType=1 → "BASE64"
        SysDictData dictData = new SysDictData();
        dictData.setDictValue("1");
        dictData.setDictLabel("BASE64");

        try (MockedStatic<DictUtils> dictMock = mockStatic(DictUtils.class)) {
            dictMock.when(() -> DictUtils.getDictCache("yy_platform_encrypt_type"))
                    .thenReturn(java.util.List.of(dictData));

            String result = service.decrypt("TEST", "aGVsbG8=", 1);
            assertEquals("hello", result);
        }
    }

    @Test
    void decrypt_passThrough() {
        // dataEncryptType=-1 表示明文直传，输入原样返回
        String result = service.decrypt("ANY", "rawData", -1);
        assertEquals("rawData", result);
    }

    @Test
    void decrypt_unknownPlatform() {
        when(platformService.selectYyPlatformByCode("UNKNOWN")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            service.decrypt("UNKNOWN", "data", 1);
        });
    }
}
