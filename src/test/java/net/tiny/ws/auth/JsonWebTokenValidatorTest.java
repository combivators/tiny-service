package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.tiny.service.Patterns;

public class JsonWebTokenValidatorTest {

    @Test
    public void testValidator() throws Exception {
        KeyPair keyPair = JsonWebToken.generateKeyPair("RS256");
        PublicKey publicKey = (PublicKey) keyPair.getPublic();
        PrivateKey privateKey = (PrivateKey) keyPair.getPrivate();
        String encodedPublicKey = Keys.encodeKey(publicKey);
        System.out.println(encodedPublicKey);
        String encodedPrivateKey = Keys.encodeKey(privateKey);

        JsonWebTokenValidator validator =  new JsonWebTokenValidator();
        validator.publicKey = () -> encodedPublicKey;
        validator.patterns = () -> Patterns.valueOf("!/api/v1/auth/key, !/api/v1/auth/token/.*, !/api/v1/activation/.*");

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("id", "1");
        payload.put("user", "hoge");
        payload.put("scope", "member");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("RS256", encodedPrivateKey)
                .subject("oauth")
                .issuer("net.tiny")
                .audience("user@net.tiny")
                .jti(true)
                .build(payload);
        String token = jwt.token();
        System.out.println(token);
        String auth = "Bearer " + token;
        assertTrue(validator.get().test("/api/v1/auth/key", auth));
        assertTrue(validator.get().test("/api/v1/auth/token/AuOuDaOx7tjzHW4mm4mviX", auth));
        assertTrue(validator.get().test("/api/v1/activation/1234/ZxswerTy", auth));

        assertTrue(validator.get().test("/api/v1/account/123/token", auth));
    }

    @Test
    public void testValidatorWithSSHPublicKey() throws Exception {
        KeyPair keyPair = JsonWebToken.generateKeyPair("RS256");
        PublicKey publicKey = (PublicKey) keyPair.getPublic();
        PrivateKey privateKey = (PrivateKey) keyPair.getPrivate();
        String encodedPublicKey = Keys.encodeSSHPublicKey(publicKey, "info@tiny.net");
        System.out.println(encodedPublicKey);
        String encodedPrivateKey = Keys.encodeKey(privateKey);

        JsonWebTokenValidator validator =  new JsonWebTokenValidator();
        validator.publicKey = () -> encodedPublicKey;
        validator.patterns = () -> Patterns.valueOf("!/api/v1/auth/key, !/api/v1/auth/token/.*, !/api/v1/activation/.*");

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("id", "1");
        payload.put("user", "hoge");
        payload.put("scope", "member");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("RS256", encodedPrivateKey)
                .subject("oauth")
                .issuer("net.tiny")
                .audience("user@net.tiny")
                .jti(true)
                .build(payload);
        String token = jwt.token();
        System.out.println(token);
        String auth = "Bearer " + token;
        assertTrue(validator.get().test("/api/v1/auth/key", auth));
        assertTrue(validator.get().test("/api/v1/auth/token/AuOuDaOx7tjzHW4mm4mviX", auth));
        assertTrue(validator.get().test("/api/v1/activation/1234/ZxswerTy", auth));

        assertTrue(validator.get().test("/api/v1/account/123/token", auth));
    }

    @Test
    public void testExpired() throws Exception {
        KeyPair keyPair = JsonWebToken.generateKeyPair("RS256");
        PublicKey publicKey = (PublicKey) keyPair.getPublic();
        PrivateKey privateKey = (PrivateKey) keyPair.getPrivate();
        String encodedPublicKey = Keys.encodeSSHPublicKey(publicKey, "info@tiny.net");
        System.out.println(encodedPublicKey);
        String encodedPrivateKey = Keys.encodeKey(privateKey);

        JsonWebTokenValidator validator =  new JsonWebTokenValidator();
        validator.publicKey = () -> encodedPublicKey;
        validator.patterns = () -> Patterns.valueOf("!/api/v1/auth/key, !/api/v1/auth/token/.*, !/api/v1/activation/.*");

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("id", "1");
        payload.put("user", "hoge");
        payload.put("scope", "member");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("RS256", encodedPrivateKey)
                .expires(new Date(System.currentTimeMillis()-100000L))
                .subject("oauth")
                .issuer("net.tiny")
                .audience("user@net.tiny")
                .jti(true)
                .build(payload);
        String auth = "Bearer " + jwt.token();

        assertFalse(validator.get().test("/api/v1/account/123/token", auth));
    }

    @Test
    public void testNotBefore() throws Exception {
        KeyPair keyPair = JsonWebToken.generateKeyPair("RS256");
        PublicKey publicKey = (PublicKey) keyPair.getPublic();
        PrivateKey privateKey = (PrivateKey) keyPair.getPrivate();
        String encodedPublicKey = Keys.encodeSSHPublicKey(publicKey, "info@tiny.net");
        System.out.println(encodedPublicKey);
        String encodedPrivateKey = Keys.encodeKey(privateKey);

        JsonWebTokenValidator validator =  new JsonWebTokenValidator();
        validator.publicKey = () -> encodedPublicKey;
        validator.patterns = () -> Patterns.valueOf("!/api/v1/auth/key, !/api/v1/auth/token/.*, !/api/v1/activation/.*");

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("id", "1");
        payload.put("user", "hoge");
        payload.put("scope", "member");

        JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("RS256", encodedPrivateKey)
                .notBefore(new Date())
                .subject("oauth")
                .issuer("net.tiny")
                .audience("user@net.tiny")
                .jti(true)
                .build(payload);
        String auth = "Bearer " + jwt.token();

        assertFalse(validator.get().test("/api/v1/account/123/token", auth));
    }
}
