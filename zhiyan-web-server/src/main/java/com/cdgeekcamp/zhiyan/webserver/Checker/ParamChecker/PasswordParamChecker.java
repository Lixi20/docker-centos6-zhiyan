package com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordParamChecker {
    public static void exists(String password) throws Exception {
        if (password == null) {
            throw new Exception("Password为空");
        }
    }

    public static void checkPasswordLength(String passwordLength) throws Exception {
        if (passwordLength.length() < 6) {
            throw new Exception("Password长度太短");
        } else if (passwordLength.length() > 20) {
            throw new Exception("Password长度太长");
        }
    }

    public static void checkPasswordFormat(String passwordFormat) throws Exception {
        String pattern = "^[A-Za-z0-9]+$";

        Pattern r = Pattern.compile(pattern);

        Matcher matcher = r.matcher(passwordFormat);

        boolean rs = matcher.matches();

        if (!rs) {
            throw new Exception("Password格式错误");
        }
    }
}
