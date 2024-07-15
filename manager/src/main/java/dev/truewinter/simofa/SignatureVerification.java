package dev.truewinter.simofa;

import dev.truewinter.simofa.common.Util;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SignatureVerification {
    public static boolean verifyHmacSha256(String payload, String signature, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        final String algorithm = "HmacSHA256";

        Mac hmac = Mac.getInstance(algorithm);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), algorithm);
        hmac.init(secretKeySpec);

        byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        return Util.secureCompare(signature, "sha256=" + Hex.encodeHexString(hash));
    }
}
