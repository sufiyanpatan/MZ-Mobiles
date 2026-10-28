package mz.mobile.bussiness.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MobileUtils {

    private static final Pattern INDIA = Pattern.compile("^(?:\\+91|91|0)?([6-9]\\d{9})$");

    private MobileUtils() {}

    public static String normalize(String raw) {
        String cleaned = raw == null ? "" : raw.replaceAll("[\\s\\-()]", "");
        Matcher m = INDIA.matcher(cleaned);
        if (!m.matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a valid 10-digit Indian mobile number");
        }
        return "+91" + m.group(1);
    }
}
