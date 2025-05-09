package com.github.im.group.gui.config;

import com.gluonhq.connect.provider.FileClient;

//import javax.crypto.Cipher;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * 安全配置
 */
public class SecureSettings {

    private static final String SECRET_KEY = "0123456789abcdef"; // 16-byte key (128-bit)

    private static final String AES = "AES";

    private static final String TOKEN_KEY = "access_token";
    private static final String UNAME_KEY = "access_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";

    private static final Preferences prefs = Preferences.userNodeForPackage(SecureSettings.class);

    public static void saveSecretToken( String refreshToken) {
        prefs.put(REFRESH_TOKEN_KEY, refreshToken);
    }


    public static void saveUserName(String userName) {
        prefs.put(UNAME_KEY, userName);
    }

    /**
     * 获取密钥 Token 用于自动登录
     * @return 没有默认返回null
     */
    public static Optional<String> getSecretToken() {
        return Optional.ofNullable(prefs.get(REFRESH_TOKEN_KEY, null));
    }

    public static Optional<String> getUserName(){
        return Optional.ofNullable(prefs.get(UNAME_KEY, null));
    }

    public static void clearTokens() {
        prefs.remove(REFRESH_TOKEN_KEY);
//        prefs.remove(REFRESH_TOKEN_KEY);
    }

//
//    /**
//     * 存储的时候二次加密
//     * @param key
//     * @param plainValue
//     */
//    public static void saveEncrypted(String key, String plainValue) {
//        try {
//            FileClient.create(settingsService.getFile()).createObjectDataWriter(null).write(key, plainValue);
//            String encrypted = encrypt(plainValue);
//            settingsService.store(key, encrypted);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to encrypt and save value", e);
//        }
//    }
//
//    /**
//     * 读取的时候解密
//     * @param key
//     * @return
//     */
//    public static Optional<String> loadDecrypted(String key) {
//        try {
//            return settingsService.retrieve(key).map(SecureSettings::decrypt);
//        } catch (Exception e) {
//            return Optional.empty();
//        }
//    }
//
//    public static void remove(String key) {
//        settingsService.remove(key);
//    }

    private static String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance(AES);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance(AES);
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), AES);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
