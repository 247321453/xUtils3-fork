package org.xutils.common;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {
    public static final String TAG = AESUtils.class.getSimpleName();

    private static final String AES = "AES";
    private static final String SHA1 = "SHA1PRNG";
    private static final String AES_METHOD = "AES/CFB/NoPadding";
    private static final String AES_METHOD2 = "AES/CBC/PKCS5Padding";

    /**
     * 使用一个安全的随机数来产生一个密匙,密匙加密使用的
     *
     * @param seed
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static byte[] getRawKey(byte[] seed) throws NoSuchAlgorithmException {
        // 获得一个随机数，传入的参数为默认方式。
        SecureRandom sr;
        if (android.os.Build.VERSION.SDK_INT < 17) {
            sr = SecureRandom.getInstance(SHA1);
        } else {
            try {
                sr = SecureRandom.getInstance(SHA1, "Crypto");
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
                sr = SecureRandom.getInstance(SHA1);
            }
        }
        // 设置一个种子,一般是用户设定的密码
        sr.setSeed(seed);
        // 获得一个key生成器（AES加密模式）
        KeyGenerator keyGen = KeyGenerator.getInstance(AES);
        // 设置密匙长度128位
        keyGen.init(128, sr);
        // 获得密匙
        SecretKey key = keyGen.generateKey();
        // 返回密匙的byte数组供加解密使用
        byte[] raw = key.getEncoded();
        return raw;
    }

    /**
     * 加密后的字符串
     *
     * @param seed
     * @return
     */
    public static String encrypt(String seed, String source) {
        // Log.d(TAG, "加密前的seed=" + seed + ",内容为:" + clearText);
        byte[] result = null;
        try {
            byte[] rawkey = getRawKey(seed.getBytes());
            result = encrypt(rawkey, source.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String content = toHex(result);
        return content;

    }

    /**
     * 解密后的字符串
     *
     * @param seed
     * @param encrypted
     * @return
     */
    public static String decrypt(String seed, String encrypted) {
        byte[] rawKey;
        try {
            rawKey = getRawKey(seed.getBytes());
            byte[] enc = toByte(encrypted);
            byte[] result = decrypt(rawKey, enc);
            String coentn = new String(result);
            return coentn;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 结合密钥生成加密后的密文
     *
     * @param raw
     * @param input
     * @return
     * @throws Exception
     */
    private static byte[] encrypt(byte[] raw, byte[] input) throws Exception {
        // 根据上一步生成的密匙指定一个密匙
        SecretKeySpec skeySpec = new SecretKeySpec(raw, AES);
        // Cipher cipher = Cipher.getInstance("AES");
        // 加密算法，加密模式和填充方式三部分或指定加密算
        Cipher cipher = Cipher.getInstance(AES_METHOD2);
        // 初始化模式为加密模式，并指定密匙
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(
                new byte[cipher.getBlockSize()]));
        byte[] encrypted = cipher.doFinal(input);
        return encrypted;
    }

    /**
     * 根据密钥解密已经加密的数据
     *
     * @param raw
     * @param encrypted
     * @return
     * @throws Exception
     */
    private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, AES);
        Cipher cipher = Cipher.getInstance(AES_METHOD2);//
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(
                new byte[cipher.getBlockSize()]));
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    private static byte[] toByte(String hexString) {
        return Base64.decode(hexString, Base64.DEFAULT);
    }

    private static String toHex(byte[] buf) {
        return Base64.encodeToString(buf, Base64.DEFAULT);
    }
}