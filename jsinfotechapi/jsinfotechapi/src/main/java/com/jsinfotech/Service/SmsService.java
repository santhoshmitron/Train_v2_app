package com.jsinfotech.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
	
	private static final Logger logger = LogManager.getLogger(SmsService.class);
	
	@Autowired
	private RedisUserRepository userRepository;
	
	@Value("${sms.api.url}")
	private String smsApiUrl;
	
	@Value("${sms.api.username}")
	private String smsUsername;
	
	@Value("${sms.api.key}")
	private String smsApiKey;
	
	@Value("${sms.api.senderid}")
	private String smsSenderId;
	
	@Value("${sms.api.entityid}")
	private String smsEntityId;
	
	@Value("${sms.api.templateid}")
	private String smsTemplateId;
	
	@Value("${sms.api.template}")
	private String smsTemplate;
	
	@Value("${sms.api.otp.expiry.minutes:5}")
	private long otpExpiryMinutes;
	
	@Value("${sms.api.default.country.code:}")
	private String defaultCountryCode;
	
	/**
	 * Generate a 4-digit OTP
	 * @return 4-digit OTP as string
	 */
	private String generateOTP() {
		Random random = new Random();
		int otp = 1000 + random.nextInt(9000); // Generates number between 1000 and 9999
		return String.valueOf(otp);
	}
	
	/**
	 * Disable SSL certificate validation (for SMS gateway SSL issues)
	 * This is a workaround for SSL certificate validation errors
	 */
	private void disableSSLVerification() {
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}
					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}
					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				}
			};
			
			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			
			// Also disable hostname verification
			HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
		} catch (Exception e) {
			logger.warn("Error disabling SSL verification: {}", e.getMessage());
		}
	}

	/**
	 * Format phone number with country code if needed
	 * User provides: "9880020224" -> System maps to: "919880020224" (adds +91)
	 * User provides: "+919880020224" -> System maps to: "919880020224" (removes +)
	 * User provides: "919880020224" -> System maps to: "919880020224" (as-is)
	 * @param phoneNumber Original phone number from user
	 * @return Formatted phone number with country code (without + sign)
	 */
	private String formatPhoneNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			return phoneNumber;
		}
		
		// Remove any spaces, dashes, or other non-digit characters except +
		String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
		
		// If phone number starts with +, remove it first
		if (cleaned.startsWith("+")) {
			cleaned = cleaned.substring(1);
		}
		
		// If default country code is configured
		if (defaultCountryCode != null && !defaultCountryCode.trim().isEmpty()) {
			// Check if phone number already starts with country code
			if (cleaned.startsWith(defaultCountryCode)) {
				// Already has country code, return as-is
				logger.info("Phone number already has country code: {} -> {}", phoneNumber, cleaned);
				return cleaned;
			}
			
			// For Indian numbers: if it's 10 digits (like 9880020224), add country code 91
			// For other countries, adjust logic as needed
			if (cleaned.length() == 10 && defaultCountryCode.equals("91")) {
				// 10-digit Indian number without country code - add 91
				String formatted = defaultCountryCode + cleaned;
				logger.info("Formatted phone number: {} -> {} (added country code +{})", 
					phoneNumber, formatted, defaultCountryCode);
				return formatted;
			} else if (cleaned.length() >= 10 && cleaned.length() <= 12) {
				// Phone number without country code - add it
				String formatted = defaultCountryCode + cleaned;
				logger.info("Formatted phone number: {} -> {} (added country code +{})", 
					phoneNumber, formatted, defaultCountryCode);
				return formatted;
			}
		}
		
		// Return cleaned number as-is if no country code configured or number format doesn't match
		logger.info("Using phone number as-is: {} -> {}", phoneNumber, cleaned);
		return cleaned;
	}
	
	public boolean sendOtp(String phoneNumber) {
		try {
			// Generate 4-digit OTP locally
			String otp = generateOTP();
			logger.info("Generated 4-digit OTP for phone number: {}", phoneNumber);
			
			// Format phone number with country code if needed
			String formattedPhoneNumber = formatPhoneNumber(phoneNumber);
			logger.info("Original phone: {}, Formatted phone: {}", phoneNumber, formattedPhoneNumber);
			
			// Replace template placeholder {#var#} with generated OTP
			String message = smsTemplate.replace("{#var#}", otp);
			
			// Build GET request URL with query parameters according to textsms.org PUSH API documentation
			// Sample format: https://textsms.org/pushapi/sendmsg?username=jsinfo&dest=9901491200&apikey=...&signature=JSIPVT&msgtype=PM&msgtxt=...&entityid=1701177122957003164&templateid=1707177131575765996
			// IMPORTANT: entityid and templateid are REQUIRED for SMS delivery
			
			// URL encode parameters - include entityid and templateid as shown in sample API URL
			String queryString = String.format(
				"username=%s&dest=%s&apikey=%s&signature=%s&msgtype=PM&msgtxt=%s&entityid=%s&templateid=%s",
				java.net.URLEncoder.encode(smsUsername, "UTF-8"),
				java.net.URLEncoder.encode(formattedPhoneNumber, "UTF-8"),
				java.net.URLEncoder.encode(smsApiKey, "UTF-8"),
				java.net.URLEncoder.encode(smsSenderId, "UTF-8"),
				java.net.URLEncoder.encode(message, "UTF-8"),
				java.net.URLEncoder.encode(smsEntityId, "UTF-8"),
				java.net.URLEncoder.encode(smsTemplateId, "UTF-8")
			);
			
			String fullUrl = smsApiUrl + "?" + queryString;
			logger.info("Sending SMS to textsms.org - URL: {}", fullUrl.replace(smsApiKey, "***"));
			
			// Disable SSL verification to handle certificate issues
			disableSSLVerification();
			
			// Make GET request
			URL url = new URL(fullUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(10000); // 10 seconds
			connection.setReadTimeout(10000); // 10 seconds
			
			int responseCode = connection.getResponseCode();
			
			// Read response
			BufferedReader in;
			if (responseCode >= 200 && responseCode < 300) {
				in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
			} else {
				in = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
			}
			
			String inputLine;
			StringBuilder response = new StringBuilder();
			
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			connection.disconnect();
			
			// Log response for debugging
			logger.info("SMS API Response (Code: {}): {}", responseCode, response.toString());
			
			// Parse JSON response to check for success code "6001"
			// Success response: {"code":"6001","desc":"Message received by platform.",...}
			// Error response: {"code":"110","desc":"INVALID_DESTINATION_NUMBER",...}
			String responseStr = response.toString();
			boolean isSuccess = false;
			
			if (responseCode >= 200 && responseCode < 300) {
				// Check for success code "6001" in response
				if (responseStr.contains("\"code\":\"6001\"") || responseStr.contains("\"code\":6001")) {
					isSuccess = true;
					logger.info("SMS sent successfully! Response code: 6001");
				} else if (responseStr.contains("\"code\"")) {
					// Extract error code and description
					logger.error("SMS API returned error code. Response: {}", responseStr);
				}
			}
			
			if (isSuccess) {
				// Store OTP in Redis for verification (use original phone number for storage)
				userRepository.saveOTP(phoneNumber, otp, otpExpiryMinutes);
				
				// Extract request ID from response for tracking
				String reqId = "N/A";
				try {
					if (responseStr.contains("\"reqId\"")) {
						int reqIdStart = responseStr.indexOf("\"reqId\":\"") + 9;
						int reqIdEnd = responseStr.indexOf("\"", reqIdStart);
						if (reqIdEnd > reqIdStart) {
							reqId = responseStr.substring(reqIdStart, reqIdEnd);
						}
					}
				} catch (Exception e) {
					logger.debug("Could not extract reqId from response: {}", e.getMessage());
				}
				
				logger.info("SMS API accepted request successfully! Response code: 6001");
				logger.info("Phone number: {} (formatted: {}), Request ID: {}, OTP: {}", 
					phoneNumber, formattedPhoneNumber, reqId, otp);
				logger.info("OTP stored in Redis for verification. Expiry: {} minutes", otpExpiryMinutes);
				logger.warn("======================================================================");
				logger.warn("IMPORTANT: If SMS is not received, please check the following:");
				logger.warn("======================================================================");
				logger.warn("1. SENDER ID APPROVAL:");
				logger.warn("   - Login to textsms.org panel: https://textsms.org/");
				logger.warn("   - Verify that sender ID '{}' is APPROVED and ACTIVE", smsSenderId);
				logger.warn("   - Unapproved sender IDs will be accepted by API but SMS won't be delivered");
				logger.warn("");
				logger.warn("2. DELIVERY STATUS CHECK:");
				logger.warn("   - Check delivery reports in textsms.org dashboard");
				logger.warn("   - Use Request ID: {} to track this specific SMS", reqId);
				logger.warn("   - Look for delivery status: Delivered, Failed, Pending, etc.");
				logger.warn("");
				logger.warn("3. PHONE NUMBER ISSUES:");
				logger.warn("   - Verify phone number {} is correct and active", formattedPhoneNumber);
				logger.warn("   - Check if number is in DND (Do Not Disturb) registry");
				logger.warn("   - DND numbers may block promotional/transactional SMS");
				logger.warn("");
				logger.warn("4. ACCOUNT SETTINGS:");
				logger.warn("   - Verify account has sufficient balance/credits");
				logger.warn("   - Check if account is in test mode (test mode may not deliver SMS)");
				logger.warn("   - Verify API key permissions allow sending SMS");
				logger.warn("");
				logger.warn("5. CARRIER BLOCKING:");
				logger.warn("   - Some carriers block SMS from unverified sender IDs");
				logger.warn("   - Contact textsms.org support if sender ID is approved but SMS not delivered");
				logger.warn("======================================================================");
				return true;
			} else {
				logger.error("SMS API Error (Code: {}): {}", responseCode, responseStr);
				// Store OTP anyway for testing purposes (remove in production if needed)
				userRepository.saveOTP(phoneNumber, otp, otpExpiryMinutes);
				logger.warn("OTP stored in Redis for manual testing. OTP: {}", otp);
				return false;
			}
		} catch (Exception e) {
			logger.error("Error sending SMS to phone number: {}", phoneNumber, e);
			return false;
		}
	}

	public boolean verifyOtp(String phoneNumber, String otp) {
		try {
			// Validate input
			if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
				logger.warn("Phone number is null or empty for OTP verification");
				return false;
			}
			
			if (otp == null || otp.trim().isEmpty()) {
				logger.warn("OTP is null or empty for phone number: {}", phoneNumber);
				return false;
			}
			
			// Get stored OTP from Redis
			String storedOtp = userRepository.getOTP(phoneNumber);
			
			if (storedOtp == null || storedOtp.isEmpty()) {
				logger.warn("No OTP found in Redis for phone number: {} (may have expired)", phoneNumber);
				return false;
			}
			
			// Compare OTPs (case-insensitive, trim whitespace)
			boolean isValid = storedOtp.trim().equals(otp.trim());
			
			if (isValid) {
				// OTP verified successfully - delete it from Redis to prevent reuse
				userRepository.deleteOTP(phoneNumber);
				logger.info("OTP verified successfully for phone number: {}", phoneNumber);
				return true;
			} else {
				logger.warn("Invalid OTP provided for phone number: {} (stored: {}, provided: {})", 
					phoneNumber, storedOtp, otp);
				return false;
			}
		} catch (Exception e) {
			logger.error("Error verifying OTP for phone number: {}", phoneNumber, e);
			return false;
		}
	}
}
