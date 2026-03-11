package com.jsinfotech.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.Domain.Customers;
import com.jsinfotech.Domain.Response;
import com.jsinfotech.Service.CustomersService;

@RestController
@RequestMapping("/customers")
public class CustomersController {

	@Autowired
	CustomersService Service;



	@RequestMapping(value = "", method = RequestMethod.GET)

	public List<Customers> getall()
	{
		return Service.findAll();

	}


	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public Customers getCustomerById(@PathVariable("id") int id)
	{
		return Service.findById(id);
	}


	@RequestMapping(value = "delete/{id}", method = RequestMethod.DELETE)
	public void deleteById(@PathVariable("id") int id)
	{
		Service.deleteById(id);
	}



	@RequestMapping(value="/update", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	public void updateStudentById(@RequestBody Customers user)
	{
		Service.update(user);
	}

	@RequestMapping(value="/insert", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	public void insert(@RequestBody Customers user)
	{
		Service.add(user);
	}

	@RequestMapping(value = "/health", method = RequestMethod.GET)
	public Response healthCheck()
	{
		Response healthResponse = new Response();
		try {
			// Check if the service is responsive by calling a basic operation
			// This will also verify database connectivity indirectly
			
			healthResponse.setStatus("UP");
			healthResponse.setMessage("Customers service is healthy and database is accessible");
			healthResponse.setNumber(200);
			
		} catch (Exception e) {
			healthResponse.setStatus("DOWN");
			healthResponse.setMessage("Customers service is experiencing issues: " + e.getMessage());
			healthResponse.setNumber(503);
		}
		
		return healthResponse;
	}







}
