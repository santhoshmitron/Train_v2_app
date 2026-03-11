package com.jsinfotech.Service;


import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsinfotech.Domain.J_Message;

@Service
public class KafKaConsumerService {
	
@Autowired
AcknowledgementService ackService;

private static final Logger logger = LogManager.getLogger(KafKaConsumerService.class);
 
 @KafkaListener(topics = "${general.topic.name}", group = "${general.topic.group.id}",containerFactory = "kafkaListenerContainerFactory")
 public void consume(String message) {
     logger.info(String.format("Message recieved -> %s", message));
		ObjectMapper objectMapper = new ObjectMapper();
		
		//convert json string to object
		try {
			
			J_Message emp = objectMapper.readValue(message, J_Message.class);
			if(emp.getFailsafe_method()) {
				if(emp.getIsfailsafe()) {
					ackService.updateFailSafe(emp.getBoom1Id(), true);
					System.out.println("parameters11");
				}
				else {
					ackService.updateFailSafe(emp.getBoom1Id(), false);
					System.out.println("parameters12");
				}
			   
			}else {
				 ackService.updateQueue(emp.getUsername(), emp.getClose(), emp.getBoom1Id());
					System.out.println(emp.toString());
			}
		   
			
			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

     //ackService.updateQueue(username, status, gate_id);
 }
 
}
