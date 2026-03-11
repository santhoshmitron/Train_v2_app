package com.jsinfotech.Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Status;

@Service
public class CacheStatusUpdate {
	
	@Autowired
	private RedisUserRepository userRepository;
	
    private static final Logger logger = LogManager.getLogger(ManageGatesService.class);

	
    public void updatestatus(String user,String type) {	
    
    	logger.info("User" + user + type + "updated");
    	userRepository.push(user, type);
    }
    
    public Status getstatus(String user) {	
    	
    	Status status = new Status();
    	Boolean report = (Boolean)userRepository.pop(user, "report");
    	Boolean statu = (Boolean)userRepository.pop(user, "status");
    	if(report==null) {
        	status.setIs_report_changed(false);
        	logger.info("failed");
    	}else {
        	status.setIs_report_changed(report);
        	logger.info("hit");
    	}
    	if(statu==null) {

    	status.setIs_status_changed(false);
    	}
    	else {
        	status.setIs_status_changed(statu);

    	}
    	return status;
    }

}
