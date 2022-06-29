package com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignatureMethodParamChecker {
    public static void exists(String SignatureMethod) throws Exception {
        if (SignatureMethod == null) {
            throw new Exception("SignatureMethod为空");
        }
    }

    public static void signatureMethodLength(String SignatureMethodLength) throws Exception {
        if (SignatureMethodLength.length() < 8) {
            throw new Exception("SignatureMethod长度太短了");
        } else if (SignatureMethodLength.length() > 10) {
            throw new Exception("SignatureMethod长度太长了");
        }
    }

    public static void signatureMethodFormat(String SignatureMethodFormat) throws Exception {
        String pattern = "^[A-Za-z0-9]+$";

        Pattern r = Pattern.compile(pattern);

        Matcher matcher = r.matcher(SignatureMethodFormat);

        boolean rs = matcher.matches();

        if (!rs) {
            throw new Exception("SignatureMethod格式错误");
        }
    }

    public enum signatureMethodEnum {
        M_HmacSHA1("HmacSHA1", 0),
        M_HmacSHA256("HmacSHA256", 1),
        M_HmacSHA512("HmacSHA512", 2),
        M_NONE("NONE", 3);

        private String getMethod;
        public int getNum;

        signatureMethodEnum(String getMethod, int getNum) {
            this.getMethod = getMethod;
            this.getNum = getNum;
        }

        public String getGetName() {
            return getMethod;
        }

        public void setGetName(String getName) {
            this.getMethod = getName;
        }

        public int getGetValue() {
            return getNum;
        }

        public void setGetValue(int getValue) {
            this.getNum = getValue;
        }

        public static void signatureMethod(String getMethod) {
            for (signatureMethodEnum c : signatureMethodEnum.values()) {
                if (Objects.equals(c.getMethod, getMethod)) {
                    return;
                }
            }
        }
    }
}
