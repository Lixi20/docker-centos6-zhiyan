package com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameParamChecker {
    public static void exists(String name) throws Exception {
        if (name == null) {
            throw new Exception("Name为空");
        }
    }

    public static void checkNameLength(String nameLength) throws Exception {
        if (nameLength.length() < 2) {
            throw new Exception("Name长度太短");
        } else if (nameLength.length() > 20) {
            throw new Exception("Name长度太长");
        }
    }

    public static void checkNameFormat(String nameFormat) throws Exception {
        String pattern = "^[A-Za-z_-]+$";

        Pattern r = Pattern.compile(pattern);

        Matcher matcher = r.matcher(nameFormat);

        boolean rs = matcher.matches();

        if (!rs) {
            throw new Exception("格式错误");
        }
    }
}










