package com.jsinfotech.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Signup;


@Service

public class SignupService {
	@Autowired
	JdbcTemplate jdbcTemplate1;

	public Boolean sendData(Signup signup) {
		String username=signup.getUsername();
		String password=signup.getPassword();
		String name=signup.getName();

		jdbcTemplate1.update("insert into customers (username,password,name,email,mobile,phone,address,city,pincode,pic,type,shop_name,added_on,target,status,logo,roles) values()",

			signup.getUsername(),signup.getPassword(),signup.getName(),signup.getEmail(),signup.getMobile(),signup.getPhone());


		return null;
	}


}
