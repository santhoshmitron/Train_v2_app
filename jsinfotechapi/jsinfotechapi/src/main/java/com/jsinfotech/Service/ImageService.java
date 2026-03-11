package com.jsinfotech.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Images;


@Service
public class ImageService {
    @Autowired
    JdbcTemplate jdbcTemplate;


    public Images findById(int id) {
        return jdbcTemplate.queryForObject("select * from images where id=?", new Object[] { id },
                new BeanPropertyRowMapper<Images>(Images.class));
    }

    
}
