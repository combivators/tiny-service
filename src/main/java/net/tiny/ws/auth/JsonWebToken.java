package net.tiny.ws.auth;

import java.io.PrintStream;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import net.tiny.config.JsonParser;

public final class JsonWebToken {

    public static final int TOKEN_VERSION = 1;

    // @see https://github.com/jwtk/jjwt/blob/master/api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java
    public static enum Algorithm {
        NONE("No digital signature or MAC performed", "None", null, false, 0, 0),
        HS256("HMAC using SHA-256", "HMAC", "HmacSHA256", true, 256, 256),
        HS384("HMAC using SHA-384", "HMAC", "HmacSHA384", true, 384, 384),
        HS512("HMAC using SHA-512", "HMAC", "HmacSHA512", true, 512, 512),
        RS256("RSASSA-PKCS-v1_5 using SHA-256", "RSA", "SHA256withRSA", true, 256, 2048),
        RS384("RSASSA-PKCS-v1_5 using SHA-384", "RSA", "SHA384withRSA", true, 384, 2048),
        RS512("RSASSA-PKCS-v1_5 using SHA-512", "RSA", "SHA512withRSA", true, 512, 2048),
        ES256("ECDSA using P-256 and SHA-256", "ECDSA", "SHA256withECDSA", true, 256, 256),
        ES384("ECDSA using P-384 and SHA-384", "ECDSA", "SHA384withECDSA", true, 384, 384),
        ES512("ECDSA using P-521 and SHA-512", "ECDSA", "SHA512withECDSA", true, 512, 521),
        PS256("RSASSA-PSS using SHA-256 and MGF1 with SHA-256", "RSA", "SHA256withRSAandMGF1", false, 256, 2048),
        PS384("RSASSA-PSS using SHA-384 and MGF1 with SHA-384", "RSA", "SHA384withRSAandMGF1", false, 384, 2048),
        PS512("RSASSA-PSS using SHA-512 and MGF1 with SHA-512", "RSA", "SHA512withRSAandMGF1", false, 512, 2048);

        final String description;
        final String family;
        final String type;
        final boolean jdk;
        final int length;
        final int min;
        /**
         *
         * @param d Description
         * @param f Family name
         * @param t JCA type
         * @param j JDK Standard
         * @param l Digest length
         * @param m Minimum key length
         */
        Algorithm(String d, String f, String t, boolean j, int l, int m) {
            description = d;
            family = f;
            type = t;
            jdk = j;
            length = l;
            min = m;
        }
    }

    private final static long DEFAULT_EXPIRES = 3600L; //1 hour (3600 seconds)
    private final static String DEFAULT_ALG = "HS256";

    private final static String TOKEN_SEP = ".";
    private final static String HEADER_JSON_FORMAT = "{\"typ\":\"JWT\",\"alg\":\"%s\"}";
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
    // HS256
    private final static Signer DEFAULT_SIGNER = createSigner(DEFAULT_ALG, new String(SECRET_KEY));
    private static final SecureRandom DEFAULT_SECURE_RANDOM;
    static {
        DEFAULT_SECURE_RANDOM = new SecureRandom();
        DEFAULT_SECURE_RANDOM.nextBytes(new byte[64]);
    }

    private static final Map<Algorithm, PSSParameterSpec> PSS_PARAMETER_SPECS = createPssParameterSpecs();
    private static Map<Algorithm, PSSParameterSpec> createPssParameterSpecs() {
        Map<Algorithm, PSSParameterSpec> m = new HashMap<Algorithm, PSSParameterSpec>();
        MGF1ParameterSpec ps = MGF1ParameterSpec.SHA256;
        PSSParameterSpec spec = new PSSParameterSpec(ps.getDigestAlgorithm(), "MGF1", ps, 32, 1);
        m.put(Algorithm.PS256, spec);

        ps = MGF1ParameterSpec.SHA384;
        spec = new PSSParameterSpec(ps.getDigestAlgorithm(), "MGF1", ps, 48, 1);
        m.put(Algorithm.PS384, spec);

        ps = MGF1ParameterSpec.SHA512;
        spec = new PSSParameterSpec(ps.getDigestAlgorithm(), "MGF1", ps, 64, 1);
        m.put(Algorithm.PS512, spec);

        return m;
    }

    private static final Map<Algorithm, String> EC_CURVE_NAMES = createEcCurveNames();
    private static Map<Algorithm, String> createEcCurveNames() {
        Map<Algorithm, String> m = new HashMap<Algorithm, String>(); //alg to ASN1 OID name
        m.put(Algorithm.ES256, "secp256r1");
        m.put(Algorithm.ES384, "secp384r1");
        m.put(Algorithm.ES512, "secp521r1");
        return m;
    }

    private static final List<Algorithm> PREFERRED_HMAC_ALGS = Collections.unmodifiableList(
            Arrays.asList(Algorithm.HS512, Algorithm.HS384, Algorithm.HS256));

    private static final List<Algorithm> PREFERRED_EC_ALGS = Collections.unmodifiableList(
            Arrays.asList(Algorithm.ES512, Algorithm.ES384, Algorithm.ES256));
/*
    public static class Header {
        public String alg = "HS256";
        public String typ = "JWT";
    }

    public static class Claims {
        public int v;
        public String iss; //Issuer
        public String sub; //Subject
        public String aud; //Audience
        public String exp; //Expiration Time
        public String nbf; //Not Before
        public String iat; //Issued At
        public String jti; //JWT ID
        public String typ; //Content Type
    }
*/

    public Builder builder;
    private String header;
    private String claims;
    private String sign;

    /**
     * Default constructor given a builder.
     *
     * @param builder
     */
    private JsonWebToken(Builder builder) {
        this.builder = builder;
    }

    /**
     * Default constructor given a builder.
     *
     * @param h Encoded header
     * @param c Encoded claims
     * @param s Signed
     */
    private JsonWebToken(String h, String c, String s) {
        header = h;
        claims = c;
        sign   = s;
    }

    /**
     * Create a token for the given object.
     *
     * @param payload
     * @return
     */
    protected JsonWebToken payload(Map<String, Object> payload) {
        payload(payload, builder.options, builder.signer);
        return this;
    }

    /**
     * Create a token for the given object and options, encoder.
     *
     * @param data
     * @param options
     * @param signer
     */
    protected void payload(Map<String, Object> data, Options options, Signer signer) {
        if ((data == null || data.size() == 0) || options == null || signer == null) {
            throw new IllegalArgumentException(
                    "JsonWebToken.payload: data is empty and no options are set.");
        }

        final Map<String, Object> require = new LinkedHashMap<>();
        require.put("v", TOKEN_VERSION);
        require.put("iat", new Date().getTime() / 1000);

        if (data != null && data.size() > 0) {
            require.put("d", data);
        }

        // Handle options
        if (options != null) {
            if (options.expires != null) {
                require.put("exp", options.expires.getTime() / 1000);
            }
            if (options.notBefore != null) {
                require.put("nbf", options.notBefore.getTime() / 1000);
            }
            if (options.jti) {
                require.put("jti", UUID.randomUUID().toString());
            }
            if (options.issuer != null && !options.issuer.isEmpty()) {
                require.put("iss", options.issuer);
            }
            if (options.audience != null && !options.audience.isEmpty()) {
                require.put("aud", options.audience);
            }
            if (options.subject != null && !options.subject.isEmpty()) {
                require.put("sub", options.subject);
            }
        }

        header = encodeJson(String.format(HEADER_JSON_FORMAT, signer.algorithm));
        claims = encodeJson(JsonParser.marshal(require, true));
        final String bits = new StringBuilder(header)
                    .append(TOKEN_SEP)
                    .append(claims)
                    .toString();
        sign = signer.sign(bits);
        String token = token();
        if (token.length() > 1024) {
            throw new IllegalArgumentException(
                    "JsonWebToken.payload: Generated token is too long. The token cannot be longer than 1024 bytes.");
        }
    }

    public String algorithm() {
        return String.valueOf(mapper(decodeJson(header)).get("alg"));
    }

    public String header() {
        return decodeJson(header);
    }

    public String claims() {
        return decodeJson(claims);
    }

    public String token() {
        return new StringBuilder(header)
                .append(TOKEN_SEP)
                .append(claims)
                .append(TOKEN_SEP)
                .append(sign)
                .toString();
    }

    public boolean verify(String secret) {
        final Signer signer = createSigner(algorithm(), secret);
        final String bits = new StringBuilder(header)
                .append(TOKEN_SEP)
                .append(claims)
                .toString();
        return signer.verify(bits, sign);
    }

    public boolean expired() {
        Object value = mapper(decodeJson(claims)).get("exp");
        if (null == value)
            return false;
        long exp = 0L;
        if (value instanceof Double) {
            exp = ((Double)value).longValue();
        } else {
            try {
                exp = Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException e) {}
        }
        return exp < System.currentTimeMillis();
    }

    public void print(PrintStream out) {
        out.println("header:");
        out.println(String.valueOf(header()));
        out.println("claims:");
        out.println(String.valueOf(claims()));
    }

    @Override
    public String toString() {
        return token();
    }

    public static Map<?,?> mapper(String fragment) {
        return JsonParser.unmarshal(fragment, Map.class);
    }

    public static JsonWebToken valueOf(String token) {
        final String[] tokenFragments = token.split("\\.");
        if (tokenFragments == null || tokenFragments.length != 3) {
            return null;
        }
        return new JsonWebToken(tokenFragments[0], tokenFragments[1], tokenFragments[2]);
    }

    /**
     * Encode and sign a set of claims.
     *
     * @param require
     * @param algorithm
     * @param secret
     * @return
     */
    public static JsonWebToken encode(Object require, String algorithm, String secret) {
        Signer signer = createSigner(algorithm, secret);
        return encode(require, signer);
    }

    private static JsonWebToken encode(final Object require, final Signer signer) {
        final String encodedHeader  = encodeJson(String.format(HEADER_JSON_FORMAT, signer.algorithm));
        final String encodedClaims = encodeJson(JsonParser.marshal(require, true));
        final String bits = new StringBuilder(encodedHeader)
                    .append(TOKEN_SEP)
                    .append(encodedClaims)
                    .toString();
        final String sig = signer.sign(bits);
        return new JsonWebToken(encodedHeader, encodedClaims, sig);
    }

    /**
     * Decode JSON web token and verify sign.
     *
     * @param token JSON web token
     * @param secret
     * @return
     */
    public static JsonWebToken decode(String token, String secret) {
        JsonWebToken jwt = valueOf(token);
        if (null != jwt && jwt.verify(secret)) {
            return jwt;
        }
        return null;
    }

    private static String encodeJson(String jsonData) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(jsonData.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeJson(String encoded) {
        return new String(Base64.getDecoder().decode(encoded.getBytes(StandardCharsets.UTF_8)));
    }

    static Algorithm forSigningKey(Key key) {
        if (!(key instanceof SecretKey ||
             (key instanceof PrivateKey && (key instanceof ECKey || key instanceof RSAKey)))) {
            String msg = "JWT standard signing algorithms require either 1) a SecretKey for HMAC-SHA algorithms or " +
                    "2) a private RSAKey for RSA algorithms or 3) a private ECKey for Elliptic Curve algorithms.  " +
                    "The specified key is of type " + key.getClass().getName();
            throw new IllegalArgumentException(msg);
        }
        if (key instanceof SecretKey) {
            SecretKey secretKey = (SecretKey)key;
            int bitLength = secretKey.getEncoded().length * Byte.SIZE;
            for(Algorithm alg : PREFERRED_HMAC_ALGS) {
                if (bitLength >= alg.min) {
                    return alg;
                }
            }
            String msg = "The specified SecretKey is not strong enough to be used with JWT HMAC signature " +
                "algorithms.  The JWT specification requires HMAC keys to be >= 256 bits long.  The specified " +
                "key is " + bitLength + " bits.  See https://tools.ietf.org/html/rfc7518#section-3.2 for more " +
                "information.";
            throw new IllegalArgumentException(msg);
        }

        if (key instanceof RSAKey) {
            RSAKey rsaKey = (RSAKey) key;
            int bitLength = rsaKey.getModulus().bitLength();

            if (bitLength >= 4096) {
                return Algorithm.RS512;
            } else if (bitLength >= 3072) {
                return Algorithm.RS384;
            } else if (bitLength >= 256) {
                return Algorithm.RS256;
            }

            String msg = "The specified RSA signing key is not strong enough to be used with JWT RSA signature " +
                "algorithms.  The JWT specification requires RSA keys to be >= 2048 bits long.  The specified RSA " +
                "key is " + bitLength + " bits.  See https://tools.ietf.org/html/rfc7518#section-3.3 for more " +
                "information.";
            throw new IllegalArgumentException(msg);
        }

        // if we've made it this far in the method, the key is an ECKey due to the instanceof assertions at the
        // top of the method

        ECKey ecKey = (ECKey) key;
        int bitLength = ecKey.getParams().getOrder().bitLength();

        for (Algorithm alg : PREFERRED_EC_ALGS) {
            if (bitLength >= alg.min) {
                return alg;
            }
        }

        String msg = "The specified Elliptic Curve signing key is not strong enough to be used with JWT ECDSA " +
            "signature algorithms.  The JWT specification requires ECDSA keys to be >= 256 bits long.  " +
            "The specified ECDSA key is " + bitLength + " bits.  See " +
            "https://tools.ietf.org/html/rfc7518#section-3.4 for more information.";
        throw new IllegalArgumentException(msg);
    }

    public static KeyPair generateKeyPair(String alg) {
        final Algorithm algorithm = Algorithm.valueOf(alg.toUpperCase());
        if ("RSA".equals(algorithm.family)) {
            return generateRSAKeyPair(algorithm);
        } else if ("ECDSA".equals(algorithm.family)) {
            return generateECKeyPair(algorithm);
        } else {
            throw new UnsupportedOperationException("Only RSA or ECDSA algorithms are supported by this method.");
        }
    }

    static KeyPair generateRSAKeyPair(Algorithm alg) {
        int keySizeInBits = 0;
        switch (alg) {
            case RS256:
            case PS256:
                keySizeInBits = 2048;
                break;
            case RS384:
            case PS384:
                keySizeInBits = 3072;
                break;
            case RS512:
            case PS512:
                keySizeInBits = 4096;
                break;
            default:
                throw new UnsupportedOperationException("Only RSA algorithms are supported by this method.");
        }

        KeyPairGenerator keyGenerator;
        try {
            keyGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to obtain an RSA KeyPairGenerator: " + e.getMessage(), e);
        }

        keyGenerator.initialize(keySizeInBits, DEFAULT_SECURE_RANDOM);
        return keyGenerator.genKeyPair();
    }

    static KeyPair generateECKeyPair(Algorithm alg) {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            String paramSpecCurveName = EC_CURVE_NAMES.get(alg);
            if (null == paramSpecCurveName) {
                throw new UnsupportedOperationException("Only ECDSA algorithms are supported by this method.");
            }
            ECGenParameterSpec spec = new ECGenParameterSpec(paramSpecCurveName);
            g.initialize(spec, DEFAULT_SECURE_RANDOM);
            return g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate Elliptic Curve KeyPair: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the expected signature byte array length (R + S parts) for
     * the specified ECDSA algorithm.
     *
     * @param alg The ECDSA algorithm. Must be supported and not
     *            {@code null}.
     * @return The expected byte array length for the signature.
     */
    private static int getSignatureByteArrayLength(final Algorithm alg) {
        switch (alg) {
            case ES256:
                return 64;
            case ES384:
                return 96;
            case ES512:
                return 132;
            default:
                throw new IllegalArgumentException("Unsupported Algorithm: " + alg.name());
        }
    }

    /**
     * Transcodes the JCA ASN.1/DER-encoded signature into the concatenated
     * R + S format expected by ECDSA JWS.
     *
     * @param derSignature The ASN1./DER-encoded. Must not be {@code null}.
     * @param outputLength The expected length of the ECDSA JWS signature.
     * @return The ECDSA JWS encoded signature.
     */
    private static byte[] transcodeSignatureToConcat(final byte[] derSignature, int outputLength) {
        if (derSignature.length < 8 || derSignature[0] != 48) {
            throw new IllegalArgumentException("Invalid ECDSA signature format");
        }

        int offset;
        if (derSignature[1] > 0) {
            offset = 2;
        } else if (derSignature[1] == (byte) 0x81) {
            offset = 3;
        } else {
            throw new IllegalArgumentException("Invalid ECDSA signature format");
        }

        byte rLength = derSignature[offset + 1];

        int i = rLength;
        while ((i > 0) && (derSignature[(offset + 2 + rLength) - i] == 0)) {
            i--;
        }

        byte sLength = derSignature[offset + 2 + rLength + 1];

        int j = sLength;
        while ((j > 0) && (derSignature[(offset + 2 + rLength + 2 + sLength) - j] == 0)) {
            j--;
        }

        int rawLen = Math.max(i, j);
        rawLen = Math.max(rawLen, outputLength / 2);

        if ((derSignature[offset - 1] & 0xff) != derSignature.length - offset
            || (derSignature[offset - 1] & 0xff) != 2 + rLength + 2 + sLength
            || derSignature[offset] != 2
            || derSignature[offset + 2 + rLength] != 2) {
            throw new IllegalArgumentException("Invalid ECDSA signature format");
        }

        final byte[] concatSignature = new byte[2 * rawLen];
        System.arraycopy(derSignature, (offset + 2 + rLength) - i, concatSignature, rawLen - i, i);
        System.arraycopy(derSignature, (offset + 2 + rLength + 2 + sLength) - j, concatSignature, 2 * rawLen - j, j);
        return concatSignature;
    }

    /**
     * Transcodes the ECDSA JWS signature into ASN.1/DER format for use by
     * the JCA verifier.
     *
     * @param jwsSignature The JWS signature, consisting of the
     *                     concatenated R and S values. Must not be
     *                     {@code null}.
     * @return The ASN.1/DER encoded signature.
     */
    private static byte[] transcodeSignatureToDER(byte[] jwsSignature) {
        int rawLen = jwsSignature.length / 2;
        int i = rawLen;

        while ((i > 0) && (jwsSignature[rawLen - i] == 0)) {
            i--;
        }

        int j = i;
        if (jwsSignature[rawLen - i] < 0) {
            j += 1;
        }

        int k = rawLen;

        while ((k > 0) && (jwsSignature[2 * rawLen - k] == 0)) {
            k--;
        }

        int l = k;
        if (jwsSignature[2 * rawLen - k] < 0) {
            l += 1;
        }

        int len = 2 + j + 2 + l;
        if (len > 255) {
            throw new IllegalArgumentException("Invalid ECDSA signature format");
        }

        int offset;

        final byte derSignature[];

        if (len < 128) {
            derSignature = new byte[2 + 2 + j + 2 + l];
            offset = 1;
        } else {
            derSignature = new byte[3 + 2 + j + 2 + l];
            derSignature[1] = (byte) 0x81;
            offset = 2;
        }

        derSignature[0] = 48;
        derSignature[offset++] = (byte) len;
        derSignature[offset++] = 2;
        derSignature[offset++] = (byte) j;
        System.arraycopy(jwsSignature, rawLen - i, derSignature, (offset + j) - i, i);

        offset += j;
        derSignature[offset++] = 2;
        derSignature[offset++] = (byte) l;

        System.arraycopy(jwsSignature, 2 * rawLen - k, derSignature, (offset + l) - k, k);
        return derSignature;
    }

    protected static Signer createSigner(String algorithm, String key) {
        Algorithm alg = Algorithm.valueOf(algorithm.toUpperCase());
        switch (alg) {
        case HS256:
        case HS384:
        case HS512:
            return new MacSigner(alg, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), alg.type));
        case RS256:
        case RS384:
        case RS512:
        case PS256:
        case PS384:
        case PS512:
            return new RsaSigner(alg, Keys.decodeRSAKey(key));
        case ES256:
        case ES384:
        case ES512:
            return new EllipticCurveSigner(alg, Keys.decodeEllipticCurveKey(key));
        default:
            throw new IllegalArgumentException(String.format("The '%s' algorithm cannot be used for signing.", alg.name()));
        }
    }

    protected static Signer createSigner(Algorithm alg, Key key) {
        switch (alg) {
        case HS256:
        case HS384:
        case HS512:
            return new MacSigner(alg, key);
        case RS256:
        case RS384:
        case RS512:
        case PS256:
        case PS384:
        case PS512:
            return new RsaSigner(alg, key);
        case ES256:
        case ES384:
        case ES512:
            return new EllipticCurveSigner(alg, key);
        default:
            throw new IllegalArgumentException(String.format("The '%s' algorithm cannot be used for signing.", alg.name()));
        }
    }

    static abstract class Signer {
        Algorithm algorithm;
        Key key;
        Signer(Algorithm a, Key k) {
            algorithm = a;
            key = k;
        }
        Signer(Algorithm alg) {
            algorithm = alg;
        }
        public abstract String sign(String bits);
        public abstract boolean verify(String bits, String sig);
    }

    static abstract class SignatureProvider extends Signer {
        SignatureProvider(Algorithm a, Key k) {
            super(a, k);
        }

        protected Signature createSignatureInstance() {
            try {
                return Signature.getInstance(algorithm.type);
            } catch (NoSuchAlgorithmException e) {
                String msg = "Unavailable " + algorithm.family + " Signature algorithm '" + algorithm.type + "'.";
                if (!algorithm.jdk) {
                    msg += " This is not a standard JDK algorithm. Try including BouncyCastle in the runtime classpath.";
                }
                throw new RuntimeException(msg, e);
            }
        }

        @Override
        public String sign(String bits) {
            try {
                byte sig[] = doSign(bits.getBytes(StandardCharsets.UTF_8));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
            }  catch (InvalidKeyException | SignatureException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        protected byte[] doSign(byte[] data) throws InvalidKeyException, SignatureException {
            if (!(key instanceof PrivateKey)) {
                 // Instead of checking for an instance of RSAPrivateKey, check for PrivateKey and RSAKey:
                String msg = "RSA signatures must be computed using an RSA PrivateKey.  The specified key of type " +
                             key.getClass().getName() + " is not an RSA PrivateKey.";
                throw new SignatureException(msg);
            }
            PrivateKey privateKey = (PrivateKey)key;
            Signature sig = createSignatureInstance();
            sig.initSign(privateKey);
            sig.update(data);
            return sig.sign();
        }

        protected boolean doVerify(Signature sig, PublicKey publicKey, byte[] data, byte[] signature)
                throws InvalidKeyException, SignatureException {
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        }
    }

    static class RsaSigner extends SignatureProvider {
        RsaSigner(Algorithm alg, Key key) {
            super(alg, key);
            if (!(key instanceof RSAKey)) {
                String msg = "RSA signatures or verify must be computed using an RSA type key.  The specified key of type " +
                             key.getClass().getName() + " is not an RSA key.";
                throw new IllegalArgumentException(msg);
            }
        }

        @Override
        protected Signature createSignatureInstance() {
            Signature sig = super.createSignatureInstance();
            PSSParameterSpec spec = PSS_PARAMETER_SPECS.get(algorithm);
            if (spec != null) {
                try {
                    sig.setParameter(spec);
                } catch (InvalidAlgorithmParameterException e) {
                    throw new RuntimeException(String.format("Unsupported RSASSA-PSS parameter '%s' : %s" ,
                            spec, e.getMessage()), e);
                }
            }
            return sig;
        }

        @Override
        public boolean verify(String bits, String sig) {
            if (key instanceof PublicKey) {
                Signature signer = createSignatureInstance();
                PublicKey publicKey = (PublicKey) key;
                try {
                    signer.initVerify(publicKey);
                    signer.update(bits.getBytes(StandardCharsets.UTF_8));
                    final byte[] bytes = Base64.getUrlDecoder().decode(sig.getBytes(StandardCharsets.UTF_8));
                    return signer.verify(bytes);
                } catch (SignatureException | InvalidKeyException e) {
                    throw new RuntimeException("Unable to verify RSA signature using configured PublicKey.", e);
                }
            } else {
                // Using Private key to verify
                return sig.equals(sign(bits));
            }
        }
    }

    static class EllipticCurveSigner extends SignatureProvider {
        EllipticCurveSigner(Algorithm alg, Key key) {
            super(alg, key);
        }

        @Override
        public boolean verify(String bits, String sig) {
            Signature signer = createSignatureInstance();
            byte[] signature = sig.getBytes(StandardCharsets.UTF_8);
            byte[] data = bits.getBytes(StandardCharsets.UTF_8);
            PublicKey publicKey = (PublicKey) key;
            try {
                int expectedSize = getSignatureByteArrayLength(algorithm);
                /**
                 *
                 * If the expected size is not valid for JOSE, fall back to ASN.1 DER signature.
                 * This fallback is for backwards compatibility ONLY (to support tokens generated by previous versions of jjwt)
                 * and backwards compatibility will possibly be removed in a future version of this library.
                 *
                 * **/
                byte[] derSignature = expectedSize != signature.length && signature[0] == 0x30 ? signature : transcodeSignatureToDER(signature);
                return doVerify(signer, publicKey, data, derSignature);
            } catch (InvalidKeyException | SignatureException e) {
                String msg = "Unable to verify Elliptic Curve signature using configured ECPublicKey. " + e.getMessage();
                throw new RuntimeException(msg, e);
            }
        }

        protected byte[] doSign(byte[] data) throws InvalidKeyException, SignatureException {
            byte[] derSignature = super.doSign(data);
            return transcodeSignatureToConcat(derSignature, getSignatureByteArrayLength(algorithm));
        }

    }

    static class MacSigner extends Signer {
        Mac mac;

        MacSigner(Algorithm alg, Key key) {
            super(alg, key);
            if (!"HMAC".equals(alg.family)) {
                throw new IllegalArgumentException("The MacSigner only supports HMAC signature algorithms.");
            }
            if (!(key instanceof SecretKey)) {
                String msg = "MAC signatures must be computed and verified using a SecretKey.  The specified key of " +
                             "type " + key.getClass().getName() + " is not a SecretKey.";
                throw new IllegalArgumentException(msg);
            }
            try {
                mac = Mac.getInstance(algorithm.type);
                mac.init((SecretKeySpec)key);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }
        public String sign(String bits) {
            byte sig[] = mac.doFinal(bits.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        }

        public boolean verify(String bits, String sig) {
            return sig.equals(sign(bits));
        }
    }

    public static class Options {
        Date expires = new Date(System.currentTimeMillis() + DEFAULT_EXPIRES);
        Date notBefore;
        boolean jti;
        String issuer;
        String audience;
        String subject;
    }

    public static class Builder {
        private Signer signer = DEFAULT_SIGNER;
        private Options options = new Options();

        public Builder() {}

        public Builder key(Key key) {
            Algorithm alg = forSigningKey(key);
            signer = createSigner(alg, key);
            return this;
        }

        public Builder signer(String alg, String key) {
            signer = createSigner(alg, key);
            return this;
        }
        public Builder expires(Date d) {
            options.expires = d;
            return this;
        }
        public Builder notBefore(Date d) {
            options.notBefore = d;
            return this;
        }
        public Builder issuer(String s) {
            options.issuer = s;
            return this;
        }
        public Builder audience(String a) {
            options.audience = a;
            return this;
        }
        public Builder subject(String s) {
            options.subject = s;
            return this;
        }
        public Builder jti(boolean enable) {
            options.jti = enable;
            return this;
        }
        public JsonWebToken build(Map<String, Object> payload) {
            return new JsonWebToken(this)
                    .payload(payload);
        }
    }
}
