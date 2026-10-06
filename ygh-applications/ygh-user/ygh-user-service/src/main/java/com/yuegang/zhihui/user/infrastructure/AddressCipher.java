package com.yuegang.zhihui.user.infrastructure;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/** AES-256-GCM 字段加密。IV（初始化向量）是随机生成的，并作为每个加密值的前缀。支持密钥轮转场景下的跨版本解密。 */
public final class AddressCipher {
    private static final int IV_BYTES = 12; // GCM 模式推荐的 IV 长度为 12 字节
    private final SecretKeySpec key; // AES 密钥规格对象
    private final int keyVersion; // 密钥版本号（用于支持后续密钥轮转）
    private final SecureRandom random; // 强随机数生成器

    /** 公开构造函数：接受 Base64 格式的密钥和版本，内部完成密钥构造 */
    public AddressCipher(String keyBase64, int keyVersion) {
        this(keyBase64, keyVersion, new SecureRandom());
    }

    /** 内部构造函数，支持注入随机源（便于测试） */
    AddressCipher(String keyBase64, int keyVersion, SecureRandom random) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(Objects.requireNonNull(keyBase64));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("PII key must be valid Base64", invalid);
        }
        if (decoded.length != 32) throw new IllegalArgumentException("PII key must contain exactly 32 bytes");
        if (keyVersion < 1 || keyVersion > 65535) throw new IllegalArgumentException("PII key version is invalid");
        this.key = new SecretKeySpec(decoded, "AES"); // 从解码后的字节数组构造 AES 密钥
        Arrays.fill(decoded, (byte) 0); // 擦除内存中的明文密钥数组
        this.keyVersion = keyVersion;
        this.random = Objects.requireNonNull(random);
    }

    public int keyVersion() { return keyVersion; }

    /** 加密方法：传入用户ID、字段名和明文，返回包含IV前缀的密文字节数组 */
    public byte[] encrypt(long userId, String field, String value) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        byte[] plain = value.getBytes(StandardCharsets.UTF_8);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(aad(userId, field, keyVersion));
            return ByteBuffer.allocate(iv.length + cipher.getOutputSize(plain.length)).put(iv).put(cipher.doFinal(plain)).array();
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("PII encryption failed", failure);
        } finally {
            Arrays.fill(plain, (byte) 0);
        }
    }

    /** 解密方法：支持跨版本解密（仅校验版本范围，不要求严格匹配当前密钥版本） */
    public String decrypt(long userId, String field, int storedVersion, byte[] value) {
        if (storedVersion < 1 || storedVersion > 65535 || value == null || value.length <= IV_BYTES + 16)
            throw new IllegalStateException("PII ciphertext or key version is invalid");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, value, 0, IV_BYTES));
            cipher.updateAAD(aad(userId, field, storedVersion));
            byte[] plain = cipher.doFinal(value, IV_BYTES, value.length - IV_BYTES);
            try {
                return new String(plain, StandardCharsets.UTF_8);
            } finally {
                Arrays.fill(plain, (byte) 0);
            }
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("PII decryption failed", failure);
        }
    }

    private static byte[] aad(long userId, String field, int version) {
        return (userId + ":" + field + ":" + version).getBytes(StandardCharsets.US_ASCII);
    }
}
