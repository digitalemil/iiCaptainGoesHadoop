package net.iicaptain.homeport.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hadoop.hbase.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.protobuf.ServiceException;

@Controller
public class AnalyticsController {

//	@Autowired
//	HbaseTemplate ht;
// 	Uncomment for HBase
	
	
	@Autowired
	JdbcTemplate hive;
//  For Hive. Comment for HBase
	
	@RequestMapping(value = { "/analytics/longterm" }, method = RequestMethod.GET)
	public String longterm(Model model, @RequestParam("query") String query) {
		System.out.println("Longterm Analytics.");
		System.out.println("Executing query: "+query);
		HashMap<Location, Integer> locations = new HashMap<Location, Integer>();

		try {
			hive.execute("FROM iiCaptain i INSERT OVERWRITE Table HiiCaptain Select i.user, i.longitude, i.latitude, i.timestamp");
		}
		catch(Exception e) {	
			System.err.println("Insert into HiiCaptain failed.");			
		}
		
		List<Map<String, Object>> hiverows = hive
				.queryForList(query);

		System.out.println("Results: "+hiverows);
		long total = 0;
		int locs = 0;
		for (Map map : hiverows) {
			Location loc = new Location();
			try {
				String longitude= (String)map.get("longitude");
				String latitude= (String)map.get("latitude");
					
				loc.longitude = longitude;
				loc.latitude = latitude;

				if (locations.containsKey(loc)) {
					Integer n = locations.get(loc);
					loc.n = n + 1;
					locations.remove(loc);
					locations.put(loc, loc.n);
				} else {
					locs++;
					loc.n = 1;
					locations.put(loc, 1);
				}
				total++;
			} catch (Exception e) {
				continue;
			}

		}

		StringBuffer ret = new StringBuffer("{ ;total;:" + total
				+ ", ;locations;: [");
		Set<Location> keys = locations.keySet();
		int n = 0;
		for (Location l : keys) {
			ret.append(l.toString());
			if (n < total - 1)
				ret.append(", ");
			n++;
		}
		ret.append("] }");

		model.addAttribute("locations", ret);

		return "map";
	}
	
	
//	@RequestMapping(value = { "/analytics/realtime" }, method = RequestMethod.GET)
//	public String realtime(Model model) {
//		System.out.println("realtime:");
//
//	
//		
//		List<String> rows = ht.find("iicaptain-1min", "l", new RowMapper<String>() {
//
//			@Override
//			public String mapRow(Result result, int rowNum) throws Exception {
//				NavigableMap<byte[], byte[]> map= result.getFamilyMap("l".getBytes());
//				System.out.println(new String(map.get("longitude".getBytes()))+"@"+new String(map.get("latitude".getBytes())));
//				return new String(map.get("longitude".getBytes()))+"@"+new String(map.get("latitude".getBytes()));
//			}
//		});
//
//		System.out.println("Rows: " + rows);
//		
//		HashMap<Location, Integer> locations = new HashMap<Location, Integer>();
//		long total = 0;
//		int locs = 0;
//		for (String line : rows) {
//			StringTokenizer st= new StringTokenizer(line, "@");
//			Location loc = new Location();
//			try {
//				String longitude= st.nextToken();
//				String latitude= st.nextToken();
//					
//				loc.longitude = longitude;
//				loc.latitude = latitude;
//
//				if (locations.containsKey(loc)) {
//					Integer n = locations.get(loc);
//					loc.n = n + 1;
//					locations.remove(loc);
//					locations.put(loc, loc.n);
//				} else {
//					locs++;
//					loc.n = 1;
//					locations.put(loc, 1);
//				}
//				total++;
//			} catch (Exception e) {
//				continue;
//			}
//
//		}
//	
//		StringBuffer ret = new StringBuffer("{ ;total;:" + total
//				+ ", ;locations;: [");
//		Set<Location> keys = locations.keySet();
//		int n = 0;
//		for (Location l : keys) {
//			ret.append(l.toString());
//			if (n < total - 1)
//				ret.append(", ");
//			n++;
//		}
//		ret.append("] }");
//
//		model.addAttribute("locations", ret);
//
//		return "map";
//	}
}
