package net.iicaptain.homeport.kafka;

import kafka.producer.Partitioner;

public class iiCaptainPartitioner implements Partitioner {

	@Override
	public int partition(Object arg0, int nump) {
		return arg0.hashCode()%nump;
	}


}