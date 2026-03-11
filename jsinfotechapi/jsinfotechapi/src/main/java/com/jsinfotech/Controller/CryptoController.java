package com.jsinfotech.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsinfotech.Domain.Response;
import com.jsinfotech.Service.AsyncCryptoService;

@RestController
@RequestMapping("/cryptotrade")
public class CryptoController {
	
	  @Value(value = "${kafka.topic1}")
	    private String topic1;
	    
	    @Value(value = "${kafka.topic2}")
	    private String topic2;
	    
	    @Autowired
		private AsyncCryptoService asyncService;
	    
		@RequestMapping(value = "/trigger", method = RequestMethod.GET)
		public Response getReports(@RequestParam(name = "apikey") String apikey)
		{
	     
			asyncService.triggerStock(apikey);
		    Response res = new Response();
			res.setStatus("200");
			res.setMessage("success");
			res.setNumber(200);
			return res;
		}

}
