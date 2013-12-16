package com.hortonworks.digitalemil.flume;

import org.apache.flume.Event;
import org.apache.flume.api.RpcClient;
import org.apache.flume.api.RpcClientFactory;
import org.apache.flume.event.EventBuilder;
import org.springframework.integration.Message;

import java.nio.charset.Charset;
import java.util.Map;

class MyRpcClientFacade {
	private RpcClient client;
	private String hostname;
	private int port;

	public MyRpcClientFacade(String hostname, String port) {
		// Setup the RPC connection
		this.hostname = hostname;
		this.port = new Integer(port).intValue();
	
		this.client = RpcClientFactory.getDefaultInstance(hostname,
				this.port);
	}

	public void sendWorlds(Message msg) {
		String rowkey = msg.getHeaders().get("RowKey").toString() + ":"
				+ System.currentTimeMillis();
		sendDataToFlume(msg.getPayload().toString(), rowkey, "w", "map");
	}

	public void sendLocations(Message msg) {
		String rowkey = msg.getHeaders().get("RowKey").toString() + ":"
				+ System.currentTimeMillis();
		sendDataToFlume(msg.getPayload().toString(), rowkey, "l", "loc");
	}

	public void sendDataToFlume(String data, String rowkey,
			String colfamily, String col) {
		// Create a Flume Event object that encapsulates the sample data
		Event event = EventBuilder.withBody(data, Charset.forName("UTF-8"));
		Map headers = event.getHeaders();
		headers.put("RowKey", rowkey);
		headers.put("ColumnFamily", colfamily);
		headers.put("Column", col);

		// Send the event
		try {
			client.append(event);
		} catch (Exception e) {
			// clean up and recreate the client
			client.close();
			client = null;
			client = RpcClientFactory.getDefaultInstance(hostname, port);
			// Use the following method to create a thrift client (instead of
			// the above line):
			// this.client = RpcClientFactory.getThriftInstance(hostname, port);
		}
	}

	public void cleanUp() {
		// Close the RPC connection
		client.close();
	}

}