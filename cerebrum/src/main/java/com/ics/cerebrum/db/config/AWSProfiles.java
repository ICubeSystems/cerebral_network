package com.ics.cerebrum.db.config;

import lombok.Getter;

/**
 * AWS credential profiles ENUM
 * @author Anshul
 * @version V_6_0 
 * @since Mar 29, 2023
 */
@Getter
public enum AWSProfiles
{
	
	UAT("test"),
	LOCAL("local"),
	PROD("production");

	String profile;
	
	AWSProfiles(String profile) {
		this.profile = profile;
	}
	
}
