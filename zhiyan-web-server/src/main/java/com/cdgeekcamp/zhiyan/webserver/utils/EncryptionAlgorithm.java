package com.cdgeekcamp.zhiyan.webserver.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class EncryptionAlgorithm {
    public static String Signature(String encrypt, String expectedAccessKeySecret, StringBuilder stringToSign)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(encrypt);
        mac.init(new SecretKeySpec(expectedAccessKeySecret.getBytes(StandardCharsets.UTF_8), encrypt));
        byte[] SignData = mac.doFinal(String.valueOf(stringToSign).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(SignData);
    }
}
