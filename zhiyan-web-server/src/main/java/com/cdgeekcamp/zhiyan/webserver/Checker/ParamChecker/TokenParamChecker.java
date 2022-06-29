package com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenParamChecker {
    public static void exists(String token) throws Exception {
        if (token == null) {
            throw new Exception("Token为空");
        }
    }

    public static void checkTokenLength(String tokenLength) throws Exception {
        if (tokenLength.length() < 10) {
            throw new Exception("Token长度太短");
        } else if (tokenLength.length() > 100) {
            throw new Exception("Token长度太长");
        }
    }

    public static void checkTokenFormat(String tokenFormat) throws Exception {
        String pattern = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{19,101}$";

        Pattern r = Pattern.compile(pattern);

        Matcher matcher = r.matcher(tokenFormat);

        boolean rs = matcher.matches();

        if (!rs) {
            throw new Exception("Token格式错误");
        }
    }
}
