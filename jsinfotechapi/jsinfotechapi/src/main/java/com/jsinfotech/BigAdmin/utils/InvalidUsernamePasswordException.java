package com.jsinfotech.BigAdmin.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason="Invalid Username/Password" )
public class InvalidUsernamePasswordException extends Exception {

	
	

}
