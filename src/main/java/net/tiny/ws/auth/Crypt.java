package net.tiny.ws.auth;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 公钥/私钥/签名/加密/解密/工具包
 * <p>
 * 环境变量'SECRET_KEY_FILE','RSA_KEY_FILE','TINY_KEY_FILE'定义的文件里的密钥优先，<br/>
 * 缺省使用内置的密钥，<br/>
 * 字符串格式的密钥在未在特殊说明情况下都为BASE64编码格式<br/>
 * 由于非对称加密速度极其缓慢，一般文件不使用它来加密而是使用对称加密，<br/>
 * 非对称加密算法可以用来对对称加密的密钥加密，这样保证密钥的安全也就保证了数据的安全
 * </p>
 */
public final class Crypt {
    private static Logger LOGGER = Logger.getLogger(Crypt.class.getCanonicalName());
    /** 密钥算法  */
    public enum Algorithm {
        AES,
        DES,
        RSA,
        TINY //RSA256,
    }
    /** 签名算法  */
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    /** AES加密解密 */
    public static Algorithm AES = Algorithm.AES;

    /** DES加密解密 */
    public static Algorithm DES = Algorithm.DES;

    /** 512 - 1024位RSA加密解密 */
    public static Algorithm RSA = Algorithm.RSA;

    /** 256位RSA加密解密 */
    public static Algorithm TINY = Algorithm.TINY;

    // PKCS5和PKCS7是没有问题的，因为都是在相同的填充算法。
    // PKCS7 http://www.rfc-editor.org/rfc/rfc2315.txt
    // PKCS5 http://www.rfc-editor.org/rfc/rfc2898.txt
    /** 安全服务提供者 */
    public static final String MD5_ALGORITHM  = "MD5";
    public static final String RSA_ALGORITHM  = "RSA";
    public static final String RSA_TRANSFORMATION = RSA_ALGORITHM + "/ECB/PKCS1Padding";
    public static final String DES_ALGORITHM  = "DES";
    public static final String DES_TRANSFORMATION = DES_ALGORITHM + "/CBC/PKCS5Padding"; //DES/CBC/PKCS7Padding DES/CBC/PKCS5Padding
    /** 密钥大小 */
    public static final int RSA_KEY_SIZE = 1024;

    public static final String AES_ALGORITHM = "AES";
    public static final String AES_TRANSFORMATION = AES_ALGORITHM + "/CBC/PKCS5Padding";

    /**模量 */
    public static final String MODULUS_NAME  = "modulus";
    /**指数 */
    public static final String EXPONENT_NAME         = "exponent";
    /**公钥 */
    public static final String PUBLIC_KEY_NAME       = "publicKey";
    /** */
    public static final String PRIVATE_KEY_NAME      = "privateKey";
    /**密钥 */
    public static final String PRIVATE_EXPONENT_NAME = "privateExponent";

    /** RSA最大加密明文大小 */
    private static final int MAX_ENCRYPT_BLOCK = 117;

    /** RSA最大解密密文大小  */
    private static final int MAX_DECRYPT_BLOCK = 128;

    /** 密码格式 */
    private static final String PASSWORD_REGEX = "^\\[.*\\]$";

    /** 密钥大小 */
    // If use 256bit key need download jce_policy-8.zip

    /** AES DES 加密密钥  */
    private final static String EVN_SECRET_KEY_FILE = "SECRET_KEY_FILE";
    private final static char[] SECRET_KEY  = {
            '_',
            '0', 'P',
            '1', 'a',
            '2', 'S',
            '3', 's',
            '4', 'W',
            '5', 'o',
            '6', 'R',
            '7', 'd',
            '8', '_'
            };

    private final static String DELIM = ":";
    private static byte[] KEY_BYTES = null;
    private static byte[] IV_BYTES  = null;

    protected byte[] specKey;
    protected byte[] specIv;

    private final static String EVN_TINY_KEY_FILE = "TINY_KEY_FILE";
    private final static String TINY_KEY_VALUE256 = "1QUCL0fLuCDXt5Olcl574yfC5FldFjhavrCxnszrMkSP:86GnTBm1cMhtXmuSVH03qXigBMgEXRQbfzvaxhRaM9hMb:1J6jFKd6j4D4mTXoOMYFmuJP7oPlPlq5t3MJAyFgeGRR";
    private static TinyKey TINY_KEY = null;

    protected boolean tiny = false;
    protected TinyKey tinyKey = TINY_KEY;

    private final static String EVN_RSA_KEY_FILE = "RSA_KEY_FILE";
     private final static String[] RSA_KEY_VALUES = new String[] {
             "AN0T6S8kt86xrfafynyFjvxT7lSfsd6f/ilHuuP6G6uQbdt+yep1KTwEPrVjOwLJKgI1ywhw4qtsEXUNaPw/CoSXURqRGLzpzt3FkGvEfZUBNvye0/0jlqyGlVpn0Ngn7dBh6/+bN8CiuU3mN1GKtNJ7jDGoG5pYUtTR91LI+5Q/",
             "AQAB",
             "VX2Am9FoHs7IxekOxU5kd6EBNco3Xy6he1cYp1YtYw/L26hQ7pB17JZ7pWsFA9PEoewpYk886Cs3KPuRkJHUP691VowEVDvwHXiXE+tpTTFgnZVyVSYvIfKCdMb9+CsPv7DmcIHKczwJHWuXaG76+uD5RQAO92HNv30Fh2+G4Ok="
     };

     private static RSAKeyPair RSA_KEY = null;

    protected RSAKeyPair rsaKey = RSA_KEY;
    private final Algorithm algorithm;
    private SecretKeySpec keySpec;
    private IvParameterSpec ivSpec;
    private Cipher cipher;

    /**
     * 实例化
     */
    private Crypt(Algorithm algorithm) {
        this.algorithm = algorithm;
        switch(this.algorithm) {
        case AES:
            initializeAESCipher();
            break;
        case DES:
            initializeDESCipher();
            break;
        case RSA:
            initializeRSACipher();
            break;
        case TINY:
            initializTinyCipher();
            break;
        }
    }

    /** BASE64编码公钥 */
    public String getEncodedPublicKey() {
        return rsaKey.getEncodedPublicKey();
    }

    /** 公钥 指数*/
    public String getPublicExponent() {
        return rsaKey.getPublicExponent();
    }

    /**
     * 加密
     *
     * @param data
     *            数据
     * @return 加密后的数据
     * @throws SecurityException
     */
    public byte[] encrypt(byte[] data)  {
        return encrypt(data, true);
    }

    public byte[] encrypt(byte[] data, boolean byPublicKey)  {
        try {
            byte[] bytes = null;
            switch(this.algorithm) {
            case TINY:
                bytes = encode(data);
                break;
            case RSA:
                if(byPublicKey) {
                    bytes = encrypt(cipher, rsaKey.getPublicKey(), data);
                } else {
                    bytes = encrypt(cipher, rsaKey.getPrivateKey(), data);
                }
                break;
            default:
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
                bytes = cipher.doFinal(data);
                break;
            }
            return bytes;
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    /**
     * 加密
     *
     * @param text
     *            字符串
     *
     * @return 编码字符串
     */
    public String encrypt(String text) {
        byte[] data = encrypt( text.getBytes() );
        return data != null ? Base64.getEncoder().encodeToString(data) : null;
    }

    /**
     * 解密
     *
     * @param encryptedData
     *            已加密数据
     * @return 解密后的数据
     * @throws SecurityException
     */
    public byte[] decrypt(byte[] encryptedData) {
        return decrypt(encryptedData, true);
    }

    public byte[] decrypt(byte[] encryptedData, boolean byPrivateKey) {
        try {
            byte[] bytes = null;
            switch(this.algorithm) {
            case TINY:
                bytes = decode(encryptedData);
                break;
            case RSA:
                if(byPrivateKey) {
                    bytes = decrypt(cipher, rsaKey.getPrivateKey(), encryptedData);
                } else {
                    bytes = decrypt(cipher, rsaKey.getPublicKey(), encryptedData);
                }
                break;
            default:
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
                bytes = cipher.doFinal(encryptedData);
                break;
            }
            return bytes;
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    /**
     * 解密
     *
     * @param encrypted
     *            编码字符串
     * @return 解密后的数据
     */
    public String decrypt(String encrypted) {
        byte[] data  = decrypt(Base64.getDecoder().decode(encrypted));
        return data != null ? new String(data) : null;
    }

    /**
     * 加密
     *
     * @param data
     *            数据
     * @return 加密后的数据
     */
    byte[] encode(byte[] data) {
        BigInteger code = BigInteger.ZERO;
        //ASCII字符转换为256进制数
        for (int i = 0; i < data.length; i++) {
            code = code.multiply(BigInteger.valueOf(256));
            code = code.add(BigInteger.valueOf(data[i]));
        }
        return code.modPow(TINY_KEY.publicKey, TINY_KEY.commonKey).toByteArray();
    }

    byte[] decode(byte[] data) {
        //256进制数转换为ASCII字符
        BigInteger code = new BigInteger(data).modPow(TINY_KEY.privateKey, TINY_KEY.commonKey);
        StringBuilder sb = new StringBuilder();
        while (code.compareTo(BigInteger.ZERO) > 0) {
            int rem = code.mod(BigInteger.valueOf(256)).shortValue();
            sb.append((char)rem);
            code = code.divide(BigInteger.valueOf(256));
        }
        return sb.reverse().toString().getBytes();
    }

    public static Crypt create() {
           return new Crypt(Algorithm.DES);
    }

    public static Crypt create(String algorithm) {
        return new Crypt(Algorithm.valueOf(algorithm));
    }

    public static Crypt create(Algorithm algorithm) {
         return new Crypt(algorithm);
    }
    /**
     * 用密钥加密密码
     * @param password 密码
     * @return 加密后的密码
     * @see #encrypt(String)
     */
    public final static String encryptPassword(String password){
        return create(AES).encrypt(password);
    }

    /**
     * 解密密码
     *
     * @param password
     *            编码字符串[xxxxxxx]
     * @return 解密后的密码
     */
    public final static String decryptPassword(String password) {
        if (Pattern.matches(PASSWORD_REGEX, password)) {
            String value = password.substring(1, password.length() - 1);
            return create(AES).decrypt(value);
        } else {
            return password;
        }
    }
    /**
     * <p>
     * 公钥加密
     * </p>
     *
     * @param modulus 公钥 模数(BASE64编码)
     * @param publicExponent 已加密公钥(BASE64编码)
     * @param data 源数据
     * @return
     * @throws SecurityException
     */
    public static byte[] encryptByPublicKey(String modulus, String publicExponent, byte[] data) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            Key key = decodePublicKey(keyFactory, modulus, publicExponent);
            // 对数据加密
            return encrypt(key, data);
        } catch(GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    public static String encryptByPublicKey(String modulus, String publicExponent, String data) {
        return Base64.getEncoder().encodeToString(encryptByPublicKey(modulus, publicExponent, data.getBytes()));
    }


    /**
     * <p>
     * 公钥加密
     * </p>
     *
     * @param publicKey 公钥(BASE64编码)
     * @param data 源数据
     * @return
     * @throws SecurityException
     */
    public static byte[] encryptByPublicKey(String publicKey, byte[] data) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            Key key = keyFactory.generatePublic(x509KeySpec);
            // 对数据加密
            return encrypt(key, data);
        } catch(GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    public static String encryptByPublicKey(String publicKey, String data) {
        return Base64.getEncoder().encodeToString(encryptByPublicKey(publicKey, data.getBytes()));
    }

    /**
     * <p>
     * 私钥加密
     * </p>
     *
     * @param privateKey 私钥(BASE64编码)
     * @param data 源数据
     * @return
     * @throws SecurityException
     */
    public static byte[] encryptByPrivateKey(String privateKey, byte[] data) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            Key key = keyFactory.generatePrivate(pkcs8KeySpec);
            // 对数据加密
            return encrypt(key, data);
        } catch(GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    public static String encryptByPrivateKey(String privateKey, String data) {
        return Base64.getEncoder().encodeToString(encryptByPrivateKey(privateKey, data.getBytes()));
    }

    protected static byte[] encrypt(Key key, byte[] data) throws GeneralSecurityException {
        // 对数据加密
        return encrypt(Cipher.getInstance(RSA_TRANSFORMATION), key, data);
    }

    /**
     * <p>
     * 加密
     * </p>
     *
     * @param cipher 密钥
     * @param key 键钥
     * @param data 源数据
     * @return
     * @throws SecurityException
     */
    private static byte[] encrypt(Cipher cipher, Key key, byte[] data) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
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
            return encryptedData;
        } catch(GeneralSecurityException | IOException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    /**
     * <p>
     * 公钥解密
     * </p>
     *
     * @param publicKey 公钥(BASE64编码)
     * @param encryptedData 已加密数据
     * @return
     * @throws GeneralSecurityException
     */
    public static byte[] decryptByPublicKey(String publicKey, byte[] encryptedData) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            Key key = keyFactory.generatePublic(x509KeySpec);
            return decrypt(key, encryptedData);
        } catch(GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    public static String decryptByPublicKey(String publicKey, String encryptedData) {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        return new String(decryptByPublicKey(publicKey, encryptedBytes));
    }

    /**
     * <P>
     * 私钥解密
     * </p>
     *
     * @param privateKey 私钥(BASE64编码)
     * @param encryptedData 已加密数据
     * @return
     * @throws SecurityException
     */
    public static byte[] decryptByPrivateKey(String privateKey, byte[] encryptedData) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            Key key = keyFactory.generatePrivate(pkcs8KeySpec);
            return decrypt(key, encryptedData);
        } catch(GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    public static String decryptByPrivateKey(String privateKey, String encryptedData) {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        return new String(decryptByPrivateKey(privateKey, encryptedBytes));
    }

    /**
     * <P>
     * 私钥解密
     * </p>
     *
     * @param modulus 公钥 模数(BASE64编码)
     * @param privateExponent 已加密私钥(BASE64编码)
     * @param crypt 私钥解码
     * @param encryptedData 已加密数据
     * @return
     * @throws SecurityException
     */
    public static byte[] decryptByPrivateKey(String modulus, String privateExponent, Crypt crypt, byte[] encryptedData) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            Key key = decodePrivateKey(keyFactory, modulus, privateExponent, crypt);
            return decrypt(key, encryptedData);
        } catch(GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    public static String decryptByPrivateKey(String modulus, String privateExponent, Crypt crypt, String encryptedData) {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        return new String(decryptByPrivateKey(modulus, privateExponent, crypt, encryptedBytes));
    }

    protected static byte[] decrypt(Key key, byte[] encryptedData) throws GeneralSecurityException {
        return decrypt(Cipher.getInstance(RSA_TRANSFORMATION), key, encryptedData);
    }

    /**
     * 解密
     *
     * @param privateKey
     *            私钥
     * @param data
     *            数据
     * @return 解密后的数据
     */
    private static byte[] decrypt(Cipher cipher, Key key, byte[] encryptedData) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
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
            return decryptedData;
        } catch(Exception ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    /**
     * <p>
     * 用私钥对信息生成数字签名
     * </p>
     *
     * @param data 已加密数据
     * @param privateKey 私钥(BASE64编码)
     *
     * @return
     * @throws GeneralSecurityException
     */
    public static String sign(byte[] data, String privateKey) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateK);
        signature.update(data);
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * <p>
     * 校验数字签名
     * </p>
     *
     * @param data 已加密数据
     * @param publicKey 公钥(BASE64编码)
     * @param sign 数字签名
     *
     * @return
     * @throws GeneralSecurityException
     *
     */
    public static boolean verify(byte[] data, String publicKey, String sign)
            throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PublicKey publicK = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicK);
        signature.update(data);
        return signature.verify(Base64.getDecoder().decode(sign));
    }

    /**
     * <p>
     * 生成密钥对(公钥 模数,公钥和私钥)
     * </p>
     *
     * @param crypt 私钥加密器
     * @return
     */
    public static Map<String, String> generate(Crypt crypt) {
        Map<String, String> keyMap = new HashMap<>();
        RSAKeyPair keyPair = new RSAKeyPair();
        String modulus = keyPair.getModulus();
        keyMap.put(MODULUS_NAME, modulus);
        keyMap.put(EXPONENT_NAME, keyPair.getPublicExponent());
        keyMap.put(PUBLIC_KEY_NAME, keyPair.getEncodedPublicKey());
        if(null != crypt) {
            keyMap.put(PRIVATE_EXPONENT_NAME, keyPair.getPrivateExponent(crypt));
            keyMap.put(PRIVATE_KEY_NAME, keyPair.getEncodedPrivateKey(crypt));
        } else {
            keyMap.put(PRIVATE_KEY_NAME, keyPair.getEncodedPrivateKey());
        }
        return keyMap;
    }

    /**
     * 生成密钥对
     *
     * @return 密钥对
     */
    public static RSAKeyPair generateRSAKeyPair() {
        return new RSAKeyPair();
    }

    static void applySpecKey(String token) {
        if (null == token) {
            KEY_BYTES = null;
            IV_BYTES = null;
            return;
        }
        String[] values = token.split(DELIM);
        String[] keys = new String[2];
        switch(values.length) {
        case 2:
            keys[1] = values[1];
        case 1:
            keys[0] = values[0];
            if(keys[1] == null) {
                keys[1] = values[0];
            }
            break;
        }
        KEY_BYTES = Codec.digest(keys[0], MD5_ALGORITHM);
        IV_BYTES  = Codec.digest(keys[1], MD5_ALGORITHM);
    }

    static void applyTinyKey(String token) {
        if (null == token) {
            TINY_KEY = null;
            return;
        }
        TINY_KEY = new TinyKey(token);
    }

    static void applyRsaKey(String token) {
        if (null == token) {
            RSA_KEY = null;
            return;
        }
        RSA_KEY = new RSAKeyPair(token, null);
    }

    /**
     * 设置适用密钥对的密钥。
     * 默认为AES和DES的加密的密钥。
     * 默认符记：‘{key}:{iv}'
     * 符记：‘{algorithm}:{common}:{public}:{private}'
     * RSA符记：‘RSA:{common}:{public}:{private}'	 *
     */
    public static boolean apply(String token) {
        int pos = token.indexOf(":");
        String type = "";
        if(pos != -1) {
            type = token.substring(0, pos).trim().toUpperCase();
        }
        try {
            switch(type) {
            case "TINY":
                applyTinyKey(token.substring(pos+1));
                break;
            case "RSA":
                applyRsaKey(token.substring(pos+1));
                break;
            case "AES":
            case "DES":
                applySpecKey(token.substring(pos+1));
                break;
            default:
                applySpecKey(token);
                break;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成RSA密钥对
     *
     * @return 密钥对
     */
    void initializeRSACipher() {
        try {
            setupRSAKey();
            this.rsaKey = RSA_KEY;
            this.cipher  = Cipher.getInstance(RSA_TRANSFORMATION);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }


    /**
     * 生成自定义Tiny的密钥。
     *
     */
    void initializTinyCipher() {
        setupTinyKey();
        this.tiny = true;
        this.tinyKey = TINY_KEY;
    }

    /**
     * 生成AES密钥
     *
     * @return 密钥对
     */
    void initializeAESCipher() {
        try {
            setupSpecKey();
            this.keySpec = new SecretKeySpec(specKey, AES_ALGORITHM);
            this.ivSpec  = new IvParameterSpec(specIv);
            this.cipher  = Cipher.getInstance(AES_TRANSFORMATION);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    /**
     * 生成AES密钥
     *
     * @return 密钥对
     */
    void initializeDESCipher() {
        try {
            setupSpecKey();
            byte[] keyBytes = new byte[8];
            byte[] ivBytes = new byte[8];
            System.arraycopy(specKey, 0, keyBytes, 0, keyBytes.length);
            System.arraycopy(specIv, 0, ivBytes, 0, ivBytes.length);
            this.keySpec = new SecretKeySpec(keyBytes, DES_ALGORITHM);
            this.ivSpec  = new IvParameterSpec(ivBytes);
            this.cipher  = Cipher.getInstance(DES_TRANSFORMATION);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    private void setupSpecKey() {
        if(KEY_BYTES == null || IV_BYTES == null) {
            //从环境变量定义获取密钥,没有时用内置缺省密钥
            String tokenKey = getSecretKey(EVN_SECRET_KEY_FILE);
            if(null == tokenKey) {
                LOGGER.warning("\r\n!!!There are security risks when using built-in secret keys!!!\r\nEnvironment variable 'SECRET_KEY_FILE' should be defined.");
                tokenKey = new String(SECRET_KEY);
            }
            applySpecKey(tokenKey);
        }
        specKey = new byte[KEY_BYTES.length];
        specIv  = new byte[IV_BYTES.length];
        System.arraycopy(KEY_BYTES, 0, specKey, 0, specKey.length);
        System.arraycopy(IV_BYTES, 0, specIv, 0, specIv.length);
    }

    private void setupTinyKey() {
        if (TINY_KEY == null) {
            String tokenKey = getSecretKey(EVN_TINY_KEY_FILE);
            if(null == tokenKey) {
                LOGGER.warning("\r\n!!!There are security risks when using built-in secret keys!!!\r\nEnvironment variable 'TINY_KEY_FILE' should be defined.");
                tokenKey = TINY_KEY_VALUE256;
            }
            applyTinyKey(tokenKey);
        }
    }

    private void setupRSAKey() {
        if (RSA_KEY == null) {
            //从环境变量定义获取密钥,没有时用内置缺省密钥
             String tokenKey = getSecretKey(EVN_RSA_KEY_FILE);
             if(null == tokenKey) {
                LOGGER.warning("\r\n!!!There are security risks when using built-in secret keys!!!\r\nEnvironment variable 'RSA_KEY_FILE' should be defined.");
                 tokenKey = String.format("%1$s%4$s%2$s%4$s%3$s", RSA_KEY_VALUES[0], RSA_KEY_VALUES[1], RSA_KEY_VALUES[2], DELIM);
             }
             applyRsaKey(tokenKey);
        }
    }

    private static RSAPublicKey decodePublicKey(final KeyFactory factory, String modulus, String publicExponent) {
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                new BigInteger(Base64.getDecoder().decode(modulus)),
                new BigInteger(Base64.getDecoder().decode(publicExponent)));
        try {
            return (RSAPublicKey)factory.generatePublic(publicKeySpec);
        } catch (GeneralSecurityException e) {
            throw new SecurityException(e.getMessage(), e);
        }
    }

    private static RSAPrivateKey decodePrivateKey(final KeyFactory factory, String modulus, String privateExponent, Crypt crypt) {
        final BigInteger exponent;
        if(null != crypt) {
            exponent = new BigInteger(crypt.decrypt(Base64.getDecoder().decode(privateExponent)));
        } else {
            exponent = new BigInteger(Base64.getDecoder().decode(privateExponent));
        }
        RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(
                new BigInteger(Base64.getDecoder().decode(modulus)), exponent);
        try {
            return (RSAPrivateKey)factory.generatePrivate(privateKeySpec);
        } catch (GeneralSecurityException e) {
            throw new SecurityException(e.getMessage(), e);
        }
    }

    /** 从环境变量定义的文件里获取密钥  **/
    private static String getSecretKey(String env) {
        try {
            return loadSecretKey(env);
        } catch (IOException ex) {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        String cmd = null;
        String type = "DES";
        String key = null;
        String target = null;
        Algorithm algorithm = Algorithm.DES;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("--help")) {
                usage();
                return;
            } else
            if (args[i].equals("-c") || args[i].equals("--create")) {
                cmd = "create";
            } else
            if (args[i].equals("-e") || args[i].equals("--encrypt")) {
                cmd = "encrypt";
            } else
            if (args[i].equals("-d") || args[i].equals("--decrypt")) {
                cmd = "decrypt";
            } else
            if (args[i].equals("-t") || args[i].equals("--type")) {
                if(args.length < (i+1)) {
                    usage();
                    return;
                }
                type = args[i+1].toUpperCase();
            } else
            if (args[i].equals("-k") || args[i].equals("--key")) {
                if(args.length < (i+1)) {
                    usage();
                    return;
                }
                key = args[i+1];
            }
        }
        if (args.length > 0) {
            target = args[args.length-1];
        }
        if(cmd == null) {
            usage();
            return;
        }
        if (key != null) {
            apply(key);
            int pos = key.indexOf(":");
            if(pos != -1) {
                try {
                    algorithm = Algorithm.valueOf(key.substring(0, pos).trim().toUpperCase());
                } catch (Exception e) {
                    algorithm = Algorithm.DES;
                }
            }
        }
        switch(cmd) {
        case "create":
            System.out.println(generateToken(type));
            break;
        case "encrypt":
            if(target == null) {
                usage();
            } else {
                System.out.println(Crypt.create(algorithm).encrypt(target));
            }
            break;
        case "decrypt":
            if(target == null) {
                usage();
            } else {
                System.out.println(Crypt.create(algorithm).decrypt(target));
            }
            break;
        default:
            usage();
            break;
        }
    }

    static String generateToken(String type) {
        String token = "";
        switch(type) {
        case "AES":
        case "DES":
            RandomString gen = new RandomString(8, ThreadLocalRandom.current());
            token = String.format("%s:%s:%s", type, gen.next(), gen.next());
            break;
        case "TINY":
            TinyKey tinyKey = new TinyKey();
            token = "TINY:" + tinyKey.toString();
            break;
        case "RSA":
            RSAKeyPair rsaKey = new RSAKeyPair();
            token = "RSA:" + rsaKey.toString();
            break;
        default:
            usage();
            break;
        }
        return token;
    }

    static void usage() {
        System.out.println();
        System.out.println("Usage:");
        System.out.println("java " + Crypt.class.getName());
        System.out.println("     -c --create  Create key pair.");
        System.out.println("     -t --type    The key type: 'AES', 'DEC', 'RSA' or 'Tiny'.");
        System.out.println("                  Default: 'AES' or 'DEC'.");
        System.out.println("     -k --key     A applyied spec key string.");
        System.out.println("                  Default: Embedded key.");
        System.out.println("     -e --encrypt  Encrypt input string.");
        System.out.println("     -d --decrypt Decrypt input string.");
        System.out.println("     -h --help    This help message.");
        System.out.println();
    }


    /** 加载环境变量定义的文件里的密钥  **/
    private static String loadSecretKey(String env) throws IOException {
        String line = null;
        String filename = System.getenv(env);
        if(null == filename) {
            filename = System.getProperty(env);
        }
        if(null == filename) {
            return null;
        }
        final File file = new File(filename);
        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(new FileReader(file));
            line = reader.readLine();
            if(null != line) {
                line = line.trim();
                if(line.isEmpty()) {
                    line = null;
                }
            }
            LOGGER.config(String.format("Load secret key from '%1$s'", filename));
        } finally {
            if (null != reader) {
                reader.close();
            }
        }
        return line;
    }

    /**
     * 密钥对生成器
     *
     */
    static class RSAKeyPair {
        /** 公钥 */
        RSAPublicKey publicKey;
        /** 私钥 */
        RSAPrivateKey privateKey;
        Crypt crypt = null;
        /** 密钥对生成器 */
        final KeyFactory keyFactory;

        public RSAKeyPair(int keyLength, Crypt crypt) {
            try {
                this.keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
                generateKeys(keyLength);
            } catch (GeneralSecurityException ex) {
                throw new SecurityException(ex.getMessage(), ex);
            }
        }

        public RSAKeyPair() {
            this(RSA_KEY_SIZE, null);
        }

        public RSAKeyPair(Crypt crypt) {
            this(RSA_KEY_SIZE, crypt);
        }

        public RSAKeyPair(String modulus, String publicExponent, String privateExponent, Crypt crypt) {
            this(String.format("%1$s%4$s%2$s%4$s%3$s", modulus, publicExponent, privateExponent, DELIM), crypt);
        }

        public RSAKeyPair(String encodedKeys) {
            this(encodedKeys, null);
        }

        public RSAKeyPair(String encodedKeys, Crypt crypt) {
            if(null == encodedKeys) {
                throw new IllegalArgumentException("Null argument.");
            }
            String[] keys = encodedKeys.split(DELIM);
            if(keys.length != 3) {
                throw new IllegalArgumentException(String.format("Unknow encoded key '%1$s'", encodedKeys));
            }
            try {
                this.keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            } catch (GeneralSecurityException ex) {
                throw new SecurityException(ex.getMessage(), ex);
            }
            this.crypt = crypt;
            this.publicKey = decodePublicKey(this.keyFactory, keys[0], keys[1]);
            this.privateKey = decodePrivateKey(this.keyFactory, keys[0], keys[2], crypt);
        }

        /**
         * 生成RSA密钥对
         *
         */
        private void generateKeys(int keyLength) {
            // Transfer the public key to the other,
            // First to restore the encoded value of the public key to KeySpec based
            // openssl rsa -in ./key.pem -pubout -out ./key.x509
            // 公共密钥传输到其他地方，先恢复基于公共密钥KeySpec的编码值
            try {
                final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
                keyPairGenerator.initialize(keyLength, random);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                this.publicKey = (RSAPublicKey)keyPair.getPublic();
                this.privateKey = (RSAPrivateKey)keyPair.getPrivate();
            } catch (GeneralSecurityException ex) {
                throw new SecurityException(ex.getMessage(), ex);
            }
        }

        /** 公钥 模数*/
        public String getModulus() {
            return Base64.getEncoder().encodeToString(this.publicKey.getModulus().toByteArray());
        }

        public String getEncodedPublicKey() {
            return Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
        }
        /** 公钥 指数*/
        public String getPublicExponent() {
            return Base64.getEncoder().encodeToString(this.publicKey.getPublicExponent().toByteArray());
        }

        public String getEncodedPrivateKey() {
            return getEncodedPrivateKey(this.crypt);
        }

        public String getEncodedPrivateKey(Crypt crypt) {
            if(null != crypt) {
                //加密私钥
                return Base64.getEncoder().encodeToString(crypt.encrypt(this.privateKey.getEncoded()));
            } else {
                return Base64.getEncoder().encodeToString(this.privateKey.getEncoded());
            }
        }

        /** 私钥指数 */
        public String getPrivateExponent() {
            return getPrivateExponent(this.crypt);
        }

        /** 加密私钥指数 */
        public String getPrivateExponent(Crypt crypt) {
            if(null != crypt) {
                //加密私钥
                return Base64.getEncoder().encodeToString(
                        crypt.encrypt(this.privateKey.getPrivateExponent().toByteArray()));
            } else {
                return Base64.getEncoder().encodeToString(
                        this.privateKey.getPrivateExponent().toByteArray());
            }
        }


        public RSAPublicKey getPublicKey() {
            return this.publicKey;
        }

        public RSAPrivateKey getPrivateKey() {
            return this.privateKey;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getModulus());
            sb.append(DELIM);
            sb.append(getPublicExponent());
            sb.append(DELIM);
            sb.append(getPrivateExponent());
            return sb.toString();
        }
    }

    static class TinyKey {
        /** 密钥 */
        BigInteger commonKey;
        /** 公钥 */
        BigInteger publicKey;
        /** 私钥 */
        BigInteger privateKey;

        public TinyKey() {
            // Create 128 bit key
            this(128);
        }

        public TinyKey(int keyLength) {
            generateKeys(keyLength);
        }

        public TinyKey(String encodedKey) {
            if(null == encodedKey) {
                throw new IllegalArgumentException("Null argument.");
            }
            String[] keys = encodedKey.split(DELIM);
            if(keys.length != 3) {
                throw new IllegalArgumentException(String.format("Unknow encoded key '%1$s'", encodedKey));
            }
            this.commonKey  = new BigInteger(Codec.decodeString(keys[0]));
            this.publicKey  = new BigInteger(Codec.decodeString(keys[1]));
            this.privateKey = new BigInteger(Codec.decodeString(keys[2]));
        }

        @Override
        public boolean equals(Object obj) {
            if ( !(obj instanceof TinyKey) ) {
                return false;
            }
            TinyKey target = (TinyKey)obj;
            return (this.commonKey.equals(target.commonKey)
                    && this.publicKey.equals(target.publicKey)
                    && this.privateKey.equals(target.privateKey));
        }

        @Override
        public int hashCode() {
            int value = 17;
            value = value * 31 + this.commonKey.hashCode();
            value = value * 31 + this.publicKey.hashCode();
            value = value * 31 + this.privateKey.hashCode();
            return value;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(Codec.encodeString(commonKey.toByteArray()));
            sb.append(DELIM);
            sb.append(Codec.encodeString(publicKey.toByteArray()));
            sb.append(DELIM);
            sb.append(Codec.encodeString(privateKey.toByteArray()));
            return sb.toString();
        }


        /**
         * 生成自定义RSA的密钥。
         *
         */
        void generateKeys(int keyLength) {
            try {
                final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                for (;;) {
                    BigInteger p = BigInteger.probablePrime(keyLength >> 1, random);
                    BigInteger q = BigInteger.probablePrime(keyLength >> 1, random);
                    if (p.equals(q))
                        continue;
                    // totient function of two different prime numbers
                    BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
                    BigInteger e = BigInteger.probablePrime(keyLength, random);
                    if (e.gcd(phi).equals(BigInteger.ONE)) {
                        // private/public key (common key)
                        this.commonKey  = p.multiply(q);
                        // public key
                        this.publicKey = e;
                        // private key
                        this.privateKey= e.modInverse(phi);
                        break;
                    }
                }
            } catch (GeneralSecurityException ex) {
                throw new SecurityException(ex.getMessage(), ex);
            }
        }
    }

    // chose a Character random from this String
    private static final String ALPHA_NUMERIC_SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";
    static class RandomString {

        private final Random random;
        private final char[] symbols;
        private final char[] buf;

        public RandomString(int length, Random random, String symbols) {
            if (length < 1) throw new IllegalArgumentException();
            if (symbols.length() < 2) throw new IllegalArgumentException();
            this.random = Objects.requireNonNull(random);
            this.symbols = symbols.toCharArray();
            this.buf = new char[length];
        }

        /**
         * Create an alphanumeric string generator.
         */
        public RandomString(int length, Random random) {
            this(length, random, ALPHA_NUMERIC_SYMBOLS);
        }

        /**
         * Create an alphanumeric strings from a secure generator.
         */
        public RandomString(int length) {
            this(length, new SecureRandom());
        }

        /**
         * Create session identifiers.
         */
        public RandomString() {
            this(21);
        }

        /**
         * Generate a random string.
         */
        public String next() {
            for (int idx = 0; idx < buf.length; ++idx)
                buf[idx] = symbols[random.nextInt(symbols.length)];
            return new String(buf);
        }
    }
}
