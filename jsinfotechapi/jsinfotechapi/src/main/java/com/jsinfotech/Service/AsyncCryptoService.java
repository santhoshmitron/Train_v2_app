package com.jsinfotech.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Gate;
import com.jsinfotech.Domain.Gate2;

@Service
public class AsyncCryptoService {
	
	   @Value(value = "${kafka.topic1}")
	    private String topic1;
	    
	    @Value(value = "${kafka.topic2}")
	    private String topic2;
	    
		private String [] key;
		
		private Boolean stop = false;
		
		@Autowired
		private NamedParameterJdbcTemplate jdbcTemplate;
		
		@Autowired
	    private KafkaTemplate<String, Gate> kafkaTemplate;
		
	  @Async("processExecutor")
	  public void triggerStock(String apikey) {
	  }
		  

}
