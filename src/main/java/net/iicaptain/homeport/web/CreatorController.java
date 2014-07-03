package net.iicaptain.homeport.web;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.iicaptain.homeport.CreatorService;

import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;

@Controller
public class CreatorController {

	
	private CreatorService creator;

	@Autowired
	public CreatorController(CreatorService c) {
		creator = c;
	}

	@Autowired
	@Qualifier("locationsToBackend")
	MessageChannel locationsChannel;

	@Autowired
	@Qualifier("worldsToBackend")
	MessageChannel worldsChannel;

	@Autowired
	@Qualifier("locationsToKafka")
	MessageChannel locationsToKafkaChannel;
	
	@RequestMapping(value = { "/location" }, method = RequestMethod.GET)
	public @ResponseBody
	String location(
			@RequestParam("longitude") String longitude,
			@RequestParam("latitude") String latitude,
			@RequestParam(value = "altitude", required = false) String altitude,
			@RequestParam(value = "accuracy", required = false) String accuracy,
			@RequestParam(value = "altitudeAccuracy", required = false) String altitudeAccuracy,
			@RequestParam(value = "heading", required = false) String heading,
			@RequestParam(value = "speed", required = false) String speed,
			@RequestParam(value = "timestamp", required = false) String timestamp,
			@RequestParam(value = "iata", required = false) String airport,
			HttpServletRequest request, HttpServletResponse response) {

		String user = "anonymous";
		String val = null;
		if(airport!= null)
			user= airport;
		try {
			user = request.getUserPrincipal().getName();
		} catch (NullPointerException e) {

		}
		String key = user + ":" + System.currentTimeMillis();
		
		val = user + "," + request.getRemoteAddr() + "," + latitude + ","
				+ longitude + "," + altitude + "," + accuracy + ","
				+ altitudeAccuracy + "," + heading + "," + speed + ","
				+ timestamp + "," + System.currentTimeMillis();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(key, val);
		System.out.println("User: " + user+" "+val);
		
		locationsChannel.send(MessageBuilder.withPayload(map)
				.setHeader("RowKey", user).setHeader("ColumnFamily", "l")
				.build());
		locationsToKafkaChannel.send(MessageBuilder.withPayload(map).setHeader("RowKey", user).build());
		return null;
	}

	@RequestMapping(value = { "/create" }, method = RequestMethod.GET)
	public @ResponseBody
	String create(@RequestParam("width") int width,
			@RequestParam("height") int height,
			@RequestParam(value = "type", defaultValue = "js") String type,
			HttpServletRequest request, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache");

		String ret = null;
		if ("java".equals(type))
			ret = creator.createJavaWorld(width, height);
		else
			ret = creator.createWorld(width, height);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put(request.getRemoteAddr() + ":" + System.currentTimeMillis(), ret);
		String user = "anonymous";
		try {
			user = request.getUserPrincipal().getName();
		} catch (NullPointerException e) {
		}
		worldsChannel.send(MessageBuilder.withPayload(map)
				.setHeader("RowKey", user).setHeader("ColumnFamily", "w").build());
		return ret;
	}
}
