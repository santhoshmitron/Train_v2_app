package com.jsinfotech.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class OtpService {
	
	// Store OTPs temporarily in memory with expiration
	// Key: phoneNumber_userId, Value: OtpData (otp and timestamp)
	private static final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();
	private static final long OTP_EXPIRATION_TIME = 5 * 60 * 1000; // 5 minutes in milliseconds
	
	private static class OtpData {
		String otp;
		long timestamp;
		
		OtpData(String otp, long timestamp) {
			this.otp = otp;
			this.timestamp = timestamp;
		}
	}

	public String generateOtp() {
		Random random = new Random();
		int otp = 100000 + random.nextInt(900000); // 6-digit OTP
		return String.valueOf(otp);
	}

	public void storeOtp(String phoneNumber, String userId, String otp) {
		String key = phoneNumber + "_" + userId;
		otpStore.put(key, new OtpData(otp, System.currentTimeMillis()));
	}

	public boolean validateOtp(String phoneNumber, String userId, String otp) {
		String key = phoneNumber + "_" + userId;
		OtpData otpData = otpStore.get(key);
		
		if (otpData == null) {
			return false;
		}
		
		// Check if OTP has expired
		long currentTime = System.currentTimeMillis();
		if (currentTime - otpData.timestamp > OTP_EXPIRATION_TIME) {
			otpStore.remove(key);
			return false;
		}
		
		// Validate OTP
		if (otpData.otp.equals(otp)) {
			otpStore.remove(key); // Remove OTP after successful validation
			return true;
		}
		
		return false;
	}

	public void removeOtp(String phoneNumber, String userId) {
		String key = phoneNumber + "_" + userId;
		otpStore.remove(key);
	}

	public String getExistingOtp(String phoneNumber, String userId) {
		String key = phoneNumber + "_" + userId;
		OtpData otpData = otpStore.get(key);
		
		if (otpData == null) {
			return null;
		}
		
		// Check if OTP has expired
		long currentTime = System.currentTimeMillis();
		if (currentTime - otpData.timestamp > OTP_EXPIRATION_TIME) {
			otpStore.remove(key);
			return null;
		}
		
		// Return existing valid OTP
		return otpData.otp;
	}

	// Clean up expired OTPs periodically (optional, can be called by scheduler)
	public void cleanupExpiredOtps() {
		long currentTime = System.currentTimeMillis();
		otpStore.entrySet().removeIf(entry -> 
			currentTime - entry.getValue().timestamp > OTP_EXPIRATION_TIME
		);
	}
}
