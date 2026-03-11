package com.jsinfotech.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Notifications;


@Service
public class NotificationsService {
    @Autowired
    JdbcTemplate jdbcTemplate;


    public  Notifications findById(int id) {
        return jdbcTemplate.queryForObject("select * from  notifications where id=?", new Object[] { id },
                new BeanPropertyRowMapper< Notifications>( Notifications.class));
    }



    
}
