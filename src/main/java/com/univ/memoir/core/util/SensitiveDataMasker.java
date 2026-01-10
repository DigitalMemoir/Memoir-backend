package com.univ.memoir.core.util;

public class SensitiveDataMasker {

    public static String mask(String data) {
        if (data == null) {
            return null;
        }

        return data
                // JWT 토큰 마스킹
                .replaceAll("Bearer\\s+[A-Za-z0-9._-]+", "Bearer ***")
                // "apiKey": "value" → "apiKey": "***"
                .replaceAll("(?i)(\"apiKey\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                // "authorization": "value" → "authorization": "***"
                .replaceAll("(?i)(\"authorization\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                // "accessToken": "value" → "accessToken": "***"
                .replaceAll("(?i)(\"accessToken\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                // "refreshToken": "value" → "refreshToken": "***"
                .replaceAll("(?i)(\"refreshToken\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                // "email": "memo@example.com" → "email": "***"
                .replaceAll("(?i)(\"email\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")

                // password=value → password=***
                .replaceAll("(?i)(password['\"]?\\s*[:=]['\"]?)([^'\"\\s,}]+)", "$1***")
                // apiKey=value → apiKey=***
                .replaceAll("(?i)(apiKey['\"]?\\s*[:=]['\"]?)([^'\"\\s,}]+)", "$1***")
                // authorization=value → authorization=***
                .replaceAll("(?i)(authorization['\"]?\\s*[:=]['\"]?)([^'\"\\s,}]+)", "$1***")
                // accessToken=value → accessToken=***
                .replaceAll("(?i)(accessToken['\"]?\\s*[:=]['\"]?)([^'\"\\s,}]+)", "$1***")
                // refreshToken=value → refreshToken=***
                .replaceAll("(?i)(refreshToken['\"]?\\s*[:=]['\"]?)([^'\"\\s,}]+)", "$1***")
                // secret=value → secret=***
                .replaceAll("(?i)(secret['\"]?\\s*[:=]['\"]?)([^'\"\\s,}]+)", "$1***")
                // email=memo@example.com → email=***
                .replaceAll("(?i)(email['\"]?\\s*[:=]['\"]?)([^'\"\\s,}]+@[^'\"\\s,}]+)", "$1***");
    }
}