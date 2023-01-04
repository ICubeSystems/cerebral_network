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
	public String secretKey;
	
	public BootstrapData() {}
	
	public BootstrapData(String secretKey) {
		super();
		this.secretKey = secretKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	/**
	 * Builder class
	 * 
	 * @author Anshul
	 * @since 28-Jul-2022
	 */
	public static class Builder
	{
		private String secretKey;
		
		/**
		 * MAC address of the synaptic node, required for node resolution at cerebrum
		 * 
		 * @param secretKey
		 * @return
		 */
		public Builder secretKey(String secretKey) {
			this.secretKey = secretKey;
			return this;
		}
		
		public BootstrapData build() 
		{
			return new BootstrapData(this.secretKey);
		}
	}
}


