package com.cdgeekcamp.zhiyan.webserver.utils;

import java.util.Arrays;

public class GreetingRequestParam {
    String timestamp;
    String signatureMethod;
    String signatureNonce;
    String username;
    String password;
    String signature;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSignatureMethod() {
        return signatureMethod;
    }

    public void setSignatureMethod(String signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    public String getSignatureNonce() {
        return signatureNonce;
    }

    public void setSignatureNonce(String signatureNonce) {
        this.signatureNonce = signatureNonce;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String[] toSortedList() {
        String[] list = {
                "timestamp=" + timestamp,
                "signatureMethod=" + signatureMethod,
                "signatureNonce=" + signatureNonce,
                "username=" + username,
                "password=" + password,
        };

        Arrays.sort(list);

        return list;
    }
}
