package com.ics.cerebrum.nodes.xml;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 15-Feb-2022
 */

@XmlRootElement(name = "readerPool")
public class ReaderPool 
{
	private int corePoolSize;

	private int maximumPoolSize;
	
	private int keepAliveTime;


	public int getCorePoolSize() {
		return corePoolSize;
	}

	public int getMaximumPoolSize() {
		return maximumPoolSize;
	}

	public int getKeepAliveTime() {
		return keepAliveTime;
	}
	
	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}
	
	public void setMaximumPoolSize(int maximumPoolSize) {
		this.maximumPoolSize = maximumPoolSize;
	}
	
	public void setKeepAliveTime(int keepAliveTime) {
		this.keepAliveTime = keepAliveTime;
	}
	
}
