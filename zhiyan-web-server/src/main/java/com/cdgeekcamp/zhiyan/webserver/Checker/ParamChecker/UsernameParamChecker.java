package com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsernameParamChecker {
    public static void exists(String username) throws Exception {
        if (username == null) {
            throw new Exception("Username为空");
        }
    }

    public static void checkUsernameLength(String usernameLength) throws Exception {
        if (usernameLength.length() < 1) {
            throw new Exception("Username长度太短");
        } else if (usernameLength.length() > 10) {
            throw new Exception("Username长度太长");
        }
    }

    public static void checkUsernameFormat(String usernameFormat) throws Exception {
        String pattern = "^[A-Za-z0-9]+$";

        Pattern r = Pattern.compile(pattern);

        Matcher matcher = r.matcher(usernameFormat);

        boolean rs = matcher.matches();

        if (!rs) {
            throw new Exception("Username格式错误");
        }
    }
}
