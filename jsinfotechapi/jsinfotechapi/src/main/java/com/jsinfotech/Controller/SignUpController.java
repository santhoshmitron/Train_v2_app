package com.jsinfotech.Controller;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsinfotech.Domain.Gate;
import com.jsinfotech.Domain.Gate2;
import com.jsinfotech.Domain.Priority;
import com.jsinfotech.Domain.Reports;
import com.jsinfotech.Domain.Response;
import com.jsinfotech.Domain.Signup;
import com.jsinfotech.Service.AsyncService;
import com.jsinfotech.Service.ProcessServiceImpl;
import com.jsinfotech.Service.SignupService;
import com.jsinfotech.Service.TradeRedisRepo;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Quote;
import com.zerodhatech.models.User;

import okhttp3.OkHttpClient;
import okhttp3.Request;

@RestController
@RequestMapping("/trade")
public class SignUpController {
	@Autowired
	SignupService  signupservice;
	
	@Autowired 
	JdbcTemplate jdbcTemplate1;
	
	private String [] key;
	
	private Boolean stop = false;
	
	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	@Autowired
    private KafkaTemplate<String, Gate> kafkaTemplate;
	
    private KiteConnect kiteConnect = null;
    
    @Value(value = "${kafka.topic1}")
    private String topic1;
    
    @Value(value = "${kafka.topic2}")
    private String topic2;

    
    @Autowired
	private AsyncService asyncService;
    
    @Autowired
    private TradeRedisRepo tradeRedisRepo;

	@RequestMapping(value="/Signup", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	public  Boolean insertsend(@RequestBody Signup signup)
	{
		   return signupservice.sendData(signup);
	}
	
	@RequestMapping(value = "/stop", method = RequestMethod.GET)
	public Response getstop(@RequestParam(name = "stop") Boolean stop1)
	{
		
		asyncService.stopStock(stop1);
	 	Response res = new Response();
		res.setStatus("200");
		res.setMessage("success");
		res.setNumber(200);
		return res;
	}
	@RequestMapping(value = "/trigger", method = RequestMethod.GET)
	public Response getReports(@RequestParam(name = "apikey") String apikey)
	{
     
		asyncService.triggerStock(apikey);
	    Response res = new Response();
		res.setStatus("200");
		res.setMessage("success");
		res.setNumber(200);
		return res;
	}
	
	@RequestMapping(value = "/addtoredis", method = RequestMethod.GET)
	public Response addToRedis(@RequestParam(name = "apikey") String apikey)
	{
     
		tradeRedisRepo.custructPojo(apikey);
	    Response res = new Response();
		res.setStatus("200");
		res.setMessage("success");
		res.setNumber(200);
		return res;
	}
	@RequestMapping(value = "/getticker", method = RequestMethod.GET)
	public Response getTicker(@RequestParam(name = "ticker") String ticker)
	{
		
		String prority = tradeRedisRepo.getPojo(ticker);
	 	Response res = new Response();
		res.setStatus("200");
		res.setMessage(prority.toString());
		res.setNumber(200);
		return res;
	}
	@RequestMapping(value = "/getticker1", method = RequestMethod.GET)
	public Response getTicker1(@RequestParam(name = "ticker") String ticker) throws IOException
	{
		
		OkHttpClient client = new OkHttpClient().newBuilder()
				  .build();
		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");
		okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, "{\"filter\":[{\"left\":\"market_cap_basic\",\"operation\":\"nempty\"},{\"left\":\"type\",\"operation\":\"in_range\",\"right\":[\"stock\",\"dr\",\"fund\"]},{\"left\":\"subtype\",\"operation\":\"in_range\",\"right\":[\"common\",\"foreign-issuer\",\"\",\"etf\",\"etf,odd\",\"etf,otc\",\"etf,cfd\"]},{\"left\":\"is_primary\",\"operation\":\"equal\",\"right\":true}],\"options\":{\"lang\":\"en\"},\"markets\":[\"india\"],\"symbols\":{\"query\":{\"types\":[]},\"tickers\":[]},\"columns\":[\"logoid\",\"sector\",\"industry\",\"country\",\"name\",\"change|1\",\"change|5\",\"change|15\",\"change|60\",\"change|240\",\"change\",\"change|1W\",\"change|1M\",\"Recommend.MA\",\"close\",\"SMA20\",\"SMA50\",\"SMA200\",\"BB.upper\",\"BB.lower\",\"description\",\"type\",\"subtype\",\"update_mode\",\"pricescale\",\"minmov\",\"fractional\",\"minmove2\",\"market_cap_basic\"],\"sort\":{\"sortBy\":\"market_cap_basic\",\"sortOrder\":\"desc\"},\"price_conversion\":{\"to_symbol\":false},\"range\":[0,5000]}");
				okhttp3.Request request = new okhttp3.Request.Builder()
				  .url("https://scanner.tradingview.com/india/scan")
				  .method("POST", body)
				  .addHeader("authority", "scanner.tradingview.com")
				  .addHeader("accept", "text/plain, */*; q=0.01")
				  .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
				  .addHeader("origin", "https://in.tradingview.com")
				  .addHeader("referer", "https://in.tradingview.com/")
				  .addHeader("accept-language", "en-GB,en-US;q=0.9,en;q=0.8")
				  .build();
				okhttp3.Response response = client.newCall(request).execute();
				
	 	Response res = new Response();
		res.setStatus("200");
		res.setMessage("");
		res.setNumber(200);
		return res;
	}
	public  KiteConnect getConnection(String apikey,String apiSecret,String publictoken) throws SQLException {
        if(kiteConnect==null) {
             kiteConnect = new KiteConnect(apikey);
            kiteConnect.setUserId("ZA9268");
            try {
                User user = kiteConnect.generateSession(publictoken, apiSecret);
                kiteConnect.setAccessToken(user.accessToken);
                kiteConnect.setPublicToken(user.publicToken);
                System.out.println("connection establihed");
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println("connection returned"+e.getMessage());

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (KiteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("connection returned"+e.getMessage());

            }
            return kiteConnect;
        }else{
            System.out.println("connection returned");

            return kiteConnect;
        }
        
        
        
	}
	

}
