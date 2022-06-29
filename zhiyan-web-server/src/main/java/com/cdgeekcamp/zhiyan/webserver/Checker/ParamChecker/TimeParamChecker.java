package com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParamChecker {
    public static void exists(String time) throws Exception {
        if (time == null) {
            throw new Exception("Time为空");
        }
    }

    public static void checkTimeLength(String timeLength) throws Exception {
        if (timeLength.length() > 16) {
            throw new Exception("Time长度太长");
        }
    }

    public static void checkTimeFormat(String timeFormat) throws Exception {
        String pattern = "^[0-9]*$";

        Pattern r = Pattern.compile(pattern);

        Matcher matcher = r.matcher(timeFormat);

        boolean rs = matcher.matches();

        if (!rs) {
            throw new Exception("Time格式错误");
        }
    }
}










