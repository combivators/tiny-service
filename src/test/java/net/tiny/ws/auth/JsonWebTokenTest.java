package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tiny.config.JsonParser;

public class JsonWebTokenTest {

    @Test
    public void testAlgorithm() throws Exception {
        assertEquals("HS256", JsonWebToken.Algorithm.HS256.name());
        assertEquals("HmacSHA256", JsonWebToken.Algorithm.HS256.type);
        assertNotNull(JsonWebToken.Algorithm.valueOf("HS256"));
    }

    static final String PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuA9BNNJckSivg1J1tRotBVPFclNBfx13wIEMEGifOuLEcQpOoHLSwgN+zB5ZL78x9zYoqVxUodwt4QFPv/mgYwy5ZpU40/NeJSnMa2ZYoKba4PdlwD6VWNrH2MtoL9jFSOE8jDYEI8Zb7bwIatfQKxF2APS/ZyKhQmenXyCxY/E1qv6BzgjQZohLMraYXZJ7nAyvSW2bmNgWBkf6LyGwVFM4KzM222Pz7tXTT0iFP385nMisZ3Po43xUP5y8v/klF73M6El+fcE0U6O+V8eq9U2a5DgsiEL6IyoGI1+vy51SDvb9bUqGE8Yma3sTg1YUTtqjapPhXq0XpLW7tf9+2QIDAQAB\n-----END PUBLIC KEY-----";
    static final String PRIVATE_KEY="-----BEGIN RSA PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC4D0E00lyRKK+DUnW1Gi0FU8VyU0F/HXfAgQwQaJ864sRxCk6gctLCA37MHlkvvzH3NiipXFSh3C3hAU+/+aBjDLlmlTjT814lKcxrZligptrg92XAPpVY2sfYy2gv2MVI4TyMNgQjxlvtvAhq19ArEXYA9L9nIqFCZ6dfILFj8TWq/oHOCNBmiEsytphdknucDK9JbZuY2BYGR/ovIbBUUzgrMzbbY/Pu1dNPSIU/fzmcyKxnc+jjfFQ/nLy/+SUXvczoSX59wTRTo75Xx6r1TZrkOCyIQvojKgYjX6/LnVIO9v1tSoYTxiZrexODVhRO2qNqk+FerRektbu1/37ZAgMBAAECggEBAK9pimFG4gVNWweyfI9eJO3gylmMUu1MLiZ+VfsFWksKduCsTAbJp2ZTYnIxshm5A2twaAwP/HBNoEPBtjllM59yLvc+22vTkjOkxDbO0UQ6AHtKC+TNQBPwXWmVYPPIiSxNzCBsEkHt9wp4myUIDFIfT/DVT9yAumLI2k/knmp5hrJg3y7hgDIJCxRGq6fxfrJ/TT61ExAQNg3M4kznruyIleuLLecgblSqVJvGO3VjeStxa8uUUhURLUjNpAmgg5919i3lLH5zeq9j6Rs/PDs1g0y/8asjJ4EZeUUf8FNCYww9FuP19Auwn6YMfBjlHYMDB1FjYoBV26li55Q3m8kCgYEA5Nno7eqimVn80kf9vjZRroFQ9TrUv42YUtHphwDEEecsDg1rMKoYw02oPeJiZpC2B6PKAd87DtT9dGMAG3LQvjSnfEWZN0NEFxpQnp5pK77dHiz7R6YzQXBtQBNi2U6fctyoi0HRakTAYFXbmYJX5FbFl3Qmb8mdf/W3+6OSmYcCgYEAzeUMB4X6SD/QreiK5a9JO9pROc6U2haY7z3VTiEV9xLHB80QbUZkifQPc99fqMV28GnUwWH2vYHYhV6aqMOeJVLQDjx8FUY+ewQpUqJTFHZ+67/82nnZ71uvHXhUKwyAp1YMCIhMV4237Luja0JUjf51KUJAaDKAM6LU/QH1vJ8CgYAizd016bxG1D+1/0rg1cQAZRxZ31OhujgTSGdap8wp6N1zWakWopfXhAT1PGu4q4Nzj/5V4IxLyiqtu6y1f/WtJ7bdGHyfwfQmC1N9fBwEkidwwmiI0gbzidSjrQ8Ye3OWdWaQnzbpEYGsZQJby03wpR2x1fFOiPwSDN2pQIw4GwKBgC9zmlIhjo0YJ//smBZXT4l7xzyLX8Ljcluw5HgdJ/LJYRVrLV4B2ynwFZ+e220KFV9TBLWM3lDOnBggtYQvkUMI0up1BiPhDIVNcDibMIqGxLmQhbXUX2XKu5EPKlbBiuUF78AHqZCEGN92XzNZFiOjgV0A59cbzHAaFvw4d2oBAoGALAPEDU5U+RfgpFuduz2OG+d3bjY6EmBvTzYKGzMWcRsPg9k4bwC7MB2lBAM/ad+EUvRlI/BLy3exsK1afSptqJV3xmUoOSiitJT2jM92Yt15DHqbqZ2lTAQJjeAzd2iFpUSRK3KWYstdg6CZ6j9VSbCPPxQIZZQpCE9+AgpZiAI=\n-----END RSA PRIVATE KEY-----";

    static final String TEST_PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDA2kMLtgwn8V8PSysPT7nsRD+i\nvNq+Ai1e8miRNTPZwGc7fPBRZtLzO5jWdjLmwdLdA8pC2KFM6veNTlZ/tPQ+Y3Wl\n5LknE5MInEScLiOMYGCxe9AeA5ew6KQLGHqtHsl5IY9+U6D2eu4AE16HalXqpIBP\nGOwShAhzMPUYRpuhSQIDAQAB\n-----END PUBLIC KEY-----\n";
    static final String TEST_JWT="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InQtaWdvIiwic2NtQ29udGV4dCI6ImdpdGh1YjpwYXJ0bmVyLmdpdC5jb3JwLnlhaG9vLmNvLmpwIiwic2NvcGUiOlsidXNlciJdLCJpYXQiOjE1Nzk3NjY3ODEsImV4cCI6MTU3OTc3Mzk4MSwianRpIjoiMmYyZDA5OTctZDA0OC00ZDI4LTk2MjAtYmVjMzQ0NWUzNDMxIn0.FrtgzIMHdhcdsBeg4eeYprynYSbC26f4Z2J4tyx_capYkSj1V-PFO8ki7dg377ajGM0mSG7WOtHozhYR61AHFTa9s7gq8Exk572FkuWZiqODiB1kMlVZbkJkxeEMzGlRYjI_eyBNREg2eTuqDfd4XjCOJykeevjJ1ncounp9btM";

    @Test
    public void testDecodeToken() throws Exception {
        String token = TEST_JWT;
        JsonWebToken jwt = JsonWebToken.valueOf(token);
        assertNotNull(jwt);
        jwt.print(System.out);
        assertTrue(jwt.expired());
    }

    @Test
    public void testHMACToken() throws Exception {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid", "-Je9A7op-mEPuhrDPN9T");
        payload.put("id", "-Je9A7op-mEPuhrDPN9T");
        payload.put("request", "hoge");

        String secret = "OPeJAbqF07aWFw5SXvV7sUZnT3SbHVxzeZwqvJHV";

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("HS256", secret)
                .notBefore(new Date())
                .subject("oauth")
                .issuer("net.tiny")
                .audience("user@net.tiny")
                .jti(true)
                .build(payload);
        String token = jwt.token();

        assertTrue(token.indexOf('=') < 0);
        assertTrue(token.indexOf('+') < 0);
        assertTrue(token.indexOf('/') < 0);

        System.out.println(token);
        String[] tokenFragments = token.split("\\.");
        assertEquals(3, tokenFragments.length);
        assertEquals("HS256", jwt.algorithm());

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"HS256\"}", jwt.header());
        System.out.println(jwt.header());
        assertTrue(jwt.claims().startsWith("{\"v\":1,"));
        assertTrue(jwt.claims().contains("\"iat\""));
        assertTrue(jwt.claims().contains("\"exp\""));
        assertTrue(jwt.claims().contains("\"nbf\""));
        assertTrue(jwt.claims().contains("\"sub\""));
        assertTrue(jwt.claims().contains("\"iss\""));
        assertTrue(jwt.claims().contains("\"aud\""));
        assertTrue(jwt.claims().contains("\"jti\""));
        assertTrue(jwt.claims().contains("\"uid\""));
        System.out.println(jwt.claims());


        Map<?,?> jsonHeader = JsonWebToken.mapper(jwt.header());
        assertEquals("HS256", jsonHeader.get("alg"));
        assertEquals("JWT", jsonHeader.get("typ"));

        Map<?,?> jsonClaims = JsonWebToken.mapper(jwt.claims());
        assertEquals(1.0d, jsonClaims.get("v"));
        assertNotNull(jsonClaims.get("exp"));
        assertNotNull(jsonClaims.get("nbf"));
        assertNotNull(jsonClaims.get("iat"));

        Map<?,?> jsonData = (Map<?,?>)jsonClaims.get("d");
        assertEquals("-Je9A7op-mEPuhrDPN9T", jsonData.get("uid"));
        assertEquals("-Je9A7op-mEPuhrDPN9T", jsonData.get("id"));
        assertEquals("hoge", jsonData.get("request"));

        JsonWebToken other = JsonWebToken.valueOf(token);
        assertTrue(other.verify(secret));
    }

    @Test
    public void testRSAToken() throws Exception {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid", "-Je9A7op-mEPuhrDPN9T");
        payload.put("id", "-Je9A7op-mEPuhrDPN9T");
        payload.put("request", "hoge");

        String secret = PRIVATE_KEY;

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("RS256", secret)
                .notBefore(new Date())
                .subject("oauth")
                .issuer("net.tiny")
                .audience("user@net.tiny")
                .jti(true)
                .build(payload);
        String token = jwt.token();

        assertTrue(token.indexOf('=') < 0);
        assertTrue(token.indexOf('+') < 0);
        assertTrue(token.indexOf('/') < 0);

        System.out.println(token);
        String[] tokenFragments = token.split("\\.");
        assertEquals(3, tokenFragments.length);
        assertEquals("RS256", jwt.algorithm());

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"RS256\"}", jwt.header());
        System.out.println(jwt.header());
        assertTrue(jwt.claims().startsWith("{\"v\":1,"));
        assertTrue(jwt.claims().contains("\"iat\""));
        assertTrue(jwt.claims().contains("\"exp\""));
        assertTrue(jwt.claims().contains("\"nbf\""));
        assertTrue(jwt.claims().contains("\"sub\""));
        assertTrue(jwt.claims().contains("\"iss\""));
        assertTrue(jwt.claims().contains("\"aud\""));
        assertTrue(jwt.claims().contains("\"jti\""));
        assertTrue(jwt.claims().contains("\"uid\""));
        System.out.println(jwt.claims());


        Map<?,?> jsonHeader = JsonWebToken.mapper(jwt.header());
        assertEquals("RS256", jsonHeader.get("alg"));
        assertEquals("JWT", jsonHeader.get("typ"));

        Map<?,?> jsonClaims = JsonWebToken.mapper(jwt.claims());
        assertEquals(1.0d, jsonClaims.get("v"));
        assertNotNull(jsonClaims.get("exp"));
        assertNotNull(jsonClaims.get("nbf"));
        assertNotNull(jsonClaims.get("iat"));

        Map<?,?> jsonData = (Map<?,?>)jsonClaims.get("d");
        assertEquals("-Je9A7op-mEPuhrDPN9T", jsonData.get("uid"));
        assertEquals("-Je9A7op-mEPuhrDPN9T", jsonData.get("id"));
        assertEquals("hoge", jsonData.get("request"));

        JsonWebToken other = JsonWebToken.valueOf(token);
        String publicKey = PUBLIC_KEY;
        assertTrue(other.verify(publicKey));
    }

    @Test
    public void testEllipticCurveSigner() throws Exception {
        KeyPair keyPair = JsonWebToken.generateKeyPair("ES256");
        PublicKey publicKey = (PublicKey) keyPair.getPublic();
        PrivateKey privateKey = (PrivateKey) keyPair.getPrivate();
        String encodedPublicKey = Keys.encodeKey(publicKey);
        System.out.println(encodedPublicKey);
        String encodedPrivateKey =  Keys.encodeKey(privateKey);
        System.out.println(encodedPrivateKey);

        String plaintext = "1234567890abcdefghijklmnopqrst";
        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
        ecdsaSign.initSign(privateKey);
        ecdsaSign.update(plaintext.getBytes("UTF-8"));
        byte[] signature = ecdsaSign.sign();
        String pub = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String sig = Base64.getEncoder().encodeToString(signature);
        System.out.println(pub);
        System.out.println(sig);


        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pub));
        PublicKey verifyPublicKey = keyFactory.generatePublic(publicKeySpec);
        ecdsaVerify.initVerify(verifyPublicKey);
        ecdsaVerify.update(plaintext.getBytes("UTF-8"));
        boolean result = ecdsaVerify.verify(Base64.getDecoder().decode(sig));
        System.out.println("verify " + result);
        assertTrue(result);


        JsonWebToken.EllipticCurveSigner signer = new JsonWebToken.EllipticCurveSigner(JsonWebToken.Algorithm.ES256, privateKey);
        String s = signer.sign(plaintext);
        System.out.println(s);

        JsonWebToken.EllipticCurveSigner signer2 = new JsonWebToken.EllipticCurveSigner(JsonWebToken.Algorithm.ES256, publicKey);
        boolean ret = signer2.verify(plaintext, s);
        System.out.println("verify " + ret);
        assertTrue(ret);

    }


    @Test
    public void testECDSAToken() throws Exception {
        KeyPair keyPair = JsonWebToken.generateKeyPair("ES256");
        PublicKey publicKey = (PublicKey) keyPair.getPublic();
        PrivateKey privateKey = (PrivateKey) keyPair.getPrivate();
        String encodedPublicKey = Keys.encodeKey(publicKey);
        System.out.println(encodedPublicKey);
        String encodedPrivateKey =  Keys.encodeKey(privateKey);
        System.out.println(encodedPrivateKey);

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid", "-Je9A7op-mEPuhrDPN9T");
        payload.put("id", "-Je9A7op-mEPuhrDPN9T");
        payload.put("request", "hoge");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("ES256", encodedPrivateKey)
                .notBefore(new Date())
                .subject("oauth")
                .issuer("net.tiny")
                .audience("user@net.tiny")
                .jti(true)
                .build(payload);
        String token = jwt.token();

        assertTrue(token.indexOf('=') < 0);
        assertTrue(token.indexOf('+') < 0);
        assertTrue(token.indexOf('/') < 0);

        System.out.println(token);
        String[] tokenFragments = token.split("\\.");
        assertEquals(3, tokenFragments.length);
        assertEquals("ES256", jwt.algorithm());

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"ES256\"}", jwt.header());
        System.out.println(jwt.header());
        assertTrue(jwt.claims().startsWith("{\"v\":1,"));
        assertTrue(jwt.claims().contains("\"iat\""));
        assertTrue(jwt.claims().contains("\"exp\""));
        assertTrue(jwt.claims().contains("\"nbf\""));
        assertTrue(jwt.claims().contains("\"sub\""));
        assertTrue(jwt.claims().contains("\"iss\""));
        assertTrue(jwt.claims().contains("\"aud\""));
        assertTrue(jwt.claims().contains("\"jti\""));
        assertTrue(jwt.claims().contains("\"uid\""));
        System.out.println(jwt.claims());


        Map<?,?> jsonHeader = JsonWebToken.mapper(jwt.header());
        assertEquals("ES256", jsonHeader.get("alg"));
        assertEquals("JWT", jsonHeader.get("typ"));

        Map<?,?> jsonClaims = JsonWebToken.mapper(jwt.claims());
        assertEquals(1.0d, jsonClaims.get("v"));
        assertNotNull(jsonClaims.get("exp"));
        assertNotNull(jsonClaims.get("nbf"));
        assertNotNull(jsonClaims.get("iat"));

        Map<?,?> jsonData = (Map<?,?>)jsonClaims.get("d");
        assertEquals("-Je9A7op-mEPuhrDPN9T", jsonData.get("uid"));
        assertEquals("-Je9A7op-mEPuhrDPN9T", jsonData.get("id"));
        assertEquals("hoge", jsonData.get("request"));

        JsonWebToken other = JsonWebToken.valueOf(token);
        assertNotNull(other);
        //String publicKey = PUBLIC_KEY;
        assertTrue(other.verify(encodedPublicKey));

    }

    private final String SECRET_KEY = "moozooherpderp";

    @Test
    public void checkIfBasicLength() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            Map<String, Object> payload = new HashMap<String, Object>();
            new JsonWebToken.Builder()
                .signer("HS256", "x")
                .build(payload);
        });

        assertEquals("JsonWebToken.payload: data is empty and no options are set.",
                exception.getMessage());
    }

    @Test
    public void checkBasicStructureHasCorrectNumberOfFragments() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid", "1");
        payload.put("abc", "0123456789~!@#$%^&*()_+-=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,./;'[]\\<>?\"{}|");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("HS256", SECRET_KEY)
                .build(payload);
        String token = jwt.token();

        String[] tokenFragments = token.split("\\.");

        assertTrue(tokenFragments.length == 3, "Token has the proper number of fragments: jwt metadata, payload, and signature");
    }


    @Test
    public void checkIfResultProperlyDoesNotHavePadding() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid", "1");
        payload.put("abc", "0123456789~!@#$%^&*()_+-=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,./;'[]\\<>?\"{}|");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("HS256", SECRET_KEY)
                .build(payload);
        String token = jwt.token();

        assertTrue(token.indexOf('=') < 0);
    }


    @Test
    public void checkIfResultIsUrlSafePlusSign() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid", "1");
        payload.put("abc", "0123456789~!@#$%^&*()_+-=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,./;'[]\\<>?\"{}|");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("HS256", SECRET_KEY)
                .build(payload);
        String token = jwt.token();

        assertTrue(token.indexOf('+') < 0);
    }

    @Test
    public void checkIfResultIsUrlSafePlusSlash() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid", "1");
        payload.put("abc", "0123456789~!@#$%^&*()_+-=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,./;'[]\\<>?\"{}|");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("HS256", SECRET_KEY)
                .build(payload);
        String token = jwt.token();

        assertTrue(token.indexOf('/') < 0);
    }

    @Test
    public void checkIfResultHasWhiteSpace() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid", "1");
        payload.put("a", "apple");
        payload.put("b", "banana");
        payload.put("c", "carrot");
        payload.put("number", Double.MAX_VALUE);
        payload.put("abc", "0123456789~!@#$%^&*()_+-=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,./;'[]\\<>?\"{}|");
        payload.put("help", "You get a risk-free product. We offer a 30-day money-back guarantee, provided that you did not print the materials and your access to the files will be fully disabled. If you are not satisfied with our product, let us know within 30 days, and we will disable your service and give you a full refund. We will however ask what didn't work for you so that we can learn and improve.");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("HS256", SECRET_KEY)
                .build(payload);
        String token = jwt.token();

        Pattern pattern = Pattern.compile("\\s");
        Matcher matcher = pattern.matcher(token);
        boolean hasWhiteSpace = matcher.find();

        assertFalse(hasWhiteSpace, "Token has white space");
    }

    @Test
    public void basicInspectionTest() {
        String customData = "You get a risk-free product. We offer a 30-day money-back guarantee, provided that you did not print the materials and your access to the files will be fully disabled. If you are not satisfied with our product, let us know within 30 days, and we will disable your service and give you a full refund. We will however ask what didn't work for you so that we can learn and improve.";
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid", "1");
        payload.put("help", customData);

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("HS256", SECRET_KEY)
                .expires(new Date())
                .notBefore(new Date())
                .build(payload);

        String token = jwt.token();

        String[] tokenFragments = token.split("\\.");

        String header = tokenFragments[0];
        String claims = tokenFragments[1];

        try {
            header = new String(Base64.getDecoder().decode(header.getBytes(Charset.forName("UTF-8"))), "UTF-8");
            claims = new String(Base64.getDecoder().decode(claims.getBytes(Charset.forName("UTF-8"))), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            fail(e.getMessage());
        }

        Map<?,?> jsonHeader = JsonParser.unmarshal(header, Map.class);
        assertEquals("HS256", jsonHeader.get("alg"));
        assertEquals("JWT", jsonHeader.get("typ"));

        Map<?,?> jsonClaims = JsonParser.unmarshal(claims, Map.class);
        assertEquals(1.0d, jsonClaims.get("v"));

        Map<?,?> jsonData = (Map<?,?>)jsonClaims.get("d");
        assertEquals(customData, jsonData.get("help"));
        assertNotNull(jsonClaims.get("exp"));
        assertNotNull(jsonClaims.get("iat"));
        assertNotNull(jsonClaims.get("nbf"));
    }

    @Test
    public void allowMaxLengthUid() {
        Map<String, Object> payload = new HashMap<String, Object>();
        //                          10        20        30        40        50        60        70        80        90       100       110       120       130       140       150       160       170       180       190       200       210       220       230       240       250   256
        payload.put("uid", "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("HS256", SECRET_KEY)
                .build(payload);
        String token = jwt.token();
        assertNotNull(token);
    }

    @Test
    public void disallowTokensTooLong() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid", "blah");
        payload.put("longVar", "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345612345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234561234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456");

        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            JsonWebToken jwt = new JsonWebToken.Builder()
                    .signer("HS256", SECRET_KEY)
                    .build(payload);
            jwt.token();
        });

        assertEquals("JsonWebToken.payload: Generated token is too long. The token cannot be longer than 1024 bytes.",
                exception.getMessage());
    }


    @Test
    public void emptyPayload() {
        JsonWebToken.Builder builder = new JsonWebToken.Builder()
                .signer("HS256", SECRET_KEY);
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            builder.build(null)
                   .token();
        });
        assertEquals("JsonWebToken.payload: data is empty and no options are set.",
                exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
            builder.build(new HashMap<String, Object>())
                   .token();
        });

        assertEquals("JsonWebToken.payload: data is empty and no options are set.",
                exception.getMessage());

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("foo", "bar");
        String token2 = builder
                .build(payload)
                .token();
        assertNotNull(token2);
    }

    @Test
    public void testRandomString() {
        int loop = 1000;
        List<String> list = new ArrayList<>();
        for (int i=0; i<loop; i++) {
            String r = JsonWebToken.randomString(10, 16);
            assertFalse(list.contains(r));
        }
    }

}
