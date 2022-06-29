package com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HostParamChecker {
    public static void exists(String host) throws Exception {
        if (host == null) {
            throw new Exception("Host为空");
        }
    }

    public static void checkHostLength(String hostLength) throws Exception {
        if (hostLength.length() < 7) {
            throw new Exception("Host长度太短");
        } else if (hostLength.length() > 15) {
            throw new Exception("Host长度太长");
        }
    }

    public static void checkHostFormat(String hostFormat) throws Exception {
        String[] temp;
        String delimeter = "\\.";
        temp = hostFormat.split(delimeter);
        if (temp.length != 4) {
            throw new Exception("Host格式错误");
        }
        for (String x : temp) {
            if (x.length() > 3) {
                throw new Exception("Host格式错误");
            }
            String pattern = "^[0-9]*$";

            Pattern r = Pattern.compile(pattern);

            Matcher matcher = r.matcher(x);
            boolean rs = matcher.matches();

            if (!rs) {
                throw new Exception("Host长度错误");
            }
        }
    }
}

