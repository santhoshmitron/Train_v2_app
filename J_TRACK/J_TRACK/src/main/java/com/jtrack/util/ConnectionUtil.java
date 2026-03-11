package com.jtrack.util;
import java.sql.SQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import com.mysql.cj.jdbc.Driver;

public class ConnectionUtil {

    public static JdbcTemplate getDataSource(String host,String username,String password) throws SQLException  {

        SimpleDriverDataSource ds = new SimpleDriverDataSource();
        ds.setDriver(new com.mysql.cj.jdbc.Driver());
        ds.setUrl("jdbc:mysql://"+host+":3306/wmirchic_jsinfo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        ds.setUsername(username);
        ds.setPassword(password);
        return new JdbcTemplate(ds);
    }

}
