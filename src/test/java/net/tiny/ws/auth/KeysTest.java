package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;


public class KeysTest {

    static final String PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuA9BNNJckSivg1J1tRotBVPFclNBfx13wIEMEGifOuLEcQpOoHLSwgN+zB5ZL78x9zYoqVxUodwt4QFPv/mgYwy5ZpU40/NeJSnMa2ZYoKba4PdlwD6VWNrH2MtoL9jFSOE8jDYEI8Zb7bwIatfQKxF2APS/ZyKhQmenXyCxY/E1qv6BzgjQZohLMraYXZJ7nAyvSW2bmNgWBkf6LyGwVFM4KzM222Pz7tXTT0iFP385nMisZ3Po43xUP5y8v/klF73M6El+fcE0U6O+V8eq9U2a5DgsiEL6IyoGI1+vy51SDvb9bUqGE8Yma3sTg1YUTtqjapPhXq0XpLW7tf9+2QIDAQAB\n-----END PUBLIC KEY-----";
    static final String PRIVATE_KEY="-----BEGIN RSA PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC4D0E00lyRKK+DUnW1Gi0FU8VyU0F/HXfAgQwQaJ864sRxCk6gctLCA37MHlkvvzH3NiipXFSh3C3hAU+/+aBjDLlmlTjT814lKcxrZligptrg92XAPpVY2sfYy2gv2MVI4TyMNgQjxlvtvAhq19ArEXYA9L9nIqFCZ6dfILFj8TWq/oHOCNBmiEsytphdknucDK9JbZuY2BYGR/ovIbBUUzgrMzbbY/Pu1dNPSIU/fzmcyKxnc+jjfFQ/nLy/+SUXvczoSX59wTRTo75Xx6r1TZrkOCyIQvojKgYjX6/LnVIO9v1tSoYTxiZrexODVhRO2qNqk+FerRektbu1/37ZAgMBAAECggEBAK9pimFG4gVNWweyfI9eJO3gylmMUu1MLiZ+VfsFWksKduCsTAbJp2ZTYnIxshm5A2twaAwP/HBNoEPBtjllM59yLvc+22vTkjOkxDbO0UQ6AHtKC+TNQBPwXWmVYPPIiSxNzCBsEkHt9wp4myUIDFIfT/DVT9yAumLI2k/knmp5hrJg3y7hgDIJCxRGq6fxfrJ/TT61ExAQNg3M4kznruyIleuLLecgblSqVJvGO3VjeStxa8uUUhURLUjNpAmgg5919i3lLH5zeq9j6Rs/PDs1g0y/8asjJ4EZeUUf8FNCYww9FuP19Auwn6YMfBjlHYMDB1FjYoBV26li55Q3m8kCgYEA5Nno7eqimVn80kf9vjZRroFQ9TrUv42YUtHphwDEEecsDg1rMKoYw02oPeJiZpC2B6PKAd87DtT9dGMAG3LQvjSnfEWZN0NEFxpQnp5pK77dHiz7R6YzQXBtQBNi2U6fctyoi0HRakTAYFXbmYJX5FbFl3Qmb8mdf/W3+6OSmYcCgYEAzeUMB4X6SD/QreiK5a9JO9pROc6U2haY7z3VTiEV9xLHB80QbUZkifQPc99fqMV28GnUwWH2vYHYhV6aqMOeJVLQDjx8FUY+ewQpUqJTFHZ+67/82nnZ71uvHXhUKwyAp1YMCIhMV4237Luja0JUjf51KUJAaDKAM6LU/QH1vJ8CgYAizd016bxG1D+1/0rg1cQAZRxZ31OhujgTSGdap8wp6N1zWakWopfXhAT1PGu4q4Nzj/5V4IxLyiqtu6y1f/WtJ7bdGHyfwfQmC1N9fBwEkidwwmiI0gbzidSjrQ8Ye3OWdWaQnzbpEYGsZQJby03wpR2x1fFOiPwSDN2pQIw4GwKBgC9zmlIhjo0YJ//smBZXT4l7xzyLX8Ljcluw5HgdJ/LJYRVrLV4B2ynwFZ+e220KFV9TBLWM3lDOnBggtYQvkUMI0up1BiPhDIVNcDibMIqGxLmQhbXUX2XKu5EPKlbBiuUF78AHqZCEGN92XzNZFiOjgV0A59cbzHAaFvw4d2oBAoGALAPEDU5U+RfgpFuduz2OG+d3bjY6EmBvTzYKGzMWcRsPg9k4bwC7MB2lBAM/ad+EUvRlI/BLy3exsK1afSptqJV3xmUoOSiitJT2jM92Yt15DHqbqZ2lTAQJjeAzd2iFpUSRK3KWYstdg6CZ6j9VSbCPPxQIZZQpCE9+AgpZiAI=\n-----END RSA PRIVATE KEY-----";
    static final String PRIVATE_KEY_PEM = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEoAIBAAKCAQEAnFWdIwBbLRw4xfFDXYFmlXKB4BpKeuAtfh1dcs5mhod0WTo/\n" +
            "i/Z4DOpiiw/2H05luI4PzOZem8AlHI9hUhHq5p1+YHM68SyvBQ9OTl+O90nmLYOt\n" +
            "2Jzquks11bf29nJh7KwGVHOv2nh3eL39BVsqHSt0O/rjSa0bV+QtUc2DP9U4WzZ3\n" +
            "8RhT2bdiRcsDuMfI024u9JGG/O4iG3wDlXyS5j6G0NVw/KEJJtYYv8ruQVpvlKUd\n" +
            "Ntx7aE+u6F60SjJYQSfdjMoQNMDglBFwhY11RlHSmiJ/Ym8aE+Hj11JHhPcB1N+X\n" +
            "RWaHV9ply4TnE13PsQtGWVKsLDNQNUeIUljKdQIDAQABAoIBAAa4d3owYxBcDOTA\n" +
            "K7vdUDekezN9wy3nwozlXkW33G3JbOsDt1pLoiWL/eh/Kyl1XqdsaVQkTco28bbP\n" +
            "Qx5wFBUN4tzqlzdpoFcrV/EZPTV268+RFZbLnXDyGBez7N3zVNpZGtHj7JoLtmHD\n" +
            "vm4jLnr1NJik1G3aZI6GtJwLpaocwtKWHB59hVwF5NinW6BXN0ALNfwKwU4vMWYo\n" +
            "I65F2zvGMVl9rbfvU+E73DXK3TN5tLOAkqZMQ8+g/VnNd/XuZwh2ZADokEXV8aNR\n" +
            "7zVm3MCCcaa8IKJMrgnb9q47tzfyaoIu5aRYGYKZ/8wuItv4Dal30MK1CQoCD8cD\n" +
            "5uzorQECgYEA9+QTCXrVHzhJJm+QWQZrXu7ydk+tEix7WY9ZY702OHiTO2x9IT4d\n" +
            "4lKFbLhQrQMAFhO3B31Hq5ODGS4jB3bFzATrtOR9eLCR7l+0Az2FcU1Zmqsdkyv8\n" +
            "zlkD9oOYif6rICrVyLQ/lbQF7erVDRbxJUjeKqGAnvELrlzcr+rx+XECgYEAoXLQ\n" +
            "MdR+OLsP5XbcoA//Z2pgwwKZVs282MfYjZLVqeEAAC8BB9+8HHrtMaJGvADI06OV\n" +
            "7lTCDaE8UlqgzN2B55FmCTiLABjhk3fEDrhGVe4jhEZz1i8t0ArjsYTwXs/uXoUz\n" +
            "YP2rcJtkybOQEzjbvM4s5+B8iht+dYaqwoW5/0UCgYAp68UYZlBiXjdoq5dCpuZD\n" +
            "gK86ONEw8JrPk4Fvb5EazbFAbGFg3Mta+c+cijMCfy5ljWH3f0U+i8yw1m+QFJLw\n" +
            "pKhjx/w8C8gyArdDkQTfG1Ca6nMu71JqZv1Xk/uY4pt37iaHMYxLOc2C5aKv+wA+\n" +
            "6OrBVNyWhHcQPp4Hlfjj0QJ/de5oJf4SNV5vPi6U+la1OdV62PgNCls+lxtkFAYu\n" +
            "DOlOFtQ+7IGB50vj912STcJE8FOOMYm4NjyQ05df3kXvnjeXUST8ZBXIsO/LRvVU\n" +
            "a3CIgRb1hn7v+Af8Sq/Q5XD9rg2eejrSAG+CL9P6ahAecswoATj5v+hVd4PnODB2\n" +
            "rQKBgAwe3pkQRFHjameLHip+xcHQ85aASiLjhTvFhFjRHDpJ+FoiJ2H4xi4/jd1F\n" +
            "KGrhMpVnLXKwe1HaONFPV3yEFK2da1r66iIr/opcx1hyKmV1xvebcUxYYoRY6j/g\n" +
            "JMsceBR10oGEath+43rS78LASIQG83PmTYhkcEkQNftxEGqC\n" +
            "-----END RSA PRIVATE KEY-----";

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
    public void testRSAKey() throws Exception {
        KeyPair keyPair = JsonWebToken.generateRSAKeyPair(JsonWebToken.Algorithm.RS256);

        PublicKey publicKey = (PublicKey) keyPair.getPublic();
        PrivateKey privateKey = (PrivateKey) keyPair.getPrivate();
        String encodedPublicKey = Keys.encodeKey(publicKey);
        System.out.println("Public Key:");
        System.out.println(encodedPublicKey);
        assertNotNull(Keys.decodeRSAPublicKey(encodedPublicKey));

        String encodedPrivateKey = Keys.encodeKey(privateKey);
        System.out.println("Private Key:");
        System.out.println(encodedPrivateKey);
        assertNotNull(Keys.decodeRSAPrivateKey(encodedPrivateKey));

        JsonWebToken.Signer signer = JsonWebToken.createSigner("RS256", encodedPrivateKey);
        assertNotNull(signer);
        String sig = signer.sign("mydata");
        //String sig = sign("mydata".getBytes(), encodedPrivateKey);
        System.out.println(sig);

        //Using PrivateKey to verify
        JsonWebToken.Signer validator = JsonWebToken.createSigner("RS256", encodedPrivateKey);
        assertTrue(validator.verify("mydata", sig));

        //Using PublicKey to verify
        validator = JsonWebToken.createSigner("RS256", encodedPublicKey);
        assertTrue(validator.verify("mydata", sig));

    }

    @Test
    public void testHMACForSigningKey() throws Exception {
        String secret =  "OPeJAbqF07aWFw5SXvV7sUZnT3SbHVxzeZwqvJHV";
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JsonWebToken.Algorithm algorithm = JsonWebToken.forSigningKey(key);
        assertEquals(JsonWebToken.Algorithm.HS256, algorithm);

        //                          10        20        30        40        50        60
        secret =         "1234567890123456789012345678901234567890123456789012345678901";
        key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA384");
        algorithm = JsonWebToken.forSigningKey(key);
        assertEquals(JsonWebToken.Algorithm.HS384, algorithm);

        //                          10        20        30        40        50        60        70        80        90       100       110       120       130       140       150       160       170       180       190       200       210       220       230       240       250   256
        secret =         "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456";
        key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        algorithm = JsonWebToken.forSigningKey(key);
        assertEquals(JsonWebToken.Algorithm.HS512, algorithm);
    }

    @Test
    public void testRSAForSigningKey() throws Exception {
        KeyPair keyPair = JsonWebToken.generateKeyPair("RS256");

        PrivateKey privateKey = (PrivateKey) keyPair.getPrivate();

        JsonWebToken.Algorithm algorithm = JsonWebToken.forSigningKey(privateKey);
        assertEquals(JsonWebToken.Algorithm.RS256, algorithm);

        keyPair = JsonWebToken.generateKeyPair("RS384");
        privateKey = (PrivateKey) keyPair.getPrivate();
        algorithm = JsonWebToken.forSigningKey(privateKey);
        assertEquals(JsonWebToken.Algorithm.RS384, algorithm);
    }

    @Test
    public void testRSA512ForSigningKey() throws Exception {
        // Take about 4ms
        KeyPair keyPair = JsonWebToken.generateKeyPair("RS512");
        PrivateKey privateKey = (PrivateKey) keyPair.getPrivate();
        JsonWebToken.Algorithm algorithm = JsonWebToken.forSigningKey(privateKey);
        assertEquals(JsonWebToken.Algorithm.RS512, algorithm);
    }

    @Test
    public void testEllipticCurveForSigningKey() throws Exception {
        KeyPair keyPair = JsonWebToken.generateKeyPair("ES256");

        PrivateKey privateKey = (PrivateKey) keyPair.getPrivate();

        JsonWebToken.Algorithm algorithm = JsonWebToken.forSigningKey(privateKey);
        assertEquals(JsonWebToken.Algorithm.ES256, algorithm);

        keyPair = JsonWebToken.generateKeyPair("ES384");
        privateKey = (PrivateKey) keyPair.getPrivate();
        algorithm = JsonWebToken.forSigningKey(privateKey);
        assertEquals(JsonWebToken.Algorithm.ES384, algorithm);

        keyPair = JsonWebToken.generateKeyPair("ES512");
        privateKey = (PrivateKey) keyPair.getPrivate();
        algorithm = JsonWebToken.forSigningKey(privateKey);
        assertEquals(JsonWebToken.Algorithm.ES512, algorithm);
    }



    @Test
    public void testSSHPublicKey() throws Exception {
        KeyPair keyPair = JsonWebToken.generateKeyPair("RS256");
        PublicKey publicKey = (PublicKey) keyPair.getPublic();
        String encodedPublicKey = Keys.encodeSSHPublicKey(publicKey, "user@test.com");
        System.out.println(encodedPublicKey);
        assertTrue(encodedPublicKey.startsWith("ssh-rsa "));
        assertTrue(encodedPublicKey.endsWith(" user@test.com"));

        assertNotNull(Keys.decodeSSHPublicKey(encodedPublicKey));

        keyPair = JsonWebToken.generateKeyPair("ES256");
        publicKey = (PublicKey) keyPair.getPublic();
        encodedPublicKey = Keys.encodeSSHPublicKey(publicKey, "user@test.com");
        System.out.println(encodedPublicKey);
        assertTrue(encodedPublicKey.startsWith("ecdsa-sha2-"));
        assertTrue(encodedPublicKey.endsWith(" user@test.com"));
    }



}
