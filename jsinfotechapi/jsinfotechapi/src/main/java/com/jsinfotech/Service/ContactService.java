package com.jsinfotech.Service;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Contact;

@Service
public class ContactService {
	
	@Autowired
	JdbcTemplate jdbcTemplate;

	public void add(Contact contact) {
		jdbcTemplate.update("INSERT INTO contacts (name, phone_number) VALUES (?, ?)",
				contact.getName(), contact.getPhoneNumber());
	}

	public boolean phoneNumberExists(String phoneNumber) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM contacts WHERE phone_number = ?", 
				Integer.class, phoneNumber);
		return count != null && count > 0;
	}

	public Contact getContactByPhoneNumber(String phoneNumber) {
		try {
			return jdbcTemplate.queryForObject(
					"SELECT * FROM contacts WHERE phone_number = ?",
					new ContactRowMapper(),
					phoneNumber);
		} catch (Exception e) {
			return null;
		}
	}

	private static class ContactRowMapper implements RowMapper<Contact> {
		@Override
		public Contact mapRow(ResultSet rs, int rowNum) throws SQLException {
			Contact contact = new Contact();
			contact.setId(rs.getInt("id"));
			contact.setName(rs.getString("name"));
			contact.setPhoneNumber(rs.getString("phone_number"));
			return contact;
		}
	}
}
