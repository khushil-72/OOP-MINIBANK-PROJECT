package util;

import java.util.regex.Pattern;

/** Practical 4: regex validation of raw user input. */
public class Validator {
    private static final Pattern MOBILE = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PAN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern IFSC = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    private Validator() {
    }

    private static boolean matches(Pattern pattern, String text) {
        return text != null && pattern.matcher(text).matches();
    }

    public static boolean isValidMobile(String mobile) {
        return matches(MOBILE, mobile);
    }

    public static boolean isValidEmail(String email) {
        return matches(EMAIL, email);
    }

    public static boolean isValidPan(String pan) {
        return matches(PAN, pan);
    }

    public static boolean isValidIfsc(String ifsc) {
        return matches(IFSC, ifsc);
    }
}
