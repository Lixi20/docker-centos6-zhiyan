package com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimestampParamChecker {
    public static void exists(String timestamp) throws Exception {
        if (timestamp == null) {
            throw new Exception("Timestamp为空");
        }
    }

    public static void checkTimestampLength(String timestampLength) throws Exception {
        if (timestampLength.length() < 10) {
            throw new Exception("Timestamp长度太短了");
        } else if (timestampLength.length() > 30) {
            throw new Exception("Timestamp长度太长了");
        }
    }

    public static void checkTimestampFormat(String timestampFormat) throws Exception {
        String pattern = "^[0-9]*$";

        Pattern r = Pattern.compile(pattern);

        Matcher matcher = r.matcher(timestampFormat);

        boolean rs = matcher.matches();

        if (!rs) {
            throw new Exception("Timestamp格式错误");
        }
    }
}
