package com.ics.logger.S3Backup;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.FileSize;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

import com.ics.nceph.core.Configuration;

/**
 * Size based triggering policy that causes mandatory rollover after MAX_FILE_SIZE of file reached.
 * 
 * @author Anshul
 * @version 1.0
 * @since 01 Dec, 2022
 */
@Plugin(name = "LogBackupOnAWSS3Policy", category = "Core", printObject = true)
public class LogBackupOnAWSS3Policy implements TriggeringPolicy 
{
	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // let 10 MB the default max size
	private final long maxFileSize;
	private LogBackupOnAWSS3Listner listner;
	private RollingFileManager manager;

	/**
	 * Constructs a new instance.
	 */
	protected  LogBackupOnAWSS3Policy() {
		this.maxFileSize = MAX_FILE_SIZE;
	}

	/**
	 * Constructs a new instance.
	 * @param maxFileSize rollover threshold size in bytes.
	 */
	protected LogBackupOnAWSS3Policy(final long maxFileSize) 
	{
		this.maxFileSize = maxFileSize;
	}

	public long getMaxFileSize()
	{
		return maxFileSize;
	}

	/**
	 * Initialize the policy.
	 * @param manager The RollingFileManager.
	 */
	@Override
	public void initialize(final RollingFileManager manager) 
	{
		this.manager = manager;
		if(Boolean.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("log.savetos3"))) 
		{
			// create object of rollover listner
			listner = new LogBackupOnAWSS3Listner();
			// create object of rollover strategy
			LogBackupOnAWSS3Strategy strategy = new LogBackupOnAWSS3Strategy(new StrSubstitutor(), listner);
			// if local storage contains compressed log file then add it to upload queue of rollover listner.
			strategy.getOldLogs(manager).values().stream().forEach(logPath ->{
				// add old logs to upload queue. this case arrives after restart of application.
				listner.addFileToUploadQueue(logPath.toString());
			});
			// set listner to rolling manager
			manager.addRolloverListener(listner);
			// set strategy to rolling manager
			manager.setRolloverStrategy(strategy);
		}
	}

	/**
	 * Determine whether a rollover should occur.
	 * @param event A reference to the currently event.
	 * @return true if a rollover should occur.
	 */
	@Override
	public boolean isTriggeringEvent(final LogEvent event) 
	{
		final boolean triggered = manager.getFileSize() > maxFileSize;
		if (triggered) 
			manager.getPatternProcessor().updateTime();
		return triggered;
	}

	@Override
	public String toString() 
	{
		return "SizeBasedAwsTriggeringPolicy(size=" + maxFileSize + ")";
	}

	/**
	 * Create a AwsTriggringPolicy.
	 * @param size The size of the file before rollover is required.
	 * @return a AwsTriggringPolicy.
	 */
	@PluginFactory
	public static LogBackupOnAWSS3Policy createPolicy(@PluginAttribute("size") final String size) 
	{
		final long maxSize = size == null ? MAX_FILE_SIZE : FileSize.parse(size, MAX_FILE_SIZE);
		return new LogBackupOnAWSS3Policy(maxSize);
	}
}
