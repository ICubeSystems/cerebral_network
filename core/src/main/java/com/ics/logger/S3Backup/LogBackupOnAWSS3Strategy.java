package com.ics.logger.S3Backup;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.SortedMap;

import org.apache.logging.log4j.core.appender.rolling.AbstractRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.FileExtension;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverDescription;
import org.apache.logging.log4j.core.appender.rolling.RolloverDescriptionImpl;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.FileRenameAction;
import org.apache.logging.log4j.core.appender.rolling.action.ZipCompressAction;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * Log4j2 custom strategy class, we can create custom strategy to rolling the log file.
 * 
 * @author Anshul
 * @version 1.0
 * @since Dec 12, 2022
 */
public class LogBackupOnAWSS3Strategy extends AbstractRolloverStrategy
{
	private LogBackupOnAWSS3Listner listner;
	
	protected LogBackupOnAWSS3Strategy(StrSubstitutor strSubstitutor, LogBackupOnAWSS3Listner listner) 
	{
		super(strSubstitutor);
		this.listner = listner;
	}

	@Override
	public RolloverDescription rollover(RollingFileManager manager) throws SecurityException
	{
		// 1. file index is the count of files created for this logger
		int fileIndex;
		// 2. create object of string builder
		final StringBuilder buf = new StringBuilder(255);
		// 3. get all eligible log files for conversion
		final SortedMap<Integer, Path> eligibleFiles = getEligibleFiles(manager);
		// 4. if there are zero file then set file index to 1. Otherwise set fileIndex as last created fileIndex+1 
		fileIndex = eligibleFiles.size() > 0 ? eligibleFiles.lastKey() + 1 : 1;
		// 5. generate name of file according to the manager's pattern processor and set it to the StringBuilder.
		manager.getPatternProcessor().formatFileName(strSubstitutor, buf, fileIndex);
		final String currentFileName = manager.getFileName();
		// 6. get generated name from string builder as a string
		String renameTo = buf.toString();
		// 7. create compressed name of the file
		final String compressedName = renameTo;
		// 8. create an action for compression of file
		Action compressAction = null;
		// 9. get extension of compression
		final FileExtension fileExtension = manager.getFileExtension();
		// 10. check for file extension
		if (fileExtension != null) 
		{
			// name of the file according to pattern processor without compression (Source)
            renameTo = renameTo.substring(0, renameTo.length() - (fileExtension.name().length()+1));
            // name of the file according to pattern processor with compression (Target)
			compressAction = new ZipCompressAction(source(renameTo), target(compressedName),
                    true, -1);
		}
		// 11. currentFileName is equals to renameTo then ignoring rename action
		if (currentFileName.equals(renameTo)) {
            LOGGER.warn("Attempt to rename file {} to itself will be ignored", currentFileName);
            // return RolloverDescriptionImpl with file name, append, synchronous action, asynchronous action.
            return new RolloverDescriptionImpl(currentFileName, false, null, null);
        }
		// 12. create a rename file action
        final FileRenameAction renameAction = new FileRenameAction(new File(currentFileName), new File(renameTo),
                    manager.isRenameEmptyFiles());
        // 13. set compressed file name to listner for further processing on compressed file after compression
       	listner.addFileToUploadQueue(new File(compressedName).getPath());
       	// 14. create an asyncAction to compress file.
        final Action asyncAction = merge(compressAction, new ArrayList<>(), false);
        // return RolloverDescriptionImpl with file name, append, synchronous action, asynchronous action.
        return new RolloverDescriptionImpl(currentFileName, false, renameAction, asyncAction);
	}
	
	File source(final String fileName) {
        return new File(fileName);
    }

    File target(final String fileName) {
        return new File(fileName);
    }

	public LogBackupOnAWSS3Listner getListner()
	{
		return listner;
	}
	
	public SortedMap<Integer, Path> getOldLogs(RollingFileManager manager) {
		return getEligibleFiles(manager);
	}
}
