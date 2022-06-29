package com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignatureNonceParamChecker {
    public static void exists(String signatureNonce) throws Exception {
        if (signatureNonce == null) {
            throw new Exception("SignatureNonce为空");
        }
    }

    public static void checkSignatureNonceLength(String signatureNonceLength) throws Exception {
        if (signatureNonceLength.length() < 30) {
            throw new Exception("SignatureNonceLength长度太短了");
        } else if (signatureNonceLength.length() > 50) {
            throw new Exception("SignatureNonce长度太长了");
        }
    }

    public static void checkSignatureNonceFormat(String signatureNonceFormat) throws Exception {
        String pattern = "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}";

        Pattern r = Pattern.compile(pattern);

        Matcher matcher = r.matcher(signatureNonceFormat);

        boolean rs = matcher.matches();

        if (!rs) {
            throw new Exception("SignatureNonceFormat格式错误");
        }
    }
}
