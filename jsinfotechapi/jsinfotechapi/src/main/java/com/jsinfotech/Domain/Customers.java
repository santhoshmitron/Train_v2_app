package com.jsinfotech.Domain;

import java.util.Date;


public class Customers {
	
	private int id;
	private String name;
	private String phone;
	private String username;
	private String password;
	private String email;
	private String mobile;
	private String address;
	private String city;
	private String pincode;
	private String pic;
	private String type;
	private String shop_name;
	private Date added_on;
	private Integer target;
	private String status;
	private String logo;
	private String roles;


	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getMobile() {
		return mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getPincode() {
		return pincode;
	}
	public void setPincode(String pincode) {
		this.pincode = pincode;
	}
	public String getPic() {
		return pic;
	}
	public void setPic(String pic) {
		this.pic = pic;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getShop_name() {
		return shop_name;
	}
	public void setShop_name(String shop_name) {
		this.shop_name = shop_name;
	}
	public Date getAdded_on() {
		return added_on;
	}
	public void setAdded_on(Date added_on) {
		this.added_on = added_on;
	}
	public Integer getTarget() {
		return target;
	}
	public void setTarget(Integer target) {
		this.target = target;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getLogo() {
		return logo;
	}
	public void setLogo(String logo) {
		this.logo = logo;
	}
	public String getRoles() {
		return roles;
	}
	public void setRoles(String roles) {
		this.roles = roles;
	}
	public Customers(int id, String name, String phone) {
		this.id = id;
		this.name = name;
		this.phone = phone;
	}
	public Customers() { }
	public int getId()

	{
		return id;
	}
	public void setId(int id)

	{
		this.id = id;
	}
	public String getName()
	{
		return name;

	}
	public void setName(String name)

	{
		this.name = name;
	}
	public String getPhone()

	{
		return phone;
	}
	public void setPhone(String phone)
	{
		this.phone = phone;
	}


}

