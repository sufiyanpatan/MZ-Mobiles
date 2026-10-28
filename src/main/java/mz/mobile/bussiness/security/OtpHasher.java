package mz.mobile.bussiness.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

// Required only when we need to print on console
@Component
public class OtpHasher {

    private final byte[] key;

    public OtpHasher(@Value("${app.otp.pepper}") String pepper) {
        if (pepper == null || pepper.length() < 32) {
            throw new IllegalStateException("app.otp.pepper must be at least 32 characters.");
        }
        this.key = pepper.getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String mobile, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            // the mobile number is part of the input: a code is only valid for its own number
            byte[] out = mac.doFinal((mobile + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean matches(String mobile, String code, String expectedHash) {
        return MessageDigest.isEqual(                       // constant-time comparison
                hash(mobile, code).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
