package com.jsinfotech.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.jsinfotech.Domain.Customers;


@Service
public class CustomersService {
	@Autowired
	JdbcTemplate jdbcTemplate;


	public Customers findById(int id) {
		return jdbcTemplate.queryForObject("select * from customers where id=?", new Object[] { id },
				new BeanPropertyRowMapper<Customers>(Customers.class));
	}



	public void deleteById(int id) {
		jdbcTemplate.update("delete from customers where id=?", id);
	}

	public void add(Customers customer) {
		jdbcTemplate.update("insert into customers (id, name, phone) values(?,  ?, ?)",
				customer.getId(), customer.getName(), customer.getPhone());
	}

	public void update(Customers customer)
	{
		jdbcTemplate.update("update customers set id = ?, set name = ?, phone = ? where id = ?",
				new Object[] { customer.getId(), customer.getName(), customer.getPhone(), customer.getId()});
	}

	public List<Customers> findAll() {
		
		
		return jdbcTemplate.query("select * from customers", new RowMapper<Customers>() {
			@Override
			public Customers mapRow(ResultSet rs, int i) throws SQLException {
				Customers customer = new Customers();
				customer.setId(rs.getInt("id"));
				customer.setName(rs.getString("name"));
				customer.setPhone(rs.getString("phone"));
				return customer;
			}
		});
	}
}
