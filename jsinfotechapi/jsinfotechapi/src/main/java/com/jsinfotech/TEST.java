package com.jsinfotech;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;

public class TEST {

	
	public static void main(String[] args) throws ParseException {
		
		int k =6;
		  Set<Integer> s = new HashSet<Integer>();
		for(int i =0;i<k;i++) {     
		s.add(getRandomNumberWithExclusion(s));
		}
		System.out.println(s.toString());

	}
	public static void main1(String[] args) throws ParseException {
		//int k =5;
		//for(int i =0;i<k;i++) {
		//System.out.println(getRandomNumberWithExclusion());
		//}
		  Set<Integer> s = new HashSet<Integer>();
		
		  SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
	        String dateInString = "05-01-2021 02:02:02";
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00:00");

	        Date date = sdf.parse(dateInString);
	        TEST obj = new TEST();
	        try {
				obj.test2();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        //2. Test - Convert Date to Calendar
	       /* Calendar calendar = obj.dateToCalendar(date);
	        System.out.println(calendar.getTime());
	        calendar.setTimeInMillis(calendar.getTimeInMillis());
	        calendar.add(Calendar.MINUTE, 0);
			// Using DateFormat format method we can create a string 
			// representation of a date with the defined format.
			String todayAsString = formatter.format(calendar.getTime());
	        //3. Test - Convert Calendar to Date
	        Date newDate = obj.calendarToDate(calendar);
	        System.out.println(newDate);
	        System.out.println(todayAsString);*/
	        String gate = "GNT-246BS";
	        
	       String name = gate;
			if(name.indexOf("BS")!=-1 ) {
				name = name.replace("BS", "");
				System.out.println("dd"+gate);

			}else {
				name = name.replace("LS", "");
				System.out.println("ww"+gate);
			}
			//String[] parts = gate.split("-");


	}
    private Calendar dateToCalendar(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;

    }
    
    public void test2() throws IOException {
    	URL url = new URL("https://scanner.tradingview.com/india/scan");
    	HttpURLConnection http = (HttpURLConnection)url.openConnection();
    	http.setRequestMethod("POST");
    	http.setDoOutput(true);
    	http.setRequestProperty("authority", "scanner.tradingview.com");
    	http.setRequestProperty("accept", "text/plain, */*; q=0.01");
    	http.setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
    	http.setRequestProperty("origin", "https://in.tradingview.com");
    	http.setRequestProperty("referer", "https://in.tradingview.com/");
    	http.setRequestProperty("accept-language", "en-GB,en-US;q=0.9,en;q=0.8");

    	String data = "{\"filter\":[{\"left\":\"market_cap_basic\",\"operation\":\"nempty\"},{\"left\":\"type\",\"operation\":\"in_range\",\"right\":[\"stock\",\"dr\",\"fund\"]},{\"left\":\"subtype\",\"operation\":\"in_range\",\"right\":[\"common\",\"foreign-issuer\",\"\",\"etf\",\"etf,odd\",\"etf,otc\",\"etf,cfd\"]},{\"left\":\"is_primary\",\"operation\":\"equal\",\"right\":true}],\"options\":{\"lang\":\"en\"},\"markets\":[\"india\"],\"symbols\":{\"query\":{\"types\":[]},\"tickers\":[]},\"columns\":[\"logoid\",\"sector\",\"industry\",\"country\",\"name\",\"change|1\",\"change|5\",\"change|15\",\"change|60\",\"change|240\",\"change\",\"change|1W\",\"change|1M\",\"Recommend.MA\",\"close\",\"SMA20\",\"SMA50\",\"SMA200\",\"BB.upper\",\"BB.lower\",\"description\",\"type\",\"subtype\",\"update_mode\",\"pricescale\",\"minmov\",\"fractional\",\"minmove2\",\"market_cap_basic\"],\"sort\":{\"sortBy\":\"market_cap_basic\",\"sortOrder\":\"desc\"},\"price_conversion\":{\"to_symbol\":false},\"range\":[0,5000]}";    	byte[] out = data.getBytes(StandardCharsets.UTF_8);

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
            Map<String, List<String>> map = new HashMap<String, List<String>>();

			for(int i=0; i < array.length(); i++)   
			{  
				try {
			JSONObject object = array.getJSONObject(i); 
			if(object.getJSONArray("d").getDouble(6)>0 && object.getString("s").contains("NSE")) {
			System.out.println(object.getString("s")); 
			System.out.println(object.getJSONArray("d"));
			System.out.println(object.getJSONArray("d").getString(1)); 
			System.out.println(object.getJSONArray("d").getDouble(5)); 
			System.out.println(object.getJSONArray("d").getDouble(6));
			String key = object.getJSONArray("d").getString(1).replaceAll("\\s+","")+"-"+object.getJSONArray("d").getString(2).replaceAll("\\s+","");
			sec.add(key);
			map.putIfAbsent(key, new ArrayList<String>());
			map.get(key).add(object.getString("s"));
			}
			}catch(Exception e) {
					continue;
				
			}
			
			}  
			  Map<String, Long> result
	            = sec.stream().collect(
	                Collectors.groupingBy(
	                    Function.identity(),
	                    Collectors.counting()));
			  System.out.println(result);
			  System.out.println(map);
		} else {
			System.out.println("POST request not worked");
		}    	http.disconnect();
				
    }
    
    public void test() throws IOException {
    	URL url = new URL("https://scanner.tradingview.com/india/scan");
    	HttpURLConnection http = (HttpURLConnection)url.openConnection();
    	http.setRequestMethod("POST");
    	http.setDoOutput(true);
    	http.setRequestProperty("authority", "scanner.tradingview.com");
    	http.setRequestProperty("accept", "text/plain, */*; q=0.01");
    	http.setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
    	http.setRequestProperty("sec-ch-ua-mobile", "?0");
    	http.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36");
    	http.setRequestProperty("origin", "https://in.tradingview.com");
    	http.setRequestProperty("sec-fetch-site", "same-site");
    	http.setRequestProperty("sec-fetch-mode", "cors");
    	http.setRequestProperty("sec-fetch-dest", "empty");
    	http.setRequestProperty("referer", "https://in.tradingview.com/");
    	http.setRequestProperty("accept-language", "en-GB,en-US;q=0.9,en;q=0.8");

    	String data = "{\"filter\":[{\"left\":\"relative_volume_10d_calc\",\"operation\":\"nempty\"},{\"left\":\"type\",\"operation\":\"in_range\",\"right\":[\"stock\",\"dr\",\"fund\"]},{\"left\":\"subtype\",\"operation\":\"in_range\",\"right\":[\"common\",\"foreign-issuer\",\"\",\"etf\",\"etf,odd\",\"etf,otc\",\"etf,cfd\"]},{\"left\":\"is_primary\",\"operation\":\"equal\",\"right\":true}],\"options\":{\"lang\":\"en\"},\"markets\":[\"india\"],\"symbols\":{\"query\":{\"types\":[]},\"tickers\":[]},\"columns\":[\"logoid\",\"industry\",\"sector\",\"country\",\"name\",\"change|1\",\"change|5\",\"change|15\",\"change|60\",\"change|240\",\"change\",\"change|1W\",\"change|1M\",\"Recommend.MA\",\"close\",\"SMA20\",\"SMA50\",\"SMA200\",\"BB.upper\",\"BB.lower\",\"description\",\"type\",\"subtype\",\"update_mode\",\"pricescale\",\"minmov\",\"fractional\",\"minmove2\",\"currency\",\"fundamental_currency_code\",\"market_cap_basic\"],\"sort\":{\"sortBy\":\"relative_volume_10d_calc\",\"sortOrder\":\"desc\"},\"price_conversion\":{\"to_symbol\":false},\"range\":[0,3000]}";

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
            Map<String, List<String>> map = new HashMap<String, List<String>>();

			for(int i=0; i < array.length(); i++)   
			{  
				try {
			JSONObject object = array.getJSONObject(i); 
			if(object.getJSONArray("d").getDouble(6)>0 && object.getString("s").contains("NSE")) {
			System.out.println(object.getString("s")); 
			System.out.println(object.getJSONArray("d"));
			System.out.println(object.getJSONArray("d").getString(1)); 
			System.out.println(object.getJSONArray("d").getDouble(5)); 
			System.out.println(object.getJSONArray("d").getDouble(6));
			String key = object.getJSONArray("d").getString(1).replaceAll("\\s+","")+"-"+object.getJSONArray("d").getString(2).replaceAll("\\s+","");
			sec.add(key);
			map.putIfAbsent(key, new ArrayList<String>());
			map.get(key).add(object.getString("s"));
			}
			}catch(Exception e) {
					continue;
				
			}
			
			}  
			  Map<String, Long> result
	            = sec.stream().collect(
	                Collectors.groupingBy(
	                    Function.identity(),
	                    Collectors.counting()));
			  System.out.println(result);
			  System.out.println(map);
		} else {
			System.out.println("POST request not worked");
		}    	http.disconnect();
				
    }

    //Convert Calendar to Date
    private Date calendarToDate(Calendar calendar) {
        return calendar.getTime();
    }
	public static int getRandomNumberWithExclusion( Set<Integer> set)
	{
	  Random r = new Random();
	  int result = -1;

	  do
	  {
		  	// 3-digit number, digits 1-9 only (no zeros anywhere)
		  	result = 111 + r.nextInt(889);
	  }//do
	  while( !isAllowed( result,set ) );

	  return result;

	}//met

	private static boolean isAllowed( int number,Set<Integer>set )
	{
		
		if (number < 111 || number > 999) {
			return false;
		}
		String s = String.valueOf(number);
		if (s.length() != 3 || s.indexOf('0') >= 0) {
			return false;
		}
		return !set.contains(number);
	}//met

}
