package com.cdgeekcamp.zhiyan.webserver.Checker;

import com.cdgeekcamp.zhiyan.webserver.Checker.ParamChecker.*;

public class URLChecker {
    public static void checkTime(String time) throws Exception {
        TimeParamChecker.exists(time);
        TimeParamChecker.checkTimeLength(time);
        TimeParamChecker.checkTimeFormat(time);
    }

    public static void checkName(String name) throws Exception {
        NameParamChecker.exists(name);
        NameParamChecker.checkNameLength(name);
        NameParamChecker.checkNameFormat(name);
    }

    public static void checkToken(String token) throws Exception {
        TokenParamChecker.exists(token);
        TokenParamChecker.checkTokenLength(token);
        TokenParamChecker.checkTokenFormat(token);
    }

    public static void checkHost(String host) throws Exception {
        HostParamChecker.exists(host);
        HostParamChecker.checkHostLength(host);
        HostParamChecker.checkHostFormat(host);
    }

    public static void checkSignature(String signature) throws Exception {
        SignatureParamChecker.exists(signature);
        SignatureParamChecker.checkSignatureLength(signature);
        SignatureParamChecker.checkSignatureFormat(signature);
    }

    public static void checkSignatureMethod(String signatureMethod) throws Exception {
        SignatureMethodParamChecker.exists(signatureMethod);
        SignatureMethodParamChecker.signatureMethodLength(signatureMethod);
        SignatureMethodParamChecker.signatureMethodFormat(signatureMethod);
        SignatureMethodParamChecker.signatureMethodEnum.signatureMethod(signatureMethod);
    }

    public static void checkTimestamp(String timestamp) throws Exception {
        TimestampParamChecker.exists(timestamp);
        TimestampParamChecker.checkTimestampLength(timestamp);
        TimestampParamChecker.checkTimestampFormat(timestamp);
    }

    public static void checkUsername(String username) throws Exception {
        UsernameParamChecker.exists(username);
        UsernameParamChecker.checkUsernameLength(username);
        UsernameParamChecker.checkUsernameFormat(username);
    }

    public static void checkPassword(String password) throws Exception {
        PasswordParamChecker.exists(password);
        PasswordParamChecker.checkPasswordLength(password);
        PasswordParamChecker.checkPasswordFormat(password);
    }

    public static void checkSignatureNonce(String signatureNonce) throws Exception {
        SignatureNonceParamChecker.exists(signatureNonce);
        SignatureNonceParamChecker.checkSignatureNonceLength(signatureNonce);
        SignatureNonceParamChecker.checkSignatureNonceFormat(signatureNonce);
    }
}


