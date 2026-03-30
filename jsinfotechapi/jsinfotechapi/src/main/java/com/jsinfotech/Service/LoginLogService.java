package com.jsinfotech.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.LoginLog;

@Service
public class LoginLogService {
	
	@Autowired
	JdbcTemplate jdbcTemplate;

	public void createLoginLog(String userId, String phoneNumber) {
		// Store login/logout times as IST wall-clock time (not NOW() which uses DB/server timezone).
		String istNow = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
			ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
		);
		jdbcTemplate.update(
				"INSERT INTO login_logs (user_id, phone_number, login_time, is_logged_out) VALUES (?, ?, ?, ?)",
				userId, phoneNumber, istNow, false);
	}

	public void updateLogout(String userId, String phoneNumber) {
		// Store login/logout times as IST wall-clock time (not NOW() which uses DB/server timezone).
		String istNow = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
			ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
		);
		jdbcTemplate.update(
				"UPDATE login_logs SET logout_time = ?, is_logged_out = ? WHERE user_id = ? AND phone_number = ? AND is_logged_out = ?",
				istNow, true, userId, phoneNumber, false);
	}

	public LoginLog getLatestLoginLog(String userId, String phoneNumber) {
		try {
			List<LoginLog> logs = jdbcTemplate.query(
					"SELECT * FROM login_logs WHERE user_id = ? AND phone_number = ? ORDER BY login_time DESC LIMIT 1",
					new LoginLogRowMapper(),
					userId, phoneNumber);
			
			if (logs != null && !logs.isEmpty()) {
				return logs.get(0);
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public boolean hasActiveSession(String userId, String phoneNumber) {
		LoginLog latestLog = getLatestLoginLog(userId, phoneNumber);
		return latestLog != null && !latestLog.isLoggedOut();
	}

	private static class LoginLogRowMapper implements RowMapper<LoginLog> {
		@Override
		public LoginLog mapRow(ResultSet rs, int rowNum) throws SQLException {
			LoginLog log = new LoginLog();
			log.setId(rs.getInt("id"));
			log.setUserId(rs.getString("user_id"));
			log.setPhoneNumber(rs.getString("phone_number"));
			log.setLoginTime(rs.getTimestamp("login_time"));
			java.sql.Timestamp logoutTime = rs.getTimestamp("logout_time");
			if (logoutTime != null) {
				log.setLogoutTime(new Date(logoutTime.getTime()));
			}
			log.setLoggedOut(rs.getBoolean("is_logged_out"));
			return log;
		}
	}
}
