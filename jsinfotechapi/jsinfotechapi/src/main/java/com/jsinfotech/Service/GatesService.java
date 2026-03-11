package com.jsinfotech.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Gates;


@Service
public class GatesService {
    @Autowired
    JdbcTemplate jdbcTemplate;


    public Gates findById(int id) {
        return jdbcTemplate.queryForObject("select * from gates where id=?", new Object[] { id },
                new BeanPropertyRowMapper<Gates>(Gates.class));
    }



    
}
