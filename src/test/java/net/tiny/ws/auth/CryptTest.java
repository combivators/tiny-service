package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

public class CryptTest {

    static String LONG_TEXT ="W4a5bifaxGkvvLL+LEsSApTThZD+QB4OHBouZcsrE0fzY1B4WLDyr9ajyC+gasx6BoDTQQjVYcJea6/d89AwyAajZwK+j+Mh/ymLghB7HlwDtdTtTmFIm6vgsc/29qitIUfmUTjskHRp2433BWBFmFIVcESV51n5ReCXgn7iD1k=";

    @Test
    public void testTinyKey() throws Exception {
        Crypt.TinyKey tinyKey = new Crypt.TinyKey();
        String enc = tinyKey.toString();
        System.out.println(tinyKey.toString());
        Crypt.TinyKey other = new Crypt.TinyKey(enc);
        assertEquals(other, tinyKey);
        String[] tokens = enc.split(":");
        for(String token : tokens) {
            System.out.println(token);
        }
        System.out.println();

        for(int i=0; i<10; i++) {
            tinyKey = new Crypt.TinyKey();
            System.out.println(tinyKey.toString());
        }
        System.out.println();

        for(int i=0; i<10; i++) {
            tinyKey = new Crypt.TinyKey(256);
            System.out.println(tinyKey.toString());
        }
    }

    @Test
    public void testTiny() throws Exception {
        Crypt.TinyKey tinyKey = new Crypt.TinyKey();
        String tokenKey = tinyKey.toString();
        System.out.println(tokenKey);

        String data = "MyPassword";
        Crypt crypt = Crypt.create();
        String enc = crypt.encrypt(data);
        System.out.println(enc);

        crypt = Crypt.create();
        String dec = crypt.decrypt(enc);
        System.out.println(dec);
        assertEquals(data, dec);

        crypt = Crypt.create();
        int number = 100;
        for(int i=0; i<number; i++) {
            data = Codec.getRandom("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            enc = crypt.encrypt(data);
            dec = crypt.decrypt(enc);
            System.out.println("## '"+ data + "'\t" + enc);
            assertEquals(data, dec);
        }

        System.out.println();
        data = Codec.getRandom("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        enc = crypt.encrypt(data);
        dec = crypt.decrypt(enc);
        assertNotSame(data, dec); //FIXME Too short string
        System.out.println("## '"+ data + "'\t" + enc);
    }

    @Test
    public void testRSAKeyPair() throws Exception {
        Crypt.RSAKeyPair rsaKey = new Crypt.RSAKeyPair();
        String enc = rsaKey.toString();
        System.out.println(rsaKey.toString());
        Crypt.RSAKeyPair other = new Crypt.RSAKeyPair(enc);
        assertEquals(other.toString(), rsaKey.toString());
        String[] keys = enc.split(":");
        System.out.println();
        for(String key : keys) {
            System.out.println(key);
        }
        System.out.println();

        rsaKey = new Crypt.RSAKeyPair(Crypt.RSA_KEY_SIZE, null);
        enc = rsaKey.toString();
        keys = enc.split(":");
        for(String key : keys) {
            System.out.println(key);
        }
        System.out.println();

        for(int i=0; i<5; i++) {
            rsaKey = new Crypt.RSAKeyPair();
            System.out.println(rsaKey.toString());
        }

        rsaKey = new Crypt.RSAKeyPair();
        Crypt crypt = Crypt.create(Crypt.DES);
        String encrypted = Base64.getEncoder().encodeToString(
                crypt.encrypt(rsaKey.getPrivateKey().getPrivateExponent().toByteArray()));
        crypt = Crypt.create(Crypt.DES);
        BigInteger b = new BigInteger(crypt.decrypt(Base64.getDecoder().decode(encrypted)));
        assertTrue(rsaKey.getPrivateKey().getPrivateExponent().equals(b));

        String exponent = rsaKey.getPublicExponent();
        System.out.println("exponent: " + exponent);
        String publicKey = rsaKey.getEncodedPublicKey();
        System.out.println("publicKey: " + publicKey);

        crypt =  Crypt.create("DES");
        exponent = crypt.getPublicExponent();
        System.out.println("exponent: " + exponent);
        publicKey = crypt.getEncodedPublicKey();
        System.out.println("publicKey: " + publicKey);
    }

    @Test
    public void testRSA() throws Exception {
        String data = "MyPassword";
        Crypt crypt = Crypt.create("RSA");
        String enc = crypt.encrypt(data);
        System.out.println(enc);
        crypt = Crypt.create("RSA");
        String dec = crypt.decrypt(enc);
        assertEquals(dec, data);
    }

    @Test
    public void testAES() throws Exception {
        Crypt  crypt = Crypt.create(Crypt.AES);
        String data = "MyPassword";
        String enc = crypt.encrypt(data);
        System.out.println(enc);
        crypt = Crypt.create(Crypt.AES);
        String dec = crypt.decrypt(enc);
        assertEquals(dec, data);
    }

    @Test
    public void testDES() throws Exception {
        Crypt  crypt = Crypt.create(Crypt.DES);
        String data = "MyPassword";
        String enc = crypt.encrypt(data);
        System.out.println(enc);
        crypt = Crypt.create(Crypt.DES);
        String dec = crypt.decrypt(enc);
        assertEquals(dec, data);
    }

    @Test
    public void testLongText() throws Exception {
        String data = LONG_TEXT;
        Crypt  crypt = Crypt.create(Crypt.AES);
        String enc = crypt.encrypt(data);
        System.out.println(enc);
        crypt = Crypt.create(Crypt.AES);
        String dec = crypt.decrypt(enc);
        assertEquals(dec, data);

        crypt = Crypt.create(Crypt.DES);
        enc = crypt.encrypt(data);
        System.out.println(enc);
        crypt = Crypt.create(Crypt.DES);
        dec = crypt.decrypt(enc);
        assertEquals(dec, data);

        crypt = Crypt.create(Crypt.RSA);
        enc = crypt.encrypt(data);
        System.out.println(enc);
        crypt = Crypt.create(Crypt.RSA);
        dec = crypt.decrypt(enc);
        assertEquals(dec, data);
    }

    @Test
    public void testStaticCrypt() throws Exception {
        String data = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJs+ZuAtfP23CcAfQbNqifeTwA5FzmFsYlaln3+nuasmHZbnuX0ByczEY8neYaH2HjBXjrtWTJb4jONqRHPtdNOf0om8Sm9ed/fUJHH7oXXdv2P3bT7mU1uMiKaGtSxz+vmdwbZcAV8cgVbFQ7euLX0fSEEJEQFuDGkR6XavRMndAgMBAAECgYEAj0C+96CiFREhGzr8io4GMAIUGFeMANRdzizZCJgCOY9bgJPl0xeiWqTinDXsC+MrqloxaGdTF4DVqUi3T+5PfT9ctoJ6UoN94Y6re5s1DQkp1Q+n5tUueCsBRJWLIt56K3VdhKDTzAvLG27p0vMrB966fZAhkX7VXx28isR0AcECQQDsVE0xxpz6syfDtrEJZPMBPRekIFf+79jiH2zBDO0de1w8BnreKGDHmcFybFYwHdYhKnsJeWAh7C/0kJ9WDL2tAkEAqCpU6RjVSCD9TkiNM5OZTOdtYDQFiwdKJ8C9Sd0xp1sWT+9viFO+J5AKa8gwSUlXcbjjPh2NPS3d5Nk8IJJi8QJAXwp4EPCC6P9rmnW6NMD4SSM8grDPMqNaYXWp0ulT4mtd6HXiq70pTpwzA8U11BvrpWLkICdD1eCaWIxgx8ZP8QJAKveN9HT04dYUArGE6n22+LBVAPSpyekV6GxVsQ7ERhd+7vOlkraa6m7iSsG+nKsRnav43AEe+lfCz1s2AriEcQJANN2nOLs2eehFf8FF8jP4B7EyG8G8SSYMLh6gABdRKoi/MBzy4ThECWa0BYHCClviqktqJAtlo4BC3lwdIEzNiA==";
        Crypt des = Crypt.create(Crypt.DES);
        Map<String, String> keys = Crypt.generate(des);
        assertEquals(5, keys.size());
        String modulus = keys.get("modulus");
        String publicKey = keys.get("publicKey");
        System.out.print("modulus:\r\n\t");
        System.out.println(modulus);
        System.out.print("public:\r\n\t");
        System.out.println(publicKey);
        System.out.print("private:\r\n\t");
        System.out.println(keys.get("privateKey"));
        System.out.print("privateExponent:\r\n\t");
        System.out.println(keys.get("privateExponent"));
        System.out.print("privateModulus:\r\n\t");
        System.out.println(keys.get("privateModulus"));
        System.out.println("---------------------------");

        byte[] encryptedData = Crypt.encryptByPublicKey(publicKey, data.getBytes());
        des = Crypt.create(Crypt.DES);
        String privateKey = keys.get("privateKey");
        //解密私钥并BASE64编码
        privateKey = Base64.getEncoder().encodeToString(des.decrypt(Base64.getDecoder().decode(privateKey)));
        String decryptedData = new String(Crypt.decryptByPrivateKey(privateKey, encryptedData));
        assertEquals(decryptedData, data);

        String privateExponent = keys.get("privateExponent");
        decryptedData = new String(Crypt.decryptByPrivateKey(modulus, privateExponent, des, encryptedData));
        assertEquals(decryptedData, data);

        //
        String encrypted = Crypt.encryptByPrivateKey(privateKey, data);
        System.out.println("## " + encrypted);
        String decrypted = Crypt.decryptByPublicKey(publicKey, encrypted);
        System.out.println("## " + decrypted);
        assertEquals(data, decrypted);
    }

    @Test
    public void testApplyEnvSpecKey() throws Exception {
        final String data = "MyPassword";
        assertTrue(Crypt.apply("Hoge:Fuga"));
        Crypt des1 = Crypt.create(Crypt.DES);
        String enc1 = des1.encrypt(data);
        System.out.println(enc1);
        Crypt crypt1 = Crypt.create(Crypt.DES);
        String dec1 = crypt1.decrypt(enc1);
        assertEquals(dec1, data);

        assertTrue(Crypt.apply("_0P1a2S3s4W5o6R7d8_"));
        Crypt des2 = Crypt.create(Crypt.DES);
        String enc2 = des2.encrypt(data);
        System.out.println(enc2);
        Crypt crypt2 = Crypt.create(Crypt.DES);
        String dec2 = crypt2.decrypt(enc2);
        assertEquals(dec2, data);

        assertNotEquals(enc1, enc2);

        try {
            String f = crypt2.decrypt(enc1);
            fail(f);
        } catch (Exception e) {
            assertTrue(e instanceof SecurityException);
        }

        String tokenKey = "tiny:12P87eBgVjyxnEeiQE4dHhk1:12RZpjew51y1pphPmMtjSPdj:AxryWpThpEYo2qBcD21aqB";
        assertTrue(Crypt.apply(tokenKey));

        tokenKey = "rsa:AKJAqifUYvIcSuXNNWd98UiqzNndNV4TVt9g+GZbsVV7/SCjnH8QHton3ybeWhmuKqlJfqMPk0GvHWh2EH0qTvzgpghJKqCS2mgnm01MTPbIrbI4pLvz1/fjR62F4iQOBAN6YPyvKhKwWOy2qxGI4lsljB+SGewNsARALwLQEikT:AQAB:A3+La6Jro7aycrPy89FGU3/DPOtFDEs0c0p+8I4Hi8VJltuQMzkgwSlc6VSf8q/LoazA+zkJvr/MzYTGJDy2STqwZ+iAy4kgKonEGx325w8ZYH1lbWIykCOVutUKvpmY+LF23nVgvzFsHg89mzy4sNU7NTdTbGMUCehiHy+wWyk=";
        assertTrue(Crypt.apply(tokenKey));

        tokenKey = "AES:Hoge:Fuga";
        assertTrue(Crypt.apply(tokenKey));
        tokenKey = "DES:Fuga:Hoge";
        assertTrue(Crypt.apply(tokenKey));
    }

    @Test
    public void testRandomString() {
        Crypt.RandomString gen = new Crypt.RandomString(8, ThreadLocalRandom.current());
        String s1 = gen.next();
        String s2 = gen.next();
        assertEquals(8, s1.length());
        assertEquals(8, s2.length());
        assertNotEquals(s1, s2);
    }

    @Test
    public void testEnDecryptPassword() throws Exception {
        String password = "Password";
        String encoded = Crypt.encryptPassword(password);
        assertEquals(password, Crypt.decryptPassword("[" + encoded + "]"));
    }

    @Test
    public void testMain() throws Exception {
        String[] args = {"-h"};
        Crypt.main(args);

        args = new String[] {"-c", "-t", "DES"};
        Crypt.main(args);
        args = new String[] {"-c", "-t", "tiny"};
        Crypt.main(args);
        args = new String[] {"-c", "-t", "rsa"};
        Crypt.main(args);

        args = new String[] {"-k", "DES:CAhQn4bV:HIOsSQIg", "-e", "password"};
        Crypt.main(args);

        args = new String[] {"-k", "DES:CAhQn4bV:HIOsSQIg", "-d", "Piz5wX49L4MS4SYsGwEMNw=="};
        Crypt.main(args);

        args = new String[] {"-k", "_0P1a2S3s4W5o6R7d8_", "-e", "password"};
        Crypt.main(args);

        args = new String[] {"-k", "TINY:AmyPOFeaySrep1ajXu0wAt:12Svx5wnqSs5cNL7gwPFmoB3:8BzyPruY1ygBnXMj9TDvht", "-e", "password"};
        Crypt.main(args);

        args = new String[] {"-k", "TINY:AmyPOFeaySrep1ajXu0wAt:12Svx5wnqSs5cNL7gwPFmoB3:8BzyPruY1ygBnXMj9TDvht", "-d", "F72NDOI7YGVf5VfbddHaJA=="};
        Crypt.main(args);

        args = new String[] {"-k", "TINY:1QUCL0fLuCDXt5Olcl574yfC5FldFjhavrCxnszrMkSP:86GnTBm1cMhtXmuSVH03qXigBMgEXRQbfzvaxhRaM9hMb:1J6jFKd6j4D4mTXoOMYFmuJP7oPlPlq5t3MJAyFgeGRR", "-e", "password"};
        Crypt.main(args);
    }

    @Test
    public void testBenchmarkTiny() throws Exception {
        Crypt.applyTinyKey(null);
        List<String> tokens = new ArrayList<>();
        int number = 10000;
        for(int i=0; i<number; i++) {
            tokens.add(Codec.getRandom("********************"));
        }
        System.out.print("Tiny测试开始，循环次数：" + tokens.size()); //key:256bit 10000: 12s
        long currTime = System.currentTimeMillis();
        for(String data : tokens) {
            Crypt  crypt = Crypt.create(Crypt.Algorithm.TINY);
            String enc = crypt.encrypt(data);
            String dec = crypt.decrypt(enc);
            assertEquals(dec, data);
        }
        System.out.println(" 执行耗时" + (System.currentTimeMillis() - currTime) + "ms");
    }

    @Test
    public void testBenchmarkAES() throws Exception {
        Crypt.applySpecKey(null);
        List<String> tokens = new ArrayList<>();
        int number = 10000;
        for(int i=0; i<number; i++) {
            tokens.add(Codec.getRandom("********************"));
        }
        System.out.print("AES测试开始，循环次数：" + tokens.size()); //10000 : 2.0s
        long currTime = System.currentTimeMillis();
        for(String data : tokens) {
            Crypt  crypt = Crypt.create(Crypt.AES);
            String enc = crypt.encrypt(data);
            String dec = crypt.decrypt(enc);
            assertEquals(dec, data);
        }
        System.out.println(" 执行耗时" + (System.currentTimeMillis() - currTime) + "ms");
    }

    @Test
    public void testBenchmarkDES() throws Exception {
        Crypt.applySpecKey(null);
        List<String> tokens = new ArrayList<>();
        int number = 10000;
        for(int i=0; i<number; i++) {
            tokens.add(Codec.getRandom("********************"));
        }
        System.out.print("DES测试开始，循环次数：" + tokens.size()); //10000 : 1.0s
        long currTime = System.currentTimeMillis();
        for(String data : tokens) {
            Crypt  crypt = Crypt.create(Crypt.DES);
            String enc = crypt.encrypt(data);
            String dec = crypt.decrypt(enc);
            assertEquals(dec, data);
        }
        System.out.println(" 执行耗时" + (System.currentTimeMillis() - currTime) + "ms");
    }

    @Test
    public void testBenchmarkRSA() throws Exception {
        Crypt.applyRsaKey(null);
        List<String> tokens = new ArrayList<>();
        int number = 10000;
        for(int i=0; i<number; i++) {
            tokens.add(Codec.getRandom("********************"));
        }
        System.out.print("RSA测试开始，循环次数：" + tokens.size()); //10000 : 41.0s
        long currTime = System.currentTimeMillis();
        for(String data : tokens) {
            Crypt  crypt = Crypt.create(Crypt.RSA);
            String enc = crypt.encrypt(data);
            String dec = crypt.decrypt(enc);
            assertEquals(dec, data);
        }
        System.out.println(" 执行耗时" + (System.currentTimeMillis() - currTime) + "ms");
    }
}
