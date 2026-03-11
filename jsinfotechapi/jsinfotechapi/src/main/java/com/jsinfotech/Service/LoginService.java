package com.jsinfotech.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Customers;
import com.jsinfotech.Domain.GateManStationMaster;
import com.jsinfotech.Domain.Helpline;
import com.jsinfotech.Domain.Login;

@Service
public class LoginService {

	@Autowired
	JdbcTemplate jdbcTemplate;

	public Login checkLogin(Login login) {

		Customers cust	= jdbcTemplate.queryForObject("select * from customers where username=? and password=?", new Object[] { login.getUsername(),login.getPassword() },
				new BeanPropertyRowMapper<Customers>(Customers.class));
		 
		Login lo = new Login();
		if(cust != null) {
			lo.setId(cust.getId());
			lo.setMessage("Login Successfull");
			lo.setRole(cust.getRoles());
			lo.setUsername(login.getUsername());
			lo.setName(cust.getName());
			lo.setTypes(cust.getType());
			String pattern = "yyyy-MM-dd hh:mm:ss";
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
			String date = simpleDateFormat.format(new Date());
			lo.setToday(date);			
			
			return lo;
		}
		lo.setMessage("Invalid Username/Password");
		
        return lo;
	}

	public Helpline getHelplinePhones() {
		try {
			Helpline helpline = jdbcTemplate.queryForObject(
				"SELECT phone1, phone2, phone3 FROM helpline LIMIT 1", 
				new BeanPropertyRowMapper<Helpline>(Helpline.class)
			);
			return helpline;
		} catch (Exception e) {
			return null;
		}
	}

	public boolean submitGateManOrStationMaster(GateManStationMaster gateManStationMaster) {
		try {
			// Check if record exists
			Integer existingId = jdbcTemplate.queryForObject(
				"SELECT id FROM customers WHERE roles = ? AND username = ?", 
				new Object[]{gateManStationMaster.getRoles(), gateManStationMaster.getUsername()}, 
				Integer.class
			);
			
			if (existingId != null) {
				// Update existing record
				jdbcTemplate.update(
					"UPDATE customers SET first_name = ?, phone = ? WHERE roles = ? AND username = ?",
					gateManStationMaster.getFirst_name(), gateManStationMaster.getPhone(),
					gateManStationMaster.getRoles(), gateManStationMaster.getUsername()
				);
			} else {
				// Insert new record
				jdbcTemplate.update(
					"INSERT INTO customers (first_name, phone, roles, username) VALUES (?, ?, ?, ?)",
					gateManStationMaster.getFirst_name(), gateManStationMaster.getPhone(),
					gateManStationMaster.getRoles(), gateManStationMaster.getUsername()
				);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public GateManStationMaster getGateManOrStationMasterDetails(String roles, String username) {
		try {
			GateManStationMaster details = jdbcTemplate.queryForObject(
				"SELECT first_name, phone FROM customers WHERE roles = ? AND username = ?",
				new Object[]{roles != null ? roles.trim() : null, username != null ? username.trim() : null},
				new BeanPropertyRowMapper<GateManStationMaster>(GateManStationMaster.class)
			);
			return details;
		} catch (Exception e) {
			return null;
		}
	}





}
