package net.iicaptain.homeport.kafka;

import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.springframework.integration.Message;

public class KafkaAdapter {
	String brokerList, serializer, partitioner;
	int acks;
	Producer<String, String> producer;
	ProducerConfig config;

	public KafkaAdapter(String brokerList, String serializer,
			String partitioner, int acks) {
		this.brokerList = brokerList;
		this.serializer = serializer;
		this.partitioner = partitioner;
		this.acks = acks;

		Properties props = new Properties();

		props.put("metadata.broker.list", brokerList);
		props.put("serializer.class", serializer);
		props.put("partitioner.class", partitioner);
		props.put("request.required.acks", (new Integer(acks)).toString());

		config = new ProducerConfig(props);

		producer = new Producer<String, String>(config);
	}

	public void sendWorlds(Message msg) {
		String rowkey = msg.getHeaders().get("RowKey").toString() + ":"
				+ System.currentTimeMillis() + "w:map";
		sendDataToKafka(msg.getPayload().toString(), rowkey, "w", "map");
	}

	public void sendLocations(Message msg) {
		String rowkey = msg.getHeaders().get("RowKey").toString() + ":"
				+ System.currentTimeMillis() + ":l:loc";
		sendDataToKafka(msg.getPayload().toString(), rowkey, "l", "loc");
	}

	public void sendDataToKafka(String d, String rowkey, String colfamily,
			String col) {
		KeyedMessage<String, String> data = new KeyedMessage<String, String>(
				"iicaptain", rowkey, d);
		try {
			producer.send(data);
		} catch (Exception e) {
			try {
				producer.close();
			} catch (Exception e1) {

			}
			try {
				Thread.currentThread().sleep(1000);
			} catch (Exception e2) {

			}
			producer = new Producer<String, String>(config);
		}
	}
}
