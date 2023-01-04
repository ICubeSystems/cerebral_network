package com.ics.logger.S3Backup;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.apache.logging.log4j.core.appender.rolling.RolloverListener;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.ics.nceph.core.Configuration;

/**
 * This is a custom log4j2 listner class which handles the upload of the log.gz files to AWS S3. All but the last log.gz will be backed up to the AWS S3.
 * 
 * @author Anshul
 * @version 1.0
 * @since Dec 12, 2022
 */
public class LogBackupOnAWSS3Listner implements RolloverListener
{
	private BlockingQueue<String> uploadLogsQueue;

	private AmazonS3 s3Client;

	public LogBackupOnAWSS3Listner(){
		uploadLogsQueue = new LinkedBlockingQueue<>();
		s3Client = S3Instance.getInstance();
	}

	/**
	 * When rollover triggered this method is called.
	 */
	@Override
	public void rolloverTriggered(String fileName) {
		
	}

	/**
	 * When rollover completed this method is called.
	 */
	@Override
	public void rolloverComplete(String fileName) 
	{
		/**
		 * When the upload queue size exceeds 1, the log file will be uploaded to AWSS3.
		 * When creating new log rollover files, we use the counter value from the last created log file.
		 * Therefore, we must save the last created log file to local storage.
		 */
		if(uploadLogsQueue.size() > 1) 
		{
			File file = getLogFile(uploadLogsQueue.poll());
			// upload to AWSS3
			uploadToS3(file.getName(), file, Configuration.APPLICATION_PROPERTIES.getConfig("S3_BUCKET_NAME"), "application/x-gzip");
			// remove log from local storage
			file.delete();
		}

	}

	private File getLogFile(String filePath) 
	{
		File logFile = new File(filePath);
		if(logFile.exists())
			return logFile;
		return getLogFile(filePath);
	}

	private synchronized void uploadToS3(String key, File file, String bucket, String contentType) 
	{
		// Creating a PutObjectRequest with BUCKET_NAME, KEY, FILE.
		PutObjectRequest request = new PutObjectRequest(bucket, key, file);
		// Create metadata of request.
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType(contentType);
		// Set metadata to PutObjectRequest.
		request = request.withMetadata(metadata);
		// Upload file to s3 using putObject method.
		s3Client.putObject(request);
		Logger.getGlobal().info(key+" file uploaded to AWSS3 Successfully ");

	}

	public String getFileToUpload()
	{
		return uploadLogsQueue.peek();
	}

	public void addFileToUploadQueue(String fileToUpload)
	{
		this.uploadLogsQueue.add(fileToUpload);
	}
}
