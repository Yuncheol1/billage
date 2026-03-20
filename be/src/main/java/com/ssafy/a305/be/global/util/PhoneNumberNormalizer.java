package com.ssafy.a305.be.global.util;

public final class PhoneNumberNormalizer {

    private PhoneNumberNormalizer() {
    }

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }

        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.isBlank()) {
            return null;
        }

        if (phoneNumber.trim().startsWith("+82") && digitsOnly.startsWith("82")) {
            return "0" + digitsOnly.substring(2);
        }

        return digitsOnly;
    }
}
