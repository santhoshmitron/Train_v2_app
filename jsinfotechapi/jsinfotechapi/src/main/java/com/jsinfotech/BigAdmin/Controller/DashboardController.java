package com.jsinfotech.BigAdmin.Controller;
//bigAdmin

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.BigAdmin.Domain.Login;
import com.jsinfotech.BigAdmin.Domain.Userslist;
import com.jsinfotech.BigAdmin.Service.DashboardService;
import com.jsinfotech.BigAdmin.utils.InvalidUsernamePasswordException;
import com.jsinfotech.Domain.ManageGates;
@RestController
@RequestMapping("/jsinfo")
public class DashboardController {


	@Autowired
	DashboardService dashservice;

    private static final Logger logger = LogManager.getLogger(DashboardController.class);

	@RequestMapping(value = "/admin/getuserlist/{role}", method = RequestMethod.GET)
	public List<Userslist> getUserslist(@PathVariable("role") String role)
	{
		logger.info("in dashboard service");
		return  dashservice.GetUserslist(role);
		
	 
				

	}
	
	@RequestMapping(value = "/admin/GateStatus/{Status}", method = RequestMethod.GET)
	public List<Userslist> getGateStatus(@PathVariable("Status") String Status)
	{
		logger.info("in dashboard service");
		return  dashservice.GetGateStatus(Status);
		
	 
				

	}
	
	@RequestMapping(value = "/admin/gatelist/{role}", method = RequestMethod.GET)
	@CrossOrigin(origins = "*")
	public List<ManageGates> getGates(@PathVariable("role") String role)
	{
		System.out.println("in dashboard service");
		return  dashservice.GetGateList(role);
				

	}
	@RequestMapping(value="/Admin/login1", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	@CrossOrigin(origins = "*")
	public Boolean insert(@RequestBody ManageGates login) throws InvalidUsernamePasswordException
	{
		try {
			
			logger.info("Entered"+login.toString());
			dashservice.insertreports(login);

			return true;

		} catch (Exception e) {

			throw new InvalidUsernamePasswordException();

		}

	}
	
	@RequestMapping(value="/Admin/update", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	@CrossOrigin(origins = "*")
	public Boolean update(@RequestBody ManageGates login) throws InvalidUsernamePasswordException
	{
		try {
			
			logger.info("Entered"+login.toString());
			dashservice.updategates(login);

			return true;

		} catch (Exception e) {

			throw new InvalidUsernamePasswordException();

		}

	}
	
	@RequestMapping(value="/Admin/update/customer", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	@CrossOrigin(origins = "*")
	public Boolean updatecustomer(@RequestBody Login login) throws InvalidUsernamePasswordException
	{
		try {
			
			logger.info("Entered"+login.toString());
			//dashservice.updategates(login);

			return true;

		} catch (Exception e) {

			throw new InvalidUsernamePasswordException();

		}

	}
	@RequestMapping(value="/Admin/delete", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	@CrossOrigin(origins = "*")
	public Boolean delete(@RequestBody ManageGates login) throws InvalidUsernamePasswordException
	{
		try {
			
			logger.info("Entered"+login.toString());
			dashservice.deletegates(login);

			return true;

		} catch (Exception e) {

			throw new InvalidUsernamePasswordException();

		}

	}
	
	
	@RequestMapping(value="/Admin/addcustomer", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	@CrossOrigin(origins = "*")
	public Boolean insertaddcustomer(@RequestBody Login login) throws InvalidUsernamePasswordException
	{
		try {
			
			logger.info("Entered"+login.toString());
			dashservice.insertcustomer(login);

			return true;

		} catch (Exception e) {

			throw new InvalidUsernamePasswordException();

		}

	}
	
	@RequestMapping(value = "/admin/getcustomer/{role}", method = RequestMethod.GET)
	@CrossOrigin(origins = "*")
	public List<Login> getcustomer(@PathVariable("role") String role)
	{
		System.out.println("in dashboard service");
		return  dashservice.GetCustomer(role);
				

	}
	
	@RequestMapping(value="/Admin/customer", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	@CrossOrigin(origins = "*")
	public Boolean insertCustomers(@RequestBody ManageGates login) throws InvalidUsernamePasswordException
	{
		try {
			
			logger.info("Entered"+login.toString());
			dashservice.insertreports(login);

			return true;

		} catch (Exception e) {

			throw new InvalidUsernamePasswordException();

		}

	}

}
