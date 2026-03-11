package com.jsinfotech.BigAdmin.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.jsinfotech.BigAdmin.Domain.Login;
import com.jsinfotech.BigAdmin.Domain.Userslist;
import com.jsinfotech.Domain.ManageGates;

@Service
public class DashboardService {

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	JdbcTemplate jdbcTemplate1;


	
	public List<Userslist> GetUserslist(String role) {

		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("role", role);
		String  query = "";
		if (role.indexOf('S')!=-1) {
			query = "select * from customers where roles='SM'" ;	

		}
		else if(role.indexOf('G')!=-1)
		{
			query = "select * from customers where roles='GM'" ;	

		}
		else if (role.indexOf('a')!=-1) {
			query = "select id from admins" ;	

		}


		List<Map<String, Object>> rows = jdbcTemplate1.queryForList(query);
		Userslist NoOfuserss=  new  Userslist();
		NoOfuserss.setNoOfUsers(rows.size());
		ArrayList<Userslist>  NoOfusers  = new ArrayList<Userslist>();
		NoOfusers.add(NoOfuserss);
		System.out.println("size of rows ::"+rows.size());

		return NoOfusers;





	}


	public List<Userslist> GetGateStatus(String status) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("status", status);
		String  query = "";
		if (status.indexOf('O')!=-1) {
			query = "SELECT id FROM managegates WHERE status='Open'" ;	

		}
		else if(status.indexOf('C')!=-1)
		{
			query = "SELECT id FROM managegates WHERE status='Closed'" ;	

		}
        List<Map<String, Object>> rows = jdbcTemplate1.queryForList(query);
		Userslist GateStatus=  new  Userslist();
		GateStatus.setGateStatus(rows.size());
		ArrayList<Userslist>  NoOfusers  = new ArrayList<Userslist>();
		NoOfusers.add(GateStatus);
		return NoOfusers;




	}
	
	public List<ManageGates> GetGateList(String role) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("status", role);
		String  query = "";
		if (role.indexOf('S')!=-1) {
			query = "SELECT id,Gate_Num,BOOM1_ID,BOOM2_ID,handle,SM,GM,status,added_on,BS1_GO,BS1_GC,LS_GO,LS_GC,BS1_STATUS,LEVER_STATUS FROM managegates" ;	

		}
		else if(role.indexOf('G')!=-1)
		{
			query = "SELECT id,Gate_Num,BOOM1_ID,BOOM2_ID,handle,SM,GM,status,added_on,BS1_GO,BS1_GC,LS_GO,LS_GC,BS1_STATUS,LEVER_STATUS FROM managegates" ;	;	

		}
        List<Map<String, Object>> rows = jdbcTemplate1.queryForList(query);
		ArrayList<ManageGates>  NoOfusers  = new ArrayList<ManageGates>();

        for(Map<String, Object> map:rows) {
		ManageGates GateStatus=  new  ManageGates();
		GateStatus.setId(Integer.parseInt(map.get("id").toString()));
		GateStatus.setGateNum(map.get("Gate_Num").toString());
		GateStatus.setBoom1Id((String) map.get("BOOM1_ID"));
		GateStatus.setBoom2Id((String) map.get("BOOM2_ID"));
		GateStatus.setHandle((String) map.get("handle"));
		GateStatus.setSM((String) map.get("SM"));
		GateStatus.setGM((String) map.get("GM"));
		GateStatus.setBs1Go((String) map.get("BS1_GO"));
		GateStatus.setBs1Gc((String) map.get("BS1_GC"));
		GateStatus.setLsGo((String) map.get("LS_GO"));
		GateStatus.setLsGc((String) map.get("LS_GC"));
		GateStatus.setStatus((String) map.get("status"));
		GateStatus.setBs1Status((String) map.get("BS1_STATUS"));
		GateStatus.setLeverStatus((String) map.get("LEVER_STATUS"));
		NoOfusers.add(GateStatus);
        }
		return NoOfusers;

	}
	
	public List<Login> GetCustomer(String role) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("status", role);
		String  query = "";
		if (role.indexOf('S')!=-1) {
			query = "SELECT id,username,password,name,email,mobile,phone,address,city,pincode,type,shop_name,added_on,status,roles FROM customers" ;	

		}
		else if(role.indexOf('G')!=-1)
		{
			query = "SELECT id,username,password,name,email,mobile,phone,address,city,pincode,type,shop_name,added_on,status,roles FROM customers" ;	;	

		}
        List<Map<String, Object>> rows = jdbcTemplate1.queryForList(query);
		ArrayList<Login>  NoOfusers  = new ArrayList<Login>();

        for(Map<String, Object> map:rows) {
        	Login GateStatus=  new  Login();
		GateStatus.setId(Integer.parseInt(map.get("id").toString()));
		GateStatus.setUsername(map.get("username").toString());
		GateStatus.setPassword(map.get("password").toString());
		GateStatus.setName(map.get("name").toString());
		GateStatus.setEmail(map.get("email").toString());
		GateStatus.setMobile(map.get("mobile").toString());
		GateStatus.setPhone(map.get("phone").toString());
		GateStatus.setAddress(map.get("address").toString());
		GateStatus.setCity(map.get("city").toString());
		GateStatus.setPincode(map.get("pincode").toString());
		GateStatus.setType(map.get("type").toString());
		GateStatus.setShop_name(map.get("shop_name").toString());
		GateStatus.setAdded_on(map.get("added_on").toString());
		GateStatus.setStatus(map.get("status").toString());
		GateStatus.setRoles(map.get("roles").toString());
		NoOfusers.add(GateStatus);
        }
		return NoOfusers;

	}
	
	public  boolean insertreports( ManageGates obj) {
		
		String pattern1 = "yyyy-MM-dd hh:mm:ss";
		SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(pattern1);
		String time = simpleDateFormat1.format(new Date());
		jdbcTemplate1.update("insert into managegates (id, Gate_Num, BOOM1_ID, BOOM2_ID, handle, SM, GM, status, added_on, BS1_GO,BS1_GC,LS_GO,LS_GC,BS1_STATUS,LEVER_STATUS) values(?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?)",
				obj.getId(),obj.getGateNum(),obj.getBoom1Id(),obj.getBoom2Id(),obj.getHandle(),obj.getSM(),obj.getGM(),"Open",time,obj.getBs1Go(),obj.getBs1Gc(),obj.getLsGo(),obj.getLsGc(),"Open","Open");
		return true;
	}
	
 public  boolean updategates( ManageGates obj) {
		
		String pattern1 = "yyyy-MM-dd hh:mm:ss";
		SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(pattern1);
		String time = simpleDateFormat1.format(new Date());
		jdbcTemplate1.update("update managegates set BOOM1_ID = ?, BOOM2_ID = ?, handle = ? where id = ?",
				obj.getBoom1Id(),obj.getBoom2Id(),obj.getHandle(),obj.getId());
		return true;
	}
 
 public  boolean updateAdmin( Login obj) {
		
		String pattern1 = "yyyy-MM-dd hh:mm:ss";
		SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(pattern1);
		String time = simpleDateFormat1.format(new Date());
		/**jdbcTemplate1.update("update managegates set gateId = ?, handle = ? where id = ?",
				obj.getGateId(),obj.getHandle(),obj.getId());**/
		return true;
	}
 
	
public  boolean deletegates( ManageGates obj) {
		
		String pattern1 = "yyyy-MM-dd hh:mm:ss";
		SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(pattern1);
		String time = simpleDateFormat1.format(new Date());
		jdbcTemplate1.update("delete from managegates where id = ?",obj.getId());
		return true;
	}
	
	public  boolean insertcustomer( Login obj) {
		try {
		String pattern1 = "yyyy-MM-dd hh:mm:ss";
		SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(pattern1);
		String time = simpleDateFormat1.format(new Date());
		jdbcTemplate1.update("insert into customers (id,username,password,name,email,mobile,phone,address,city,pincode,type,shop_name,added_on,status,roles,logo,pic) values(?,?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
		obj.getId(),obj.getUsername(),obj.getPassword(),obj.getName(),obj.getEmail(),obj.getMobile(),obj.getPhone(),obj.getAddress(),obj.getCity(),obj.getPincode(),obj.getType(),obj.getShop_name(),time,obj.getStatus(),obj.getRoles(),"","");
		}catch(Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}


