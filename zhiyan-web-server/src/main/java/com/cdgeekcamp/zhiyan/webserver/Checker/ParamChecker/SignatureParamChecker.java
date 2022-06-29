package com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignatureParamChecker {
    public static void exists(String signature) throws Exception {
        if (signature == null) {
            throw new Exception("Signature为空");
        }
    }

    public static void checkSignatureLength(String signatureLength) throws Exception {
        if (signatureLength.length() < 10) {
            throw new Exception("Signature长度太短了");
        } else if (signatureLength.length() > 50) {
            throw new Exception("Signature长度太长了");
        }
    }

    public static void checkSignatureFormat(String signatureFormat) throws Exception {
        String pattern = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{19,101}$";

        Pattern r = Pattern.compile(pattern);

        Matcher matcher = r.matcher(signatureFormat);

        boolean rs = matcher.matches();

        if (!rs) {
            throw new Exception("Signature格式错误");
        }
    }
}
