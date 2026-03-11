package com.jsinfotech;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class Test1 {

	
	public static void main(String[] args) throws ParseException {

		
			 String replaceString1 = extracted();

			System.out.println(replaceString1);
			try {
				dateFetch(replaceString1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}

	private static String extracted() {
		long MS_PER_DAY = 24L * 60 * 60 * 1000;
		    long msWithoutTime = (System.currentTimeMillis() / MS_PER_DAY) * MS_PER_DAY;
		    LocalDate today = LocalDate.now();
		    System.out.println("Current date: " + today);

		    //add 1 week to the current date
		    LocalDate nextWeek = today.plus(1, ChronoUnit.WEEKS);
		    System.out.println("Next week: " + nextWeek);
		    Date date2 = java.sql.Date.valueOf(nextWeek);

		Date date1 = new Date(msWithoutTime);
		long unixTime = date1.getTime()/1000L;
		long unixTime2 = date2.getTime()/1000L;
		String data = "{\"filter\":[{\"left\":\"market_cap_basic\",\"operation\":\"nempty\"},{\"left\":\"earnings_release_date,earnings_release_next_date\",\"operation\":\"in_range\",\"right\":[1642444200,1642530600]},{\"left\":\"earnings_release_date,earnings_release_next_date\",\"operation\":\"nequal\",\"right\":1642530600}],\"options\":{\"lang\":\"en\"},\"markets\":[\"india\"],\"symbols\":{\"query\":{\"types\":[]},\"tickers\":[]},\"columns\":[\"logoid\",\"name\",\"market_cap_basic\",\"earnings_per_share_forecast_next_fq\",\"earnings_per_share_fq\",\"eps_surprise_fq\",\"eps_surprise_percent_fq\",\"revenue_forecast_next_fq\",\"revenue_fq\",\"earnings_release_next_date\",\"earnings_release_next_calendar_date\",\"earnings_release_next_time\",\"description\",\"type\",\"subtype\",\"update_mode\",\"earnings_per_share_forecast_fq\",\"revenue_forecast_fq\",\"earnings_release_date\",\"earnings_release_calendar_date\",\"earnings_release_time\",\"currency\",\"fundamental_currency_code\"],\"sort\":{\"sortBy\":\"market_cap_basic\",\"sortOrder\":\"desc\"},\"range\":[0,150]}";
		String replaceString=data.replace("1642444200",String.valueOf(unixTime));//replaces all occurrences of 'a' to 'e'  
		System.out.println(replaceString);  
		String replaceString1=replaceString.replace("1642530600",String.valueOf(unixTime2));//replaces all occurrences of 'a' to 'e'  
		return replaceString1;
	}
	
	public static void dateFetch(String replaceString1) throws IOException {
		URL url = new URL("https://scanner.tradingview.com/india/scan");
		HttpURLConnection http = (HttpURLConnection)url.openConnection();
		http.setRequestMethod("POST");
		http.setDoOutput(true);
		http.setRequestProperty("authority", "scanner.tradingview.com");
		http.setRequestProperty("accept", "text/plain, */*; q=0.01");
		http.setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
		http.setRequestProperty("sec-ch-ua-mobile", "?0");
		http.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.71 Safari/537.36");
		http.setRequestProperty("origin", "https://in.tradingview.com");
		http.setRequestProperty("sec-fetch-site", "same-site");
		http.setRequestProperty("sec-fetch-mode", "cors");
		http.setRequestProperty("sec-fetch-dest", "empty");
		http.setRequestProperty("referer", "https://in.tradingview.com/");
		http.setRequestProperty("accept-language", "en-GB,en-US;q=0.9,en;q=0.8");

		String data = "{\"filter\":[{\"left\":\"market_cap_basic\",\"operation\":\"nempty\"},{\"left\":\"earnings_release_date,earnings_release_next_date\",\"operation\":\"in_range\",\"right\":[1642444200,1642530600]},{\"left\":\"earnings_release_date,earnings_release_next_date\",\"operation\":\"nequal\",\"right\":1642530600}],\"options\":{\"lang\":\"en\"},\"markets\":[\"india\"],\"symbols\":{\"query\":{\"types\":[]},\"tickers\":[]},\"columns\":[\"logoid\",\"name\",\"market_cap_basic\",\"earnings_per_share_forecast_next_fq\",\"earnings_per_share_fq\",\"eps_surprise_fq\",\"eps_surprise_percent_fq\",\"revenue_forecast_next_fq\",\"revenue_fq\",\"earnings_release_next_date\",\"earnings_release_next_calendar_date\",\"earnings_release_next_time\",\"description\",\"type\",\"subtype\",\"update_mode\",\"earnings_per_share_forecast_fq\",\"revenue_forecast_fq\",\"earnings_release_date\",\"earnings_release_calendar_date\",\"earnings_release_time\",\"currency\",\"fundamental_currency_code\"],\"sort\":{\"sortBy\":\"market_cap_basic\",\"sortOrder\":\"desc\"},\"range\":[0,150]}";

		data = replaceString1;
		
		byte[] out = data.getBytes(StandardCharsets.UTF_8);

		OutputStream stream = http.getOutputStream();
		stream.write(out);

    	if (http.getResponseCode() == HttpURLConnection.HTTP_OK) { //success
			BufferedReader in = new BufferedReader(new InputStreamReader(
					http.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			System.out.println(response.toString());
			JSONObject json = new JSONObject(response.toString());  
			System.out.println(json.get("data"));
			JSONArray array = new JSONArray(json.get("data").toString());  
            List<String> sec = new ArrayList<>();
            Map<String, Long> map = new HashMap<String, Long>();

			for(int i=0; i < array.length(); i++)   
			{  
				try {
			JSONObject object = array.getJSONObject(i); 
			if(object.getJSONArray("d").getDouble(6)>0 && object.getString("s").contains("NSE")) {
			System.out.println(object.getString("s")); 
			System.out.println(object.getJSONArray("d"));
			System.out.println(object.getJSONArray("d").getString(1)); 
			System.out.println(object.getJSONArray("d").getLong(18)); 
			map.put(object.getString("s"),object.getJSONArray("d").getLong(18));
			}
			}catch(Exception e) {
					continue;
				
			}
			
			}  

			  System.out.println(map);
		} else {
			System.out.println("POST request not worked");
		} 
    	
		System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
		http.disconnect();


	}
}
