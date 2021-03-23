package com.dizsun.timechain.component;

//import sun.misc.BASE64Decoder;
//import sun.misc.BASE64Encoder;


import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

public class PubPriKey {
    private static final String ALGORITHM = "RSA";
    /**
     * 密钥长度，用来初始化
     */
    private static final int KEYSIZE = 1024;
    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT_BLOCK = 117;

    /**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT_BLOCK = 128;

    private Key publicKey;
    private String publicKeyBase64;
    private Key privateKey;
//    private String privateKeyBase64;

    public PubPriKey() {
    }

    public void init(){
        try {
            /** RSA算法要求有一个可信任的随机数源 */
            SecureRandom secureRandom = new SecureRandom();

            /** 为RSA算法创建一个KeyPairGenerator对象 */
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);

            /** 利用上面的随机数据源初始化这个KeyPairGenerator对象 */
            keyPairGenerator.initialize(KEYSIZE, secureRandom);
            //keyPairGenerator.initialize(KEYSIZE);

            /** 生成密匙对 */
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            /** 得到公钥、私钥 */
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

            byte[] publicKeyBytes = publicKey.getEncoded();
//            publicKeyBase64 = new BASE64Encoder().encode(publicKeyBytes);
            publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyBytes);

            //byte[] privateKeyBytes = privateKey.getEncoded();
            //this.privateKeyBase64 = new BASE64Encoder().encode(privateKeyBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(Key publicKey) {
        this.publicKey = publicKey;
    }

    public void setPrivateKey(Key privateKey) {
        this.privateKey = privateKey;
    }

    public void setPublicKeyBase64(String publicKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PubPriKey pubPriKey = (PubPriKey) o;
        return publicKey.equals(pubPriKey.publicKey) &&
                privateKey.equals(pubPriKey.privateKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicKey, privateKey);
    }

    @Override
    public String toString() {
        return "PubPriKey{" +
                "publicKey='" + publicKey + '\'' +
                ", privateKey='" + privateKey + '\'' +
                '}';
    }

    /**
     * 加密方法
     *
     * @param source 源数据
     * @return
     * @throws Exception
     */
    public String encrypt(String source) {
        try {
            /** 得到Cipher对象来实现对源数据的RSA加密 */
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] data = source.getBytes();
            /** 执行加密操作 */

            int inputLen = data.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段加密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(data, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_ENCRYPT_BLOCK;
            }
            byte[] encryptedData = out.toByteArray();
            out.close();

//            byte[] b1 = cipher.doFinal(b);
//            BASE64Encoder encoder = new BASE64Encoder();
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 解密算法
     *
     * @param cryptograph 密文
     * @return
     * @throws Exception
     */
    public String decrypt(String cryptograph) {
        try {
            /** 得到Cipher对象对已用私钥加密的数据进行RSA解密 */
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
//            BASE64Decoder decoder = new BASE64Decoder();
//            byte[] encryptedData = decoder.decodeBuffer(cryptograph);
            byte[] encryptedData = Base64.getDecoder().decode(cryptograph);
            /** 执行解密操作 */

            int inputLen = encryptedData.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段解密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                    cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_DECRYPT_BLOCK;
            }
            byte[] decryptedData = out.toByteArray();
            out.close();

            //byte[] b = cipher.doFinal(decryptedData);
            return new String(decryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public String decrypt(String _publicKeyBase64, String cryptograph) {
        try {
//            BASE64Decoder decoder = new BASE64Decoder();
//            byte[] buffer = decoder.decodeBuffer(_publicKeyBase64);
            byte[] buffer = Base64.getDecoder().decode(_publicKeyBase64);
            Key key = bytes2PK(buffer);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
//            byte[] encryptedData = decoder.decodeBuffer(cryptograph);
            byte[] encryptedData = Base64.getDecoder().decode(cryptograph);
            /** 执行解密操作 */

            int inputLen = encryptedData.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段解密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                    cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_DECRYPT_BLOCK;
            }
            byte[] decryptedData = out.toByteArray();
            out.close();

//            byte[] b = cipher.doFinal(b1);
            return new String(decryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从字节数组恢复出publickey
     * @param buf
     * @return
     */
    private Key bytes2PK(byte[] buf) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec encodedPublicKey = new X509EncodedKeySpec(buf);
            return  kf.generatePublic(encodedPublicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
