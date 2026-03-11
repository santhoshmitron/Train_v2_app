package com.jsinfotech.BigAdmin.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.BigAdmin.Domain.Login;
import com.jsinfotech.BigAdmin.Service.LoginServiceAdmin;
import com.jsinfotech.BigAdmin.utils.InvalidUsernamePasswordException;

@RestController
@RequestMapping("/jsinfo")
public class LoginControllerAdmin {

	@Autowired
	LoginServiceAdmin loginservice;

	@RequestMapping(value="/Admin/login", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	public Login insert(@RequestBody Login login) throws InvalidUsernamePasswordException
	{
		try {

			return loginservice.checkLogin(login);

		} catch (Exception e) {

			throw new InvalidUsernamePasswordException();

		}

	}




}
