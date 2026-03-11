package com.jsinfotech.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsinfotech.Domain.Gate2;
import com.jsinfotech.Domain.Gate1;
import com.jsinfotech.Domain.Reports;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Quote;
import com.zerodhatech.models.User;

@Service
public class AsyncService{
	
	private String [] key;
	
	private Boolean stop = false;
	
	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	@Autowired
    private KafkaTemplate<String, Gate2> kafkaTemplate2;
	
	@Autowired
    private KafkaTemplate<String, Gate1> kafkaTemplate1;
	
    private KiteConnect kiteConnect = null;
    
    @Value(value = "${kafka.topic1}")
    private String topic1;
    
    @Value(value = "${kafka.topic2}")
    private String topic2;
    
    @Value(value = "${kafka.topic4}")
    private String topic4;
    
    @Value(value = "${kafka.count}")
    private Integer co;
    
    @Value(value = "${kafka.sleeptime}")
    private Integer sleeptime;
    
	public void stopStock(Boolean stop1) {
	    
		this.stop = stop1;
	}
   
	public HashMap<String,List<Double>> volumelist = new HashMap<>();
	public HashMap<String,List<Double>> volumelist1 = new HashMap<>();
	public HashMap<String,List<Double>> pricelist = new HashMap<>();
	public HashMap<String,List<Double>> pricelist1 = new HashMap<>();
	public static HashMap<String,Long> datemap = new HashMap<>();

	private static Map<String,ArrayList<String>> map1 = new HashMap<>();
	
    Map<String, Gate2> map3 = new HashMap<String, Gate2>();

  @Async("processExecutor")
  public void triggerStock(String apikey) {
	 
        String [] key1 = new String [co];
        Map<String,Reports> lookup = new HashMap<String,Reports>();
        Map<String,Reports> lookup1 = new HashMap<String,Reports>();

        stop = false;
		String SQL = "select  stock_ticker,stock_price,52high from stock_price_alert";
		MapSqlParameterSource parameters = new MapSqlParameterSource();

		List<Reports> reports = jdbcTemplate.query(SQL, parameters, new RowMapper<Reports>() {
			@Override
			public Reports mapRow(ResultSet rs, int i) throws SQLException {
				Reports customer = new Reports();
				customer.setCommand(rs.getString("stock_ticker"));
				customer.setId(rs.getInt("stock_price"));
				customer.setFrom(rs.getString("52high"));
				return customer;
			}
		});
		String SQL1 = "select  BOOM1_ID,handle from managegates";
		List<Reports> reports1 = jdbcTemplate.query(SQL1, parameters, new RowMapper<Reports>() {
			@Override
			public Reports mapRow(ResultSet rs, int i) throws SQLException {
				Reports customer = new Reports();
				customer.setCommand(rs.getString("BOOM1_ID"));
				customer.setLc_name("handle");
				return customer;
			}
		});
		try {
			dateFetch();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		int count = 0;
	    for(Reports report:reports) {
	    	key1[count] = report.getCommand();
	    	lookup1.put(report.getCommand(), report);
	    	count++;
	    }
	    for(Reports report:reports1) {
	    	lookup.put(report.getCommand(), report);
	    }
        
	    key = key1;
        
	      if(kiteConnect==null) {
	            try {
	            	
					kiteConnect = getConnection("c9xon4xfwcav7ee4", "vbkr561nphacu1lnlinhdlqblqsoqdxd", apikey);
	
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	       }
	      
          int k1 = 1;
          while(k1>0) {
        	  if(stop) {
                  System.out.println("############Outer Loop Terminated############");
        		  break;
        	  }
          int k = 0;
          int start = 0; 
          try {
			test();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
          while(k<=4 && start < co) {
          String ltp = null;
          String buy = null;
          String sell = null;

          ObjectMapper Obj = new ObjectMapper();
          if(stop) {
              System.out.println("############Inner Loop Terminated############");
    		  break;
    	  }
          try {
        	  
          	String [] temp = Arrays.copyOfRange(key1, start, start+400);
          	System.out.println(Arrays.toString(temp));
              Map<String, Quote> quotes = kiteConnect.getQuote(temp);
              System.out.println(quotes);
              String sellv = null;
              for(String quote:temp) {
                  Boolean ticker = false;
                  Boolean ticker1 = false;

              	  try {
              	    ltp = String.valueOf(quotes.get(quote).lastPrice);
                    buy = String.valueOf(quotes.get(quote).buyQuantity);
                    sell = String.valueOf(quotes.get(quote).sellQuantity);
                    ticker = extractedvolumelist(quotes, quote,ticker);
                    ticker1 = extractedpricelist(quotes, quote,ticker);
                    
                    System.out.println("##@@@@@@@@@@@@@@@@@@##6"+quotes.get(quote).volumeTradedToday);
                    sellv = String.valueOf(quotes.get(quote).volumeTradedToday);
                    

                    System.out.println(quote+"##"+ltp+"##"+buy+"##"+sell);
                    Gate2 gate = new Gate2(quote,quote,"1","1");
                    gate.setBatch(ltp);
                    gate.setHandle(buy);
                    gate.setLever(sell);
                    gate.setTimeStamp(sell);
            		if(lookup1.get(quote).getId() == 0) {
                		gate.setG_value("S");
                        gate.setDate(String.valueOf(lookup1.get(quote).getId()));
            		}else if(lookup1.get(quote).getId() > 0 && lookup1.get(quote).getId() < 1000 ) {
                		gate.setG_value("S");
                        gate.setDate(String.valueOf(lookup1.get(quote).getId()));

            		}
            		else if(lookup1.get(quote).getId() > 1000 && lookup1.get(quote).getId() < 5000 ) {
                		gate.setG_value("M");
                        gate.setDate(String.valueOf(lookup1.get(quote).getId()));

            		}
            		else if(lookup1.get(quote).getId() > 5000 && lookup1.get(quote).getId() < 10000 ) {
                		gate.setG_value("M1");
                        gate.setDate(String.valueOf(lookup1.get(quote).getId()));

            		}
            		else if(lookup1.get(quote).getId() > 10000) {
                		gate.setG_value("L");
                        gate.setDate(String.valueOf(lookup1.get(quote).getId()));
            		}
            		Double ftweekhigh = Double.valueOf(lookup1.get(quote).getFrom());
            		if(quotes.get(quote).lastPrice<ftweekhigh) {
            			
            			Double percentagedip = ((ftweekhigh - quotes.get(quote).lastPrice)/ftweekhigh)*100;
            			if(percentagedip>5.0 && percentagedip<20.0) {
            				gate.setS1("1");
            			}
            			if(percentagedip>20.0) {
            				gate.setS1("2");
            			}
            		}
                    System.out.println("##"+quote+"##1");
            		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            		gate.setTimeStamp(formatter.format(quotes.get(quote).timestamp));
                    /*if(lookup.containsKey(quote)) {
                    	if(Double.parseDouble(ltp)<=Double.parseDouble(lookup.get(quote).getLc_name())) {
                    		gate.setG_value("1");
                    	}
                    }*/
                    System.out.println("##"+quote+"##2");
                    String jsonStr = Obj.writeValueAsString(gate);
                    System.out.println("##"+quote+"##3");
            		kafkaTemplate2.send(topic1,gate);
            		if(map3.get(quote)!=null) {
            			Gate2 gate1= map3.get(quote);
            			 gate1.setBatch(ltp);
                         gate1.setHandle(buy);
                         gate1.setLever(sell);
                         gate1.setG_value(gate.getG_value());
                 		gate1.setTimeStamp(formatter.format(quotes.get(quote).timestamp));
                 		if(Double.parseDouble(buy)>Double.parseDouble(sell)) {
                 		if(ticker == true && ticker1 == true) {
                 			gate1.setVolumne(sellv);
                 			if(volumelist.get(quote).size()>2) {
                 			Double currdiff = (quotes.get(quote).volumeTradedToday-volumelist.get(quote).get(volumelist.get(quote).size()-1));
                 			Double olddiff =(quotes.get(quote).volumeTradedToday-volumelist.get(quote).get(volumelist.get(quote).size()-2));
                 			Double volumnechange = ((currdiff - olddiff)/olddiff) *100;
                 			gate1.setVolumnechange(String.valueOf(volumnechange));
                 			gate1.setOnechgvolumne(String.valueOf(quotes.get(quote).volumeTradedToday-volumelist.get(quote).get(volumelist.get(quote).size()-1)));
                 			}else {
                 				gate1.setVolumnechange("0");
                 			}
                            gate1.setFilter("PriceVolumne");
                    		kafkaTemplate2.send(topic4,gate1);
                               System.out.println("##@@@@@@@@@@@@@@@@@@##3"+sellv);
                    		if(datemap.get(quote)!=null) 
                    		{
                    			Date expiry = new Date(datemap.get(quote));
                        		gate1.setPercentage("Possible Earnings "+ expiry.toString());
                                System.out.println("##@@@@@@@@@@@@@@@@@@##4"+"Earnings" +gate1.toString());

                    		}
	
                 		}	
                		
                 		}

            		}

              	  }catch(Exception e) {
              		  System.out.println(e.getMessage()+quote);
              	  }

              }
             
          k++;
          start = start+400;
          System.out.println("############Iteration############"+k);
          Thread.sleep(sleeptime);
          System.out.println("############Resumed after 10 Secs############"+k);

          
      	} catch (KiteException e) {
			// TODO Auto-generated catch block
			System.out.println(e.message);
		} catch (JSONException e) {
			System.out.println(e.toString());

			// TODO Auto-generated catch block
		} catch (IOException e) {
			System.out.println(e.toString());

			// TODO Auto-generated catch block
		} catch (InterruptedException e) {
			System.out.println(e.toString());

		}
          }
          System.out.println("############newIteration#############");
          try {
			test();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
          System.out.println("############Done with Change#############");

          }
 }

private Boolean extractedpricelist(Map<String, Quote> quotes, String quote, Boolean ticker) {
	// TODO Auto-generated method stub
	pricelist.putIfAbsent(quote, new ArrayList<Double>());
	if(pricelist.get(quote).size()>2) {
		pricelist1.putIfAbsent(quote, new ArrayList<Double>());
	    double te = quotes.get(quote).lastPrice-pricelist.get(quote).get(pricelist.get(quote).size()-1);
	    if(pricelist1.get(quote).size()>2) {
	        Double average = pricelist1.get(quote).stream().mapToDouble(val -> val).average().orElse(0.0);
	        pricelist1.get(quote).add(quotes.get(quote).lastPrice-pricelist.get(quote).get(pricelist.get(quote).size()-1));

	       if(te >=average) {
	    	   ticker = true;
	       }
	    	
	    }else {
	    	pricelist1.get(quote).add(quotes.get(quote).lastPrice-pricelist.get(quote).get(pricelist.get(quote).size()-1));
	    }

	}else {
		pricelist.get(quote).add(quotes.get(quote).lastPrice);
	}
	return ticker;}

private Boolean extractedvolumelist(Map<String, Quote> quotes, String quote, Boolean ticker) {
	
	volumelist.putIfAbsent(quote, new ArrayList<Double>());
	if(volumelist.get(quote).size()>2) {
	    volumelist1.putIfAbsent(quote, new ArrayList<Double>());
	    double te = quotes.get(quote).volumeTradedToday-volumelist.get(quote).get(volumelist.get(quote).size()-1);
	    if(volumelist1.get(quote).size()>2) {
	        Double average = volumelist1.get(quote).stream().mapToDouble(val -> val).average().orElse(0.0);
	        volumelist1.get(quote).add(quotes.get(quote).volumeTradedToday-volumelist.get(quote).get(volumelist.get(quote).size()-1));

	       if(te >=average) {
	    	   ticker = true;
	       }
	    	
	    }else {
	        volumelist1.get(quote).add(quotes.get(quote).volumeTradedToday-volumelist.get(quote).get(volumelist.get(quote).size()-1));
	    }

	}else {
	    volumelist.get(quote).add(quotes.get(quote).volumeTradedToday);
	}
	return ticker;
}
  
  public  Map<String, Long> test() throws IOException {
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

  	String data = "{\"filter\":[{\"left\":\"relative_volume_10d_calc\",\"operation\":\"nempty\"},{\"left\":\"type\",\"operation\":\"in_range\",\"right\":[\"stock\",\"dr\",\"fund\"]},{\"left\":\"subtype\",\"operation\":\"in_range\",\"right\":[\"common\",\"foreign-issuer\",\"\",\"etf\",\"etf,odd\",\"etf,otc\",\"etf,cfd\"]},{\"left\":\"is_primary\",\"operation\":\"equal\",\"right\":true}],\"options\":{\"lang\":\"en\"},\"markets\":[\"india\"],\"symbols\":{\"query\":{\"types\":[]},\"tickers\":[]},\"columns\":[\"logoid\",\"industry\",\"sector\",\"country\",\"name\",\"change|1\",\"change|5\",\"change|15\",\"change|60\",\"change|240\",\"change\",\"change|1W\",\"change|1M\",\"Recommend.MA\",\"Recommend.All\",\"close\",\"SMA20\",\"SMA50\",\"SMA200\",\"BB.upper\",\"BB.lower\",\"description\",\"type\",\"subtype\",\"update_mode\",\"pricescale\",\"minmov\",\"fractional\",\"minmove2\",\"currency\",\"fundamental_currency_code\",\"market_cap_basic\"],\"sort\":{\"sortBy\":\"relative_volume_10d_calc\",\"sortOrder\":\"desc\"},\"price_conversion\":{\"to_symbol\":false},\"range\":[0,3000]}";
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
            List<String> mainsec = new ArrayList<>();

            Map<String, List<String>> map = new HashMap<String, List<String>>();
            Map<String, Gate2> map2 = new HashMap<String, Gate2>();

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
			Gate2 gate = new Gate2(key,object.getString("s"),"1","1");
			gate.setCategory(object.getJSONArray("d").getString(1).replaceAll("\\s+",""));
			gate.setSubcategory(object.getJSONArray("d").getString(2).replaceAll("\\s+",""));
			mainsec.add(object.getJSONArray("d").getString(2).replaceAll("\\s+",""));
			gate.setFivem(String.valueOf(object.getJSONArray("d").getDouble(6)));
			gate.setOnemin(String.valueOf(object.getJSONArray("d").getDouble(5)));
			gate.setMvbuy(String.valueOf(object.getJSONArray("d").getDouble(13)));
			gate.setTechin(String.valueOf(object.getJSONArray("d").getDouble(14)));
			
			//kafkaTemplate.send(topic4,gate);
      		System.out.println("############King############1");

			map2.put(object.getString("s"), gate);
			map.putIfAbsent(key, new ArrayList<String>());
			map.get(key).add(object.getString("s"));
            
			}
				}catch(Exception e) {
					System.out.println(e.getMessage());
					continue;
				}
			}  
			map3 = map2;
			  Map<String, Long> result
	            = sec.stream().collect(
	                Collectors.groupingBy(
	                    Function.identity(),
	                    Collectors.counting()));
			  System.out.println(result);
			  System.out.println(map);
			  for(String t:result.keySet()) {
				  Gate2 gate = new Gate2(String.valueOf(result.get(t)),t,"1","1");
		            gate.setBatch("1");
		      		kafkaTemplate2.send(topic4,gate);

			  } 
			  Map<String, Long> result2
	            = mainsec.stream().collect(
	                Collectors.groupingBy(
	                    Function.identity(),
	                    Collectors.counting()));
			  for(String t:result2.keySet()) {
				  Gate2 gate = new Gate2(String.valueOf(result2.get(t)),t,"1","1");
		            gate.setBatch("2");
		      		kafkaTemplate2.send(topic4,gate);

			  } 
			  System.out.println(result);
	      		System.out.println("############King############1"+map2);
			  System.out.println("##"+result+"##2");
			  
      		//kafkaTemplate.send(topic1,gate);
			  return result;
		} else {
			System.out.println("POST request not worked");
		}    	http.disconnect();
		return null;
				
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
	
	public static void dateFetch() throws IOException {
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

		data = extracted();
		
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
            HashMap<String, Long> map = new HashMap<String, Long>();

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
			datemap = map;
			  System.out.println(map);
		} else {
			System.out.println("POST request not worked");
		} 
    	
		System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
		http.disconnect();


	}
}
