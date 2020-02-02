package net.tiny.ws.auth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.spec.SecretKeySpec;

public class Keys {

    public static String encodeSSHPublicKey(final PublicKey publicKey, final String issuer) {
        if (publicKey instanceof RSAPublicKey) {
            return encodeRSAPublicKey((RSAPublicKey) publicKey, issuer);

        } else if (publicKey instanceof DSAPublicKey) {
            return encodeDSAPublicKey((DSAPublicKey) publicKey, issuer);

        } else if (publicKey instanceof ECPublicKey) {
            final ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
            Optional<String> ret = decideEcIdentifier(ecPublicKey.getParams())
                    .map(identifier -> encodeEcPublicKey(identifier, ecPublicKey, issuer));
            return ret.orElse(null);

        } else {
            return null;
        }
    }

    public static String encodeRSAPublicKey(final RSAPublicKey publicKey, final String issuer) {
        final String sig = "ssh-rsa";
        final byte[] sigBytes = sig.getBytes(StandardCharsets.US_ASCII);
        final byte[] eBytes = publicKey.getPublicExponent().toByteArray();
        final byte[] nBytes = publicKey.getModulus().toByteArray();

        final int size = 4 + sigBytes.length
                + 4 + eBytes.length
                + 4 + nBytes.length;

        final byte[] keyBytes = ByteBuffer.allocate(size)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(sigBytes.length).put(sigBytes)
                .putInt(eBytes.length).put(eBytes)
                .putInt(nBytes.length).put(nBytes)
                .array();

        final String base64 = Base64.getEncoder()
                .encodeToString(keyBytes);

        return sig + " " + base64 + (issuer != null ? (" " + issuer) : "");
    }

    public static String encodeDSAPublicKey(final DSAPublicKey publicKey, final String issuer) {
        final String sig = "ssh-dss";
        final byte[] sigBytes = sig.getBytes(StandardCharsets.US_ASCII);
        final DSAParams params = publicKey.getParams();
        final byte[] pBytes = params.getP().toByteArray();
        final byte[] qBytes = params.getQ().toByteArray();
        final byte[] gBytes = params.getG().toByteArray();
        final byte[] yBytes = publicKey.getY().toByteArray();

        final int size = 4 + sigBytes.length
                + 4 + pBytes.length
                + 4 + qBytes.length
                + 4 + gBytes.length
                + 4 + yBytes.length;

        final byte[] keyBytes = ByteBuffer.allocate(size)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(sigBytes.length).put(sigBytes)
                .putInt(pBytes.length).put(pBytes)
                .putInt(qBytes.length).put(qBytes)
                .putInt(gBytes.length).put(gBytes)
                .putInt(yBytes.length).put(yBytes)
                .array();

        final String base64 = Base64.getEncoder()
                .encodeToString(keyBytes);

        return sig + " " + base64 + (issuer != null ? (" " + issuer) : "");
    }

    public static String encodeEcPublicKey(final String identifier, final ECPublicKey publicKey, final String issuer) {

        final String sig = "ecdsa-sha2-" + identifier;
        final BigInteger x = publicKey.getW().getAffineX();
        final BigInteger y = publicKey.getW().getAffineY();

        final byte[] sigBytes = sig.getBytes(StandardCharsets.US_ASCII);
        final byte[] identifierBytes = identifier.getBytes(StandardCharsets.US_ASCII);
        final byte[] xBytes = x.toByteArray();
        final byte[] yBytes = y.toByteArray();

        final int size = 4 + sigBytes.length
                + 4 + identifierBytes.length
                + 4 + 1 + xBytes.length + yBytes.length;

        final byte[] keyBytes = ByteBuffer.allocate(size)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(sigBytes.length).put(sigBytes)
                .putInt(identifierBytes.length).put(identifierBytes)
                .putInt(1 + xBytes.length + yBytes.length)
                .put((byte) 0x04)
                .put(xBytes)
                .put(yBytes)
                .array();

        final String base64 = Base64.getEncoder()
                .encodeToString(keyBytes);

        return sig + " " + base64 + (issuer != null ? (" " + issuer) : "");
    }

    private static Optional<String> decideEcIdentifier(final ECParameterSpec params) {

        if (!(params.getCurve().getField() instanceof ECFieldFp)) {
            return Optional.empty();
        }
        final ECFieldFp field = (ECFieldFp) params.getCurve().getField();
        final BigInteger p = field.getP();
        final BigInteger a = params.getCurve().getA();
        final BigInteger b = params.getCurve().getB();
        final BigInteger g1 = params.getGenerator().getAffineX();
        final BigInteger g2 = params.getGenerator().getAffineY();
        final BigInteger n = params.getOrder();
        final int h = params.getCofactor();

        final List<Number> tuple = Arrays.asList(p, a, b, g1, g2, n, h);
        final String identifier = identifierMap.get(tuple);
        return Optional.ofNullable(identifier);
    }

    private static final Map<List<Number>, String> identifierMap;
    static {
        final Map<List<Number>, String> map = new LinkedHashMap<>();

        map.put(Arrays.asList(
                new BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853951"),
                new BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853948"),
                new BigInteger("41058363725152142129326129780047268409114441015993725554835256314039467401291"),
                new BigInteger("48439561293906451759052585252797914202762949526041747995844080717082404635286"),
                new BigInteger("36134250956749795798585127919587881956611106672985015071877198253568414405109"),
                new BigInteger("115792089210356248762697446949407573529996955224135760342422259061068512044369"),
                1), "nistp256");
        map.put(Arrays.asList(
                new BigInteger(
                        "39402006196394479212279040100143613805079739270465446667948293404245721771496870329047266088258938001861606973112319"),
                new BigInteger(
                        "39402006196394479212279040100143613805079739270465446667948293404245721771496870329047266088258938001861606973112316"),
                new BigInteger(
                        "27580193559959705877849011840389048093056905856361568521428707301988689241309860865136260764883745107765439761230575"),
                new BigInteger(
                        "26247035095799689268623156744566981891852923491109213387815615900925518854738050089022388053975719786650872476732087"),
                new BigInteger(
                        "8325710961489029985546751289520108179287853048861315594709205902480503199884419224438643760392947333078086511627871"),
                new BigInteger(
                        "39402006196394479212279040100143613805079739270465446667946905279627659399113263569398956308152294913554433653942643"),
                1), "nistp384");
        map.put(Arrays.asList(
                new BigInteger(
                        "6864797660130609714981900799081393217269435300143305409394463459185543183397656052122559640661454554977296311391480858037121987999716643812574028291115057151"),
                new BigInteger(
                        "6864797660130609714981900799081393217269435300143305409394463459185543183397656052122559640661454554977296311391480858037121987999716643812574028291115057148"),
                new BigInteger(
                        "1093849038073734274511112390766805569936207598951683748994586394495953116150735016013708737573759623248592132296706313309438452531591012912142327488478985984"),
                new BigInteger(
                        "2661740802050217063228768716723360960729859168756973147706671368418802944996427808491545080627771902352094241225065558662157113545570916814161637315895999846"),
                new BigInteger(
                        "3757180025770020463545507224491183603594455134769762486694567779615544477440556316691234405012945539562144444537289428522585666729196580810124344277578376784"),
                new BigInteger(
                        "6864797660130609714981900799081393217269435300143305409394463459185543183397655394245057746333217197532963996371363321113864768612440380340372808892707005449"),
                1), "nistp521");
        identifierMap = Collections.unmodifiableMap(map);
    }

    private static final int VALUE_LENGTH = 4;
    private static final byte[] INITIAL_RSA_PREFIX = new byte[]{0x00, 0x00, 0x00, 0x07, 0x73, 0x73, 0x68, 0x2d, 0x72, 0x73, 0x61};

    public static RSAPublicKey decodeSSHPublicKey(String sshKey) {
        // SSH-RSA key format
        //
        //            00 00 00 07             The length in bytes of the next field
        //            73 73 68 2d 72 73 61    The key type (ASCII encoding of "ssh-rsa")
        //            00 00 00 03             The length in bytes of the public exponent
        //            01 00 01                The public exponent (usually 65537, as here)
        //            00 00 01 01             The length in bytes of the modulus (here, 257)
        //            00 c3 a3...             The modulus
        final String[] array = sshKey.split(" ");
        if (array.length < 2) {
            throw new IllegalArgumentException("Key format is invalid for SSH RSA.");
        }
        final ByteArrayInputStream stream = new ByteArrayInputStream(Base64.getMimeDecoder().decode(array[1]));
        byte[] prefix = new byte[INITIAL_RSA_PREFIX.length];
        try {
            if (INITIAL_RSA_PREFIX.length != stream.read(prefix) && Arrays.equals(INITIAL_RSA_PREFIX, prefix)) {
                throw new IllegalArgumentException("Initial [ssh-rsa] key prefix missed.");
            }
            final BigInteger exponent = getValue(stream);
            final BigInteger modulus  = getValue(stream);
            stream.close();
            final RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported RSA private key format : " + e.getMessage());
        }
    }

    private static BigInteger getValue(InputStream is) throws IOException {
        byte[] lenBuff = new byte[VALUE_LENGTH];
        if (VALUE_LENGTH != is.read(lenBuff)) {
            throw new IOException("Unable to read value length.");
        }

        int len = ByteBuffer.wrap(lenBuff).getInt();
        byte[] valueArray = new byte[len];
        if (len != is.read(valueArray)) {
            throw new IOException("Unable to read value.");
        }
        return new BigInteger(valueArray);
    }

    public static String encodeKey(final Key key) {
        Optional<String> ret = Optional.empty();
        if (key instanceof PublicKey) {
            ret = encodePublicKey((PublicKey)key);
        } else if (key instanceof PrivateKey) {
            ret = encodePrivateKey((PrivateKey)key);
        }
        return ret.orElse(null);
    }

    protected static Optional<String> encodePublicKey(final PublicKey publicKey) {
        if (!(publicKey instanceof RSAPublicKey
                || publicKey instanceof DSAPublicKey
                || publicKey instanceof ECPublicKey)) {
            return Optional.empty();
        }

        final String base64 = Base64.getMimeEncoder()
                .encodeToString(publicKey.getEncoded());
        // Add BEGIN and END comments
        final StringBuilder encoded = new StringBuilder()
                .append( "-----BEGIN PUBLIC KEY-----\n")
                .append(base64)
                .append("\n-----END")
                .append(" PUBLIC KEY-----");
        return Optional.of(encoded.toString());
    }

    protected static Optional<String> encodePrivateKey(final PrivateKey privateKey) {
        final String keyType;
        if (privateKey instanceof RSAPrivateKey) {
            keyType = "RSA";
        } else if (privateKey instanceof DSAPrivateKey) {
            keyType = "DSA";
        } else if (privateKey instanceof ECPrivateKey) {
            keyType = "EC";
        } else {
            return Optional.empty();
        }

        final String base64 = Base64.getMimeEncoder()
                .encodeToString(privateKey.getEncoded());
        // Add BEGIN and END comments
        final StringBuilder encoded = new StringBuilder()
                .append( "-----BEGIN ")
                .append(keyType)
                .append(" PRIVATE KEY-----\n")
                .append(base64)
                .append("\n-----END ")
                .append(keyType)
                .append(" PRIVATE KEY-----");
        return Optional.of(encoded.toString());
    }

    public static PublicKey decodeRSAPublicKey(String publicKey){
        // Remove BEGIN and END comments
        final String key = publicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+","");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Unsupported RSA public key format : " + e.getMessage());
        }
    }

    public static PrivateKey decodeRSAPrivateKey(String privateKey) {
        // Remove BEGIN and END comments
        final String key = privateKey
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+","");
        byte[] keyBytes = Base64.getMimeDecoder().decode(key);
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Unsupported RSA private key format : " + e.getMessage());
        }
    }

    public static Key decodeRSAKey(String key) {
        if (key.contains("RSA PRIVATE KEY")) {
            return decodeRSAPrivateKey(key);
        } else if (key.contains("PUBLIC KEY")) {
            return decodeRSAPublicKey(key);
        } else {
            throw new IllegalArgumentException("Unsupported RSA key format");
        }
    }

    public static Key decodeHMACKey(String key) {
        if (key.length() < 40) {
            return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        } else if (key.length() > 60) {
            return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        } else {
            return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA384");
        }
    }

    public static Key decodeEllipticCurveKey(String key) {
        //TODO
        return null;
    }

}
