package mz.mobile.bussiness.utils;

import java.util.Locale;

public class EmailUtils {
    private EmailUtils() {}

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
