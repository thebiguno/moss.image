/*
 * Created on Aug 28, 2005 by wyatt
 */
package ca.digitalcave.moss.image;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class ImageFunctions {
	
	private static Logger logger = Logger.getLogger("org.homeunix.thecave.moss");
	
	/**
	 * Scales the image.  Is relatively slow for large images, but should 
	 * be fine for smaller ones.
	 * @param img
	 * @param maxSize
	 * @return
	 */
	public static BufferedImage scaleImage(BufferedImage img, int maxSize){
		if (img == null)
			return null;

		//We create a new image of type RGB to significantly speed up scale operations on larger images  
		BufferedImage tempImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = tempImg.getGraphics();
		g.drawImage(img, 0, 0, null);
		img = tempImg;
		
		int width = img.getWidth();
		int height = img.getHeight();
		if (width > height){
			width = maxSize;
			height = -1;
		}
		else{
			height = maxSize;
			width = -1;
		}

		return getBufferedImage(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
	}
	
	/**
	 * Scales the image using Java2D.  Size is the sum of the desired width and height.
	 * @param img
	 * @param size
	 * @return
	 */
//	public static BufferedImage scale(BufferedImage img, int size){
//		AffineTransform tx = new AffineTransform();
//		
//		if (img == null)
//			return null;
//		
//		int currentSize = img.getWidth() + img.getHeight();
////		double widthToHeight = img.getWidth() / img.getHeight();
//		double scale = ((double) size) / currentSize;
//		tx.setToScale(scale, scale);
////		AffineTransform tx = AffineTransform.getRotateInstance(Math.toRadians(degrees));
//		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
//		img = op.filter(img, null);
//		return img;
//	}
	
	/**
	 * Scales the image.  Is relatively slow for larger ones, but
	 * should be fine for smaller ones.
	 * @param img
	 * @param width
	 * @param height
	 * @return
	 */
	public static BufferedImage scaleImage(BufferedImage img, int width, int height){
		return getBufferedImage(img.getScaledInstance(width, height, Image.SCALE_FAST));
	}
	
	/**
	 * Converts from Image to BufferedImage.  To go the other direction,
	 * you can just cast BufferedImage as Image.
	 * @param img The Image to convert to BufferedImage
	 * @return A BufferedImage representation of the Image
	 */
	public static BufferedImage getBufferedImage(Image img){
		BufferedImage bi = new BufferedImage(img.getWidth(null),img.getHeight(null),BufferedImage.TYPE_INT_RGB);
	    Graphics bg = bi.getGraphics();
	    bg.drawImage(img, 0, 0, null);
	    bg.dispose();
	    return bi;
	}
	
	/**
	 * Rotates the given BufferedImage the specified number of degrees.
	 * @param img The BufferedImage to rotate
	 * @param degrees The number of degrees to rotate
	 * @return A rotated copy of the BufferedImage 
	 */
	public static BufferedImage rotate(BufferedImage img, int degrees){
		AffineTransform tx = new AffineTransform();
//		tx.setToTranslation(tx, ty)
		tx.rotate(Math.toRadians(degrees), img.getHeight() / 2.0, img.getWidth() / 2.0);
//		AffineTransform tx = AffineTransform.getRotateInstance(Math.toRadians(degrees));
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		img = op.filter(img, null);
		return img;
	}
	
	public static BufferedImage getBufferedImage(File file){
		try {
			return ImageIO.read(file);
		}
		catch (IOException ioe){
			logger.log(Level.WARNING, "Problem encountered while reading image from file", ioe);
			return null;
		}
	}
	
	/**
	 * Get a BufferedImage representation of a component.  This allows
	 * you to make 'screenshots' of components easily. 
	 * @param c The component which you want an image of
	 * @return A BufferedImage of the given component
	 */
//	public static BufferedImage getBufferedImage(Component c) {
//		BufferedImage img = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
//		Graphics g = img.getGraphics();
//		c.paint(g);
//		
//		return img;
//	}
	
	/**
	 * Converts the given image, and caches it to the given destination.  The ImageParameters
	 * will determine the size and quality of the converted image.  The written file will be in 
	 * the same format as the original, as determined by the extension, with the image size 
	 * and quality as specified (if appropriate).
	 * @param originalFile
	 * @param destFile
	 * @param maxSize
	 * @param quality
	 */
//	public static void convertImage(final File originalFile, final File destFile, final int maxSize, final float quality){
//		try{
//			BufferedImage bi = ImageIO.read(originalFile);
//			bi = scaleImage(bi, maxSize);
//
//			//Write it.
//			writeImage(bi, destFile, quality);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	/**
	 * Writes the given buffered image to disk, using the given quality (if appropriate).
	 * The file type is determined by the extension of imageFile.
	 * @param bi
	 * @param imageFile
	 * @param quality
	 */
	public static void writeImage(BufferedImage bi, OutputStream os, float quality, String extension) {
		if (bi == null){
			logger.log(Level.WARNING, "Buffered Image is null!  Cannot write image to file");
			return;
		}
			
		try {
			//Get the ImageWriter which allows us to set quality, and write the image
			Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(extension);
			ImageWriter imageWriter = writers.next();
			ImageWriteParam imageWriterParam = imageWriter.getDefaultWriteParam();
			imageWriterParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			imageWriterParam.setCompressionQuality(quality);
			ImageOutputStream output = new MemoryCacheImageOutputStream(os);
			IIOImage image = new IIOImage(bi, null, null);
			imageWriter.setOutput(output);
			imageWriter.write(null, image, imageWriterParam);
		}
		catch (IOException ioe){
			logger.log(Level.SEVERE, "Problem encountered while writing image to file", ioe);
		}
	}
	
//	public static void writeImage(BufferedImage bi, File imageFile, float quality) {
//		if (bi == null){
//			logger.log(Level.WARNING, "Buffered Image is null!  Cannot write image to file");
//			return;
//		}
//			
//		try {
//			//Ensure we have a folder to write to
//			imageFile.getParentFile().mkdirs();
//
//			//Get the ImageWriter which allows us to set quality, and write the image
//			writeImage(bi, new FileOutputStream(imageFile), quality, imageFile.getName().replaceAll("^.*\\.(\\w+)", "$1"));
//		}
//		catch (IOException ioe){
//			logger.log(Level.SEVERE, "Problem encountered while writing image to file", ioe);
//		}
//	}
}
