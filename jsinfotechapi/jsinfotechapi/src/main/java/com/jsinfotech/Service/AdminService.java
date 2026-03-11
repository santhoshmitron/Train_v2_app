package com.jsinfotech.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.JsInfoAdmin;


@Service
public class AdminService {
	@Autowired
	JdbcTemplate jdbcTemplate;


	public JsInfoAdmin findById(int id) {
		return jdbcTemplate.queryForObject("select * from admins where id=?", new Object[] { id },
				new BeanPropertyRowMapper<JsInfoAdmin>(JsInfoAdmin.class));
	}




}
