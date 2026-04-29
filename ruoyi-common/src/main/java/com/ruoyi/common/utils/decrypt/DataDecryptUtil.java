package com.ruoyi.common.utils.decrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

public class DataDecryptUtil {

    private static final String AES_ECB_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String AES_CBC_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String DES_ECB_ALGORITHM = "DES/ECB/PKCS5Padding";
    private static final String SM4_ECB_ALGORITHM = "SM4/ECB/PKCS5Padding";
    private static final String SM4_CBC_ALGORITHM = "SM4/CBC/PKCS5Padding";
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * AES-128-ECB解密方法
     * @param encryptedData 需要解密的数据（Base64编码）
     * @param key 解密密钥（16字节字符串）
     * @return 解密后的数据
     * @throws Exception 解密异常
     */
    public static String decryptAES128ECB(String encryptedData, String key) throws Exception {
        return decryptAES128ECB(encryptedData, key, StandardCharsets.UTF_8.name());
    }

    public static String decryptAES128ECB(String encryptedData, String key, String charset) throws Exception {
        validateAesKey(key);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(AES_ECB_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return decompressGzipIfNeeded(decryptedBytes, charset);
    }

    /**
     * AES-128-ECB加密方法
     * @param plainData 明文数据
     * @param key 加密密钥（16字节字符串）
     * @return 加密后的数据（Base64编码）
     * @throws Exception 加密异常
     */
    public static String encryptAES128ECB(String plainData, String key) throws Exception {
        validateAesKey(key);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(AES_ECB_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(plainData.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * AES-128-CBC解密方法
     * @param encryptedData 需要解密的数据（Base64编码）
     * @param key 解密密钥（16字节字符串）
     * @param iv 初始向量（16字节字符串）
     * @return 解密后的数据
     * @throws Exception 解密异常
     */
    public static String decryptAES128CBC(String encryptedData, String key, String iv) throws Exception {
        validateAesKey(key);
        validateIv(iv);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

        Cipher cipher = Cipher.getInstance(AES_CBC_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return decompressGzipIfNeeded(decryptedBytes, null);
    }

    /**
     * AES-128-GCM解密方法
     * @param encryptedData 需要解密的数据（Base64编码，包含TAG）
     * @param key 解密密钥（16字节字符串）
     * @param iv 初始向量（12字节字符串）
     * @return 解密后的数据
     * @throws Exception 解密异常
     */
    public static String decryptAES128GCM(String encryptedData, String key, String iv) throws Exception {
        validateAesKey(key);
        validateGcmIv(iv);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv.getBytes(StandardCharsets.UTF_8));

        Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return decompressGzipIfNeeded(decryptedBytes, null);
    }

    /**
     * DES-ECB解密方法
     * @param encryptedData 需要解密的数据（Base64编码）
     * @param key 解密密钥（8字节字符串）
     * @return 解密后的数据
     * @throws Exception 解密异常
     */
    public static String decryptDES(String encryptedData, String key) throws Exception {
        validateDesKey(key);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");

        Cipher cipher = Cipher.getInstance(DES_ECB_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * RSA私钥解密方法
     * @param encryptedData 需要解密的数据（Base64编码）
     * @param privateKeyBase64 私钥内容（Base64编码PKCS#8）
     * @return 解密后的数据
     * @throws Exception 解密异常
     */
    public static String decryptRSA(String encryptedData, String privateKeyBase64) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * RSA公钥加密方法
     * @param plainData 明文字符串
     * @param publicKeyBase64 公钥内容（Base64编码，X.509格式）
     * @return Base64编码的密文
     * @throws Exception 加密异常
     */
    public static String encryptRSA(String plainData, String publicKeyBase64) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encryptedBytes = cipher.doFinal(plainData.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * RSA公钥解密方法
     * @param encryptedData 需要解密的数据（Base64编码）
     * @param publicKeyBase64 公钥内容（Base64编码，X.509格式）
     * @return 解密后的明文
     * @throws Exception 解密异常
     */
    public static String decryptRSAWithPublicKey(String encryptedData, String publicKeyBase64) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * SM4-ECB解密方法
     * @param encryptedData 需要解密的数据（Base64编码）
     * @param key 解密密钥（16字节字符串）
     * @return 解密后的数据
     * @throws Exception 解密异常
     */
    public static String decryptSM4ECB(String encryptedData, String key) throws Exception {
        validateSm4Key(key);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "SM4");

        Cipher cipher = Cipher.getInstance(SM4_ECB_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * SM4-CBC解密方法
     * @param encryptedData 需要解密的数据（Base64编码）
     * @param key 解密密钥（16字节字符串）
     * @param iv 初始向量（16字节字符串）
     * @return 解密后的数据
     * @throws Exception 解密异常
     */
    public static String decryptSM4CBC(String encryptedData, String key, String iv) throws Exception {
        validateSm4Key(key);
        validateSm4Iv(iv);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "SM4");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

        Cipher cipher = Cipher.getInstance(SM4_CBC_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Base64解码
     * @param base64Data Base64字符串
     * @return 解码后的明文
     */
    public static String decodeBase64(String base64Data) {
        return new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8);
    }

    /**
     * Hex字符串解码
     * @param hexData 十六进制字符串
     * @return 解码后的明文
     */
    public static String decodeHex(String hexData) {
        return new String(hexStringToBytes(hexData), StandardCharsets.UTF_8);
    }

    private static void validateAesKey(String key) {
        if (key == null || key.getBytes(StandardCharsets.UTF_8).length != 16) {
            throw new IllegalArgumentException("AES key must be 16 bytes long");
        }
    }

    private static void validateIv(String iv) {
        if (iv == null || iv.getBytes(StandardCharsets.UTF_8).length != 16) {
            throw new IllegalArgumentException("AES IV must be 16 bytes long");
        }
    }

    private static void validateGcmIv(String iv) {
        if (iv == null || iv.getBytes(StandardCharsets.UTF_8).length != 12) {
            throw new IllegalArgumentException("AES GCM IV must be 12 bytes long");
        }
    }

    private static void validateDesKey(String key) {
        if (key == null || key.getBytes(StandardCharsets.UTF_8).length != 8) {
            throw new IllegalArgumentException("DES key must be 8 bytes long");
        }
    }

    private static void validateSm4Key(String key) {
        if (key == null || key.getBytes(StandardCharsets.UTF_8).length != 16) {
            throw new IllegalArgumentException("SM4 key must be 16 bytes long");
        }
    }

    private static void validateSm4Iv(String iv) {
        if (iv == null || iv.getBytes(StandardCharsets.UTF_8).length != 16) {
            throw new IllegalArgumentException("SM4 IV must be 16 bytes long");
        }
    }

    private static byte[] hexStringToBytes(String hex) {
        if (hex == null) {
            return new byte[0];
        }
        String normalized = hex.trim();
        if (normalized.length() % 2 != 0) {
            normalized = "0" + normalized;
        }
        int len = normalized.length();
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) ((Character.digit(normalized.charAt(i), 16) << 4)
                    + Character.digit(normalized.charAt(i + 1), 16));
        }
        return result;
    }


    /**
     * 判断并解压 GZIP 数据
     * @param data 原始字节数组
     * @return 解压后的字符串，如果不是 GZIP 格式，则直接按普通字符串返回
     */
    private static String decompressGzipIfNeeded(byte[] data, String charset) {
        if (data == null || data.length < 2) {
            try { return new String(data, charset); } catch (Exception e) { return new String(data); }
        }

        // 💡 核心判断：如果开头是 1F 8B (十进制的 31 和 -117)，说明被压缩了
        boolean isGzip = (data[0] == (byte) 31) && (data[1] == (byte) -117);

        if (!isGzip) {
            // 如果没压缩，按普通明文转字符串
            try { return new String(data, charset); } catch (Exception e) { return new String(data); }
        }

        // 开始解压缩流
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             GZIPInputStream gis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            
            // 解压完毕，输出干干净净的 JSON 明文！
            return baos.toString(charset);

        } catch (Exception e) {
            System.err.println("⚠️ GZIP 解压失败，兜底输出原始字符串: " + e.getMessage());
            try { return new String(data, charset); } catch (Exception ex) { return new String(data); }
        }
    }
}