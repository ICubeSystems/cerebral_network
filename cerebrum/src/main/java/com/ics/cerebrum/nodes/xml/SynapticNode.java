package com.ics.cerebrum.nodes.xml;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 15-Feb-2022
 */
@XmlRootElement(name = "synapticNode")
public class SynapticNode 
{
	private int port;
	
	private String name;
	
	private ReaderPool readerPool;
	
	private WriterPool writerPool;
	
	private Subscriptions subscriptions;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ReaderPool getReaderPool() {
		return readerPool;
	}

	public void setReaderPool(ReaderPool readerPool) {
		this.readerPool = readerPool;
	}

	public WriterPool getWriterPool() {
		return writerPool;
	}

	public void setWriterPool(WriterPool writerPool) {
		this.writerPool = writerPool;
	}

	public Subscriptions getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(Subscriptions subscriptions) {
		this.subscriptions = subscriptions;
	}
}
