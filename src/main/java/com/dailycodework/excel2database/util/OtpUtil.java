package com.dailycodework.excel2database.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OtpUtil {

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private static final int EXPIRY_MINUTES = 10;

    public String generateOtp(String email) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        otpStore.put(email, new OtpEntry(otp, LocalDateTime.now().plusMinutes(EXPIRY_MINUTES)));

        log.info("🔐 OTP for {} is {} (valid for {} mins)", email, otp, EXPIRY_MINUTES);
        return otp;
    }

    public String getOtpForEmail(String email) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null) return null;

        if (entry.expiry.isBefore(LocalDateTime.now())) {
            otpStore.remove(email);
            log.warn("⚠️ OTP for {} has expired.", email);
            return null;
        }
        return entry.otp;
    }

    public void clearOtp(String email) {
        otpStore.remove(email);
        log.info("🧹 OTP cleared for {}", email);
    }

    private static class OtpEntry {
        String otp;
        LocalDateTime expiry;

        OtpEntry(String otp, LocalDateTime expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }
    }
}
