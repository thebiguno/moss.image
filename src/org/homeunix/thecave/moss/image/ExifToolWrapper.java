/*
 * Created on Dec 27, 2006 by wyatt
 */
package org.homeunix.thecave.moss.image;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author wyatt
 *
 * A wrapper class for the excellent ExifTool utility by Phil Harvey 
 * (see http://owl.phy.queensu.ca/~phil/exiftool/).  Allows you to read
 * and write tags to files via a normal Java interface.
 * 
 * This class can currently read binary data, but cannot write it.  That
 * functionality may be added in a later release.
 * 
 */
public class ExifToolWrapper {

	private File exifTool;
	private static Logger logger = Logger.getLogger("org.homeunix.thecave.moss");

	//When printing the values of multiple files, this is the 
	// start of the line which identifies the file name.
	private final String EXIFTOOL_FILE_HEADER = "========";

	/**
	 * Creates a new ExifTool wrapper, pointing to the ExifTool command 
	 * line file at the given location.
	 * @param exifTool The exiftool command line utility
	 * @throws FileNotFoundException
	 */
	public ExifToolWrapper(File exifTool) throws FileNotFoundException {
		if (exifTool.exists() && exifTool.isFile() && exifTool.canRead()){
			this.exifTool = exifTool;
		}
		else {
			throw new FileNotFoundException("I cannot find the exiftool utility at " + exifTool.getAbsolutePath() + " (or, perhaps the file exists, but I do not have read permissions to it).");
		}
	}

	/**
	 * Returns a byte array of a binary tag with the given name.  Can be
	 * used to load embedded thumbnails, etc
	 * @param image File to load from
	 * @param tagName Name of the tag to load
	 * @param maxSize The maximum size of the value.  You can either approximate this, or get the exact value from the getTagsFromFile(), and parse the exact number.
	 * @return
	 */
	public byte[] getBinaryTagFromFile(File image, String tagName, int maxSize){
		byte[] tagValue = new byte[maxSize];

		List<String> command = new LinkedList<String>();
		command.add(exifTool.getAbsolutePath());
		command.add("-b");
		command.add("-" + tagName);
		command.add(image.getAbsolutePath());

		try{
			Process p = Runtime.getRuntime().exec(command.toArray(new String[0]));
			BufferedInputStream bis = new BufferedInputStream(p.getInputStream());

			int offset = 0, ret = 0, bufferSize = 1024;
			byte[] temp = new byte[bufferSize];
			while ((ret = bis.read(temp, 0, temp.length)) != -1){
				for (int i = 0; i < temp.length; i++) {
					tagValue[offset + i] = temp[i];
				}
				offset += ret;
			}


		}
		catch (IOException ioe){
			logger.log(Level.SEVERE, "Unable to connect to process's console", ioe);
		}

		return tagValue;
	}

	public Map<File, byte[]> getBinaryTagFromFiles(Collection<File> files, String tagName){
		Map<File, byte[]> binaryTags = new HashMap<File, byte[]>();

		Set<String> tagNames = new HashSet<String>();
		tagNames.add(tagName);
		Map<File, Map<String, String>> sizesRaw = getTagsFromFiles(files, tagNames);
		Map<File, Integer> tagSizeByFile = new HashMap<File, Integer>();

		List<String> command = new LinkedList<String>();
		command.add(exifTool.getAbsolutePath());
		command.add("-b");
		command.add("-" + tagName);
		for (File file : files) {
			command.add(file.getAbsolutePath());	

			Map<String, String> sizeSet = sizesRaw.get(file);
			if (sizeSet != null) {
				String bufferSizeString = sizeSet.get(tagName);

				if (bufferSizeString != null){
					bufferSizeString = bufferSizeString.replaceAll("\\D", "");
					if (bufferSizeString != null && bufferSizeString.length() > 0){
						int bufferSize = Integer.parseInt(bufferSizeString);
						tagSizeByFile.put(file, bufferSize); 
					}
//					else {
//						Log.error("Nothing left in bufferSizeString after removing non-digits");
//					}
				}
//				else {
//					Log.error("No bufferSizeString for tagName " + tagName + " in file " + file);
//				}
			}
//			else {
//				Log.error("No sizeSet for file " + file);
//			}
		}

		//Find out the largest thumbnail size in bytes
//		Vector<Integer> sortedTagSizes = new Vector<Integer>(tagSizeByFile.values()); 
//		Collections.sort(sortedTagSizes);
//		int largestTag = sortedTagSizes.lastElement();
//		Log.info("Largest tag is " + largestTag);

		try{
			Process p = Runtime.getRuntime().exec(command.toArray(new String[0]));
			BufferedInputStream bis = new BufferedInputStream(p.getInputStream());

			//We assume that a) exiftool returns results for files in 
			// the same order that we passed them in, and b) that 
			// the foreach loop iterates through the files in the same
			// order every time.
			for (File file : files) {
				if (tagSizeByFile.get(file) != null){
					int bufferSize = tagSizeByFile.get(file);

					byte[] binaryTag = readBytes(bis, bufferSize);
					binaryTags.put(file, binaryTag);
				}
			}

		}
		catch (IOException ioe){
			logger.log(Level.SEVERE, "Unable to connect to process's console", ioe);
		}

		return binaryTags;
	}

	/**
	 * Returns a map of the given tag names and values for all given
	 * files.  This can be used for all non-binary tags that are 
	 * given via ExifTool.
	 * @param images The images to read from
	 * @param tagNames A set of all the names of tags to read.
	 * @return A map of tag names to values.  Values can be empty, but should not be null.
	 */
	public Map<File, Map<String, String>> getTagsFromFiles(Collection<File> images, Set<String> tagNames){
		Map<File, Map<String, String>> fileToTagValues = new HashMap<File, Map<String, String>>();

		if (images.size() == 0){
			logger.log(Level.WARNING, "ExifToolWrapper: No files to read.  Returning empty Map.");
			return fileToTagValues;
		}

		List<String> command = new LinkedList<String>();
		command.add(exifTool.getAbsolutePath());
		command.add("-S");
		command.add("-n");
		for (String tagName : tagNames) {
			command.add("-" + tagName);
		}
		for (File image : images) {
			command.add(image.getAbsolutePath());	
		}


		try{
			Process p = Runtime.getRuntime().exec(command.toArray(new String[0]));
			BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(p.getInputStream()))); 

			String line;
			File currentFile = null;
			String value;
			
			if (images.size() == 1){
				currentFile = images.toArray(new File[1])[0];
				value = null;
				fileToTagValues.put(currentFile, new HashMap<String, String>());					
			}
			
			while ((line = br.readLine()) != null){
//				Log.debug("Read line '" + line + "'");
				if (line.matches(EXIFTOOL_FILE_HEADER + ".*")){
					currentFile = new File(line.replaceAll(EXIFTOOL_FILE_HEADER, "").trim());
					value = null;
					fileToTagValues.put(currentFile, new HashMap<String, String>());
				}
				else if (line.matches("[^:]+:.+")){
					value = line.replaceFirst("[^:]+:", "").trim();
				}
				else {
					value = null;
				}

				String[] split = line.split(":");
				String tag = split[0].trim();

				if (currentFile != null){
					if (fileToTagValues.get(currentFile) != null){
						if (tagNames.contains(tag)){
							fileToTagValues.get(currentFile).put(tag, value);
//							Log.debug(tag + " = " + value + " in file " + currentFile.getAbsolutePath());
						}
					}
				}
			}
		}
		catch (IOException ioe){
			logger.log(Level.SEVERE, "Unable to connect to process's console", ioe);
		}

		return fileToTagValues;
	}

	/**
	 * @param image
	 * @param tagNames
	 * @return
	 */
	public Map<String, String> getTagsFromFile(File image, Set<String> tagNames) {
		Set<File> file = new HashSet<File>();
		file.add(image);

		return getTagsFromFiles(file, tagNames).get(image);
	}


	/**
	 * Sets the given tags to the file. 
	 * @param image File to save tags to
	 * @param tags Map of tags and associated values
	 * @return All the output of the exiftool command (TODO: boolean value to see if write was successful or not)
	 */
	public String setTagsToFiles(Collection<File> images, Map<String, String> tags){
		//TODO Return boolean value indicating whether write was successful or not. 
		StringBuffer returnValue = new StringBuffer();

		List<String> command = new LinkedList<String>();
		command.add(exifTool.getAbsolutePath());
		command.add("-overwrite_original_in_place");
		command.add("-n");
		for (String tagName : tags.keySet()) {
			command.add("-" + tagName + "=" + tags.get(tagName));
		}
		for (File image : images) {
			command.add(image.getAbsolutePath());
		}

		try{
			Process p = Runtime.getRuntime().exec(command.toArray(new String[1]));
			BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(p.getInputStream()))); 

			String line;
			while ((line = br.readLine()) != null){
				returnValue.append(line).append("\n");
			}
		}
		catch (IOException ioe){
			logger.log(Level.SEVERE, "Unable to connect to process's console", ioe);
		}

		return returnValue.toString();
	}

	public String setTagsToFile(File image, Map<String, String> tags){
		Set<File> file = new HashSet<File>();
		file.add(image);

		return setTagsToFiles(file, tags);
	}
	
	/**
	 * Sets the given tags in the scpecified images using a data file.  We do 
	 * ABSOLUTELY NO SANITY CHECKS that the tags you specify can be
	 * properly read in from the files you specify. Be sure you check 
	 * that you are writing the correct values to the correct images,
	 * or you will lose data! 
	 * @param images Set of images to update
	 * @param tags Set of tags and files to read each tag from.
	 * @return
	 */
	public String setTagsToFilesFromDatafile(Set<File> images, Map<String, File> tags){
		//TODO Return boolean value indicating whether write was successful or not. 
		StringBuffer returnValue = new StringBuffer();

		List<String> command = new LinkedList<String>();
		command.add(exifTool.getAbsolutePath());
		command.add("-overwrite_original_in_place");
		command.add("-n");
		for (String tagName : tags.keySet()) {
			command.add("-" + tagName + "<=" + tags.get(tagName).getAbsolutePath());
		}
		for (File image : images) {
			command.add(image.getAbsolutePath());
		}

		try{
			Process p = Runtime.getRuntime().exec(command.toArray(new String[1]));
			BufferedReader br = new BufferedReader(
					new InputStreamReader(new BufferedInputStream(
							p.getInputStream()))); 

			String line;
			while ((line = br.readLine()) != null){
				returnValue.append(line).append("\n");
			}
		}
		catch (IOException ioe){
			logger.log(Level.SEVERE, "Unable to connect to process's console", ioe);
		}

		return returnValue.toString();
	}
	
	public String setTagsToFileFromDatafile(File image, Map<String, File> tags){
		Set<File> file = new HashSet<File>();
		file.add(image);

		return setTagsToFilesFromDatafile(file, tags);
	}

	/**
	 * Reads the specified number of bytes from the buffered input stream, 
	 * starting at offset.
	 * @param bis
	 * @param lenth
	 * @return
	 */
	private byte[] readBytes(BufferedInputStream bis, int length) throws IOException {
//		int position = 0;
		byte[] binaryTag = new byte[length];
//		byte[] temp = new byte[1024];

		for (int i = 0; i < length; i++) {
			binaryTag[i] = (byte) bis.read();
		}
//		ret = bis.read();//temp, offset, temp.length);
//		if (ret == binaryTag.length){
//		offset += ret;
//		}
//		else {
//		Log.critical("Did not read complete tag for the file " + file + "; only read " + ret + " bytes.");
//		}

		return binaryTag;
	}

//	public String getTagFromFile(File image, String tagName){
//	String tagValue = null;

//	String[] fileToRun = {
//	exifTool.getAbsolutePath(),
//	"-S",
//	"-" + tagName,
//	image.getAbsolutePath()
//	};

//	try{
//	Process p = Runtime.getRuntime().exec(fileToRun);
//	BufferedReader br = new BufferedReader(
//	new InputStreamReader(new BufferedInputStream(
//	p.getInputStream()))); 

//	tagValue = br.readLine();
//	if (tagValue != null){
//	tagValue = tagValue.replaceFirst(tagName + "\\s*:", "").trim();
//	}
////	else {}
//	}
//	catch (IOException ioe){
//	Log.error(ioe);
//	}

//	if (tagValue == null)
//	tagValue = "";

//	return tagValue;
//	}

//	public void setTagToFile(File image, String tagName, String tagValue){
//	String[] fileToRun = {
//	exifTool.getAbsolutePath(),
//	"-q",
//	"-overwrite_original_in_place",
//	"-" + tagName + "=" + tagValue + "",
//	image.getAbsolutePath()
//	};

//	try{
//	Runtime.getRuntime().exec(fileToRun).waitFor();
//	}
//	catch (IOException ioe){
//	Log.error(ioe);
//	}
//	catch (InterruptedException ie){
//	}
//	}

//	public void setTagsToFile(File image, String tagName, Set<String> tagValues){
//	setTagToFile(image, tagName, setToString(tagValues));
//	}

//	public Set<String> getTagsFromFile(File image, String tagName){
//	return stringToSet(getTagFromFile(image, tagName));
//	}
}
