package com.ics.nceph.core.message.data;

import java.io.Serializable;

/**
 * Data class for the {@link BootstrapMessage}
 * 
 * @author Anshul
 * @since 28-Jul-2022
 */
public class BootstrapData extends MessageData implements Serializable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * MAC address of the synaptic node, required for node resolution at cerebrum
	 */
	public String macAddress;
	
	public BootstrapData() {}
	
	public BootstrapData(String macAddress) {
		super();
		this.macAddress = macAddress;
	}

	public String getMacAddress() {
		return macAddress;
	}

	/**
	 * Builder class
	 * 
	 * @author Anshul
	 * @since 28-Jul-2022
	 */
	public static class Builder
	{
		private String macAddress;
		
		/**
		 * MAC address of the synaptic node, required for node resolution at cerebrum
		 * 
		 * @param macAddress
		 * @return
		 */
		public Builder macAddress(String macAddress) {
			this.macAddress = macAddress;
			return this;
		}
		
		public BootstrapData build() 
		{
			return new BootstrapData(this.macAddress);
		}
	}
}


