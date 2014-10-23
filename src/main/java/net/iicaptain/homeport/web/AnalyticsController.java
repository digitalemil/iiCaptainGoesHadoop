package net.iicaptain.homeport.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hadoop.hbase.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.protobuf.ServiceException;

@Controller
public class AnalyticsController {

	@Autowired
	HbaseTemplate ht;
	// Uncomment for HBase

	@Autowired
	JdbcTemplate hive;

	@RequestMapping(value = "/analytics/data", method = RequestMethod.GET)
	public @ResponseBody
	String getData() {

		List<String> rows = ht.find("iicaptain-1min", "l",
				new RowMapper<String>() {

					@Override
					public String mapRow(Result result, int rowNum)
							throws Exception {
						NavigableMap<byte[], byte[]> map = result
								.getFamilyMap("l".getBytes());
						System.out.println(new String(map.get("longitude"
								.getBytes()))
								+ "@"
								+ new String(map.get("latitude".getBytes())));
						return new String(map.get("longitude".getBytes()))
								+ "@"
								+ new String(map.get("latitude".getBytes()));
					}
				});

		System.out.println("Rows: " + rows);

		int total = 0;
		HashMap<Location, Integer> locations = new HashMap<Location, Integer>();
		for (String line : rows) {
			StringTokenizer st = new StringTokenizer(line, "@");
			Location loc = new Location();
			try {
				String longitude = st.nextToken();
				String latitude = st.nextToken();

				loc.longitude = longitude;
				loc.latitude = latitude;

				if (locations.containsKey(loc)) {
					Integer n = locations.get(loc);
					loc.n = n + 1;
					locations.remove(loc);
					locations.put(loc, loc.n);
				} else {
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
		return ret.toString();
	}

	// For Solr
	@RequestMapping(value = { "/analytics/search" }, method = RequestMethod.GET)
	public String search(
			Model model,
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam("q") String q,
			@RequestParam(value = "refresh", defaultValue = "false") String refresh) {
		HashMap<Location, Integer> locations = new HashMap<Location, Integer>();

		SolrServer server = new HttpSolrServer(
				"http://127.0.0.1:8983/solr/locations");
		System.out.println("Search Query: " + request.getQueryString());
		SolrQuery solrQuery = new SolrQuery();
		// solrQuery.setRows(8192);
		Map<java.lang.String, java.lang.String[]> params = request
				.getParameterMap();
		for (Object param : params.keySet()) {
			StringBuffer buf = new StringBuffer();
			if (param.equals("refresh")) {
				continue;
			}
			for (int i = 0; i < params.get(param).length; i++) {
				buf.append(params.get(param)[i]);
			}
			System.out.println("Param: " + param.toString() + " "
					+ buf.toString());
			solrQuery.set(param.toString(), buf.toString());
		}
		QueryResponse rsp = null;
		try {
			rsp = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<SolrDocument> iter = rsp.getResults().iterator();

		long total = 0;

		while (iter.hasNext()) {
			Location loc = new Location();
			SolrDocument resultDoc = iter.next();
			Float longitude = (Float) resultDoc.getFieldValue("longitude");
			Float latitude = (Float) resultDoc.getFieldValue("latitude");
			loc.longitude = longitude.toString();
			loc.latitude = latitude.toString();
			if (locations.containsKey(loc)) {
				Integer n = locations.get(loc);
				loc.n = n + 1;
				locations.remove(loc);
				locations.put(loc, loc.n);
			} else {
				loc.n = 1;
				locations.put(loc, 1);
			}
			total++;
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
		if (refresh.equals("true"))
			model.addAttribute("refresh", true);
		else
			model.addAttribute("refresh", false);
		model.addAttribute("forSolr", true);
		model.addAttribute("solrQuery", request.getQueryString());
		
		return "map";
	}

	
	// For Solr
		@RequestMapping(value = { "/analytics/solrData" }, method = RequestMethod.GET)
		public @ResponseBody String searchData(
				Model model,
				HttpServletRequest request,
				HttpServletResponse response,
				@RequestParam("q") String q,
				@RequestParam(value = "refresh", defaultValue = "false") String refresh) throws UnsupportedEncodingException {
			HashMap<Location, Integer> locations = new HashMap<Location, Integer>();

			SolrServer server = new HttpSolrServer(
					"http://127.0.0.1:8983/solr/locations");
			System.out.println("Search Query Data: " + request.getQueryString());
			SolrQuery solrQuery = new SolrQuery();
			// solrQuery.setRows(8192);
			Map<java.lang.String, java.lang.String[]> params = request
					.getParameterMap();
			for (Object param : params.keySet()) {
				StringBuffer buf = new StringBuffer();
				if (param.equals("refresh")) {
					continue;
				}
				for (int i = 0; i < params.get(param).length; i++) {
					buf.append(params.get(param)[i]);
				}
				
				String p= java.net.URLDecoder.decode(buf.toString(), "UTF-8").replaceAll("amp;", "");
				p=p.replace("amp;", "");
				String key= param.toString().replace("amp;", "");
				System.out.println("Param: " + key+ " "
						+ p);
				solrQuery.set(key, p);
			}
			QueryResponse rsp = null;
			try {
				rsp = server.query(solrQuery);
			} catch (SolrServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Iterator<SolrDocument> iter = rsp.getResults().iterator();

			long total = 0;

			while (iter.hasNext()) {
				Location loc = new Location();
				SolrDocument resultDoc = iter.next();
				Float longitude = (Float) resultDoc.getFieldValue("longitude");
				Float latitude = (Float) resultDoc.getFieldValue("latitude");
				loc.longitude = longitude.toString();
				loc.latitude = latitude.toString();
				if (locations.containsKey(loc)) {
					Integer n = locations.get(loc);
					loc.n = n + 1;
					locations.remove(loc);
					locations.put(loc, loc.n);
				} else {
					loc.n = 1;
					locations.put(loc, 1);
				}
				total++;
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
			return ret.toString();
		}

	
	@RequestMapping(value = { "/analytics/longterm" }, method = RequestMethod.GET)
	public String longterm(Model model, @RequestParam("query") String query) {
		System.out.println("Longterm Analytics.");
		System.out.println("Executing query: " + query);

		HashMap<Location, Integer> locations = new HashMap<Location, Integer>();
		try {
			// hive.getDataSource().getConnection().set
			hive.execute("FROM iiCaptain i INSERT OVERWRITE Table HiiCaptain Select i.user, i.longitude, i.latitude, i.timestamp");
		} catch (Exception e) {
			System.err.println("Insert into HiiCaptain failed.");
		}

		List<Map<String, Object>> hiverows = hive.queryForList(query);

		System.out.println("Results: " + hiverows);
		long total = 0;
		int locs = 0;
		for (Map map : hiverows) {
			Location loc = new Location();
			try {
				String longitude = (String) map.get("longitude");
				String latitude = (String) map.get("latitude");

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
		model.addAttribute("refresh", false);
		model.addAttribute("forSolr", false);
		return "map";
	}

	@RequestMapping(value = { "/analytics/realtime" }, method = RequestMethod.GET)
	public String realtime(Model model) {
		System.out.println("realtime:");

		List<String> rows = ht.find("iicaptain-1min", "l",
				new RowMapper<String>() {

					@Override
					public String mapRow(Result result, int rowNum)
							throws Exception {
						NavigableMap<byte[], byte[]> map = result
								.getFamilyMap("l".getBytes());
						System.out.println(new String(map.get("longitude"
								.getBytes()))
								+ "@"
								+ new String(map.get("latitude".getBytes())));
						return new String(map.get("longitude".getBytes()))
								+ "@"
								+ new String(map.get("latitude".getBytes()));
					}
				});

		System.out.println("Rows: " + rows);

		HashMap<Location, Integer> locations = new HashMap<Location, Integer>();
		long total = 0;
		int locs = 0;
		for (String line : rows) {
			StringTokenizer st = new StringTokenizer(line, "@");
			Location loc = new Location();
			try {
				String longitude = st.nextToken();
				String latitude = st.nextToken();

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
		model.addAttribute("refresh", true);
		model.addAttribute("forSolr", false);
		return "map";
	}
}

// {'topology':{'id':'4e00c80d-1eef-4f27-b656-e6a2c0c35831','name':'iicaptain-topology'},'offset':0,'partition':0,'broker':{'host':'sandbox.hortonworks.com','port':9092},'topic':'iicaptain'}
// {'topology':{'id':'0ceb86c3-c516-4d38-beb8-876654eb8d05','name':'iicaptain-topology'},'offset':0,'partition':1,'broker':{'host':'sandbox.hortonworks.com','port':9092},'topic':'iicaptain'}