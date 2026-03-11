package com.jsinfotech.BigAdmin.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.BigAdmin.Domain.Admins;
import com.jsinfotech.BigAdmin.Domain.Login;


@Service
public class LoginServiceAdmin {

	@Autowired
	JdbcTemplate jdbcTemplate;

	public Login checkLogin(Login login) throws EmptyResultDataAccessException{

		Admins adm	= jdbcTemplate.queryForObject("select * from admins where email=? and password=?", new Object[] { login.getEmail(),login.getPassword() },
				new BeanPropertyRowMapper<Admins>(Admins.class));

		Login lo = new Login();
		if( adm != null) {
			/**lo.setId(adm.getId());
			lo.setMessage("Login Successfull");
			lo.setUsername(adm.getEmail());
			lo.setName(adm.getName());
			lo.setTypes(adm.getType());
			String pattern = "yyyy-MM-dd hh:mm:ss";
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
			String date = simpleDateFormat.format(new Date());
			lo.setToday(date);
			**/			

			return lo;
		}

		return lo;
	}





}
