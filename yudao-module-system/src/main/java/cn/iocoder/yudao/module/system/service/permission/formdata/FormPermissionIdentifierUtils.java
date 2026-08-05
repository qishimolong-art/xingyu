package cn.iocoder.yudao.module.system.service.permission.formdata;

import java.util.regex.Pattern;

public final class FormPermissionIdentifierUtils {

    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,63}$");

    private FormPermissionIdentifierUtils() {
    }

    public static void checkIdentifier(String value, String name) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(name + "格式不正确: " + value);
        }
    }

    public static String quote(String identifier) {
        checkIdentifier(identifier, "标识符");
        return "`" + identifier + "`";
    }

}
