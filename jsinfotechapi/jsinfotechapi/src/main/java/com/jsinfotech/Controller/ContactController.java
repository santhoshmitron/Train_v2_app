package com.jsinfotech.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.Domain.Contact;
import com.jsinfotech.Domain.Response;
import com.jsinfotech.Service.ContactService;

@RestController
@RequestMapping("/contacts")
public class ContactController {

	@Autowired
	ContactService service;

	@RequestMapping(value = "/insert", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public Response insert(@RequestBody Contact contact) {
		Response response = new Response();
		try {
			service.add(contact);
			response.setStatus("Success");
			response.setMessage("Contact added successfully");
			response.setNumber(200);
		} catch (Exception e) {
			response.setStatus("Error");
			response.setMessage("Failed to add contact: " + e.getMessage());
			response.setNumber(500);
		}
		return response;
	}
}
