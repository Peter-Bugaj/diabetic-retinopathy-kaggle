package io;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Data structure containing information about the image being processed.
 *
 * @author Peter Bugaj
 */
public class ProcessedImage {

	/**
	 * The maximum width to process for the image. If the image
	 * is wider, crop it at the center to fit this width.
	 */
	private static final int MAX_WIDTH = 2500;

	/**
	 * The maximum height to process for the image. If the image
	 * is higher, crop it at the center to fit this height.
	 */
	private static final int MAX_HEIGHT = 2500;

	/**
	 * The image source matrix.
	 */
	private short[][][] imageSource = null;
	
	/**
	 * The data object for processing the image.
	 */
	private BufferedImage imageBuffered;

	
	/**
	 * Tool for getting pixel information from the image.
	 */
	private PixelGrabber pixelGrabber;

	/**
	 * Tool for getting pixel information from the image using the colorModel.
	 */
	private ColorModel colorModel;

	/**
	 * The width of the image.
	 */
	private int width = 0;

	/**
	 * The height of the image.
	 */
	private int height = 0;

	
	/**
	 * Load image data from an image file.
	 * 
	 * @param imageFileName
	 * The file name to load the source image from.
	 */
	public void loadImageData(String imageFileName) {
		
		File imageFile = new File(imageFileName);
		try {
			this.imageBuffered = ImageIO.read(imageFile);
		} catch (IOException e) {
			System.out.print("Failed to read image file\n");
			e.printStackTrace();
			System.exit(1);
		}
		this.width = getCroppedImageWidth(this.imageBuffered);
		this.height = getCroppedImageHeigth(this.imageBuffered);

		
		/**--------------------------------------------------------------**/
		/**Initialize the useful tools for processing the image**/
		this.pixelGrabber = new PixelGrabber(
				this.imageBuffered, 0, 0, 1, 1, false);
		try {
			this.pixelGrabber.grabPixels();
		} catch (InterruptedException e) {
			System.out.print("Failed to grab image pixels\n");
			e.printStackTrace();
			System.exit(1);
		}
		this.colorModel = this.pixelGrabber.getColorModel();


		/**--------------------------------------------------------------**/
		/**Initialize the cropped image.**/
		this.imageSource = getCroppedImage(this.imageBuffered, this.width, this.height, this.colorModel);
	}
	
	/**
	 * Set the image source.
	 */
	public void setImageSource(short[][][] imageSource) {
		this.imageSource = imageSource;
	}

	/**
	 * Get the image source.
	 */
	public short[][][] getImageSource() {
		return this.imageSource;
	}
	
	/**
	 * Set the buffered image.
	 */
	public void setImageBuffered(BufferedImage imageBuffered) {
		this.imageBuffered = imageBuffered;
	}

	/**
	 * Get the buffered image.
	 */
	public BufferedImage getImageBuffered() {
		return this.imageBuffered;
	}
	
	/**
	 * Set the pixel grabber.
	 */
	public void setPixelGrabber(PixelGrabber pixelGrabber) {
		this.pixelGrabber = pixelGrabber;
	}

	/**
	 * Get the pixel grabber.
	 */
	public PixelGrabber getPixelGrabber() {
		return this.pixelGrabber;
	}
	
	/**
	 * Set the color model.
	 */
	public void setColorModel(ColorModel colorModel) {
		this.colorModel = colorModel;
	}

	/**
	 * Get the color model.
	 */
	public ColorModel getColorModel() {
		return this.colorModel;
	}

	/**
	 * Set the image height.
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * Get the image height.
	 */
	public int getWidth() {
		return this.width;
	}
	
	/**
	 * Set the image height.
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Get the image height.
	 */
	public int getHeight() {
		return this.height;
	}
	
	/**
	 * Flush any stored data.
	 */
	public void flush() {
		this.imageBuffered.flush();
		this.imageSource = null;
	}
	
	/**
	 * Write the image source to the buffer.
	 */
	public void updateBufferedImageWithSoure() {
		try {
			this.imageBuffered = this.imageBuffered.getSubimage(0, 0,
					getCroppedImageWidth(this.imageBuffered),
					getCroppedImageHeigth(this.imageBuffered));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		for(int i = 0; i < this.width; i++) {
			for(int j = 0; j < this.height; j++) {

				int r = (int) this.imageSource[i][j][0];
				int g = (int) this.imageSource[i][j][1];
				int b = (int) this.imageSource[i][j][2];

				float new_colour [] = {256-r,256-g,256-b,1};
				this.imageBuffered.setRGB(i,j,
						colorModel.getDataElement(new_colour, 0));
			}
		}
	}
	
	/**
	 * Write the image source to an output image file.
	 */
	public void writeToFile(String outputFileName) {

		File output_file_name = new File(outputFileName);
		String output_file_format = "png";

		//writetoOutImageBuffer();

		try {
			ImageIO.write(this.imageBuffered,
					output_file_format, output_file_name);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		this.flush();
	}
	
	/**
	 * Helper function for getting the width of a cropped image.
	 */
	private static int getCroppedImageWidth(BufferedImage image) {
		return image.getWidth() < MAX_WIDTH ? image.getWidth() : MAX_WIDTH;
	}
	
	/**
	 * Helper function for getting the height of a cropped image.
	 */
	private static int getCroppedImageHeigth(BufferedImage image) {
		return image.getHeight() < MAX_HEIGHT ? image.getHeight() : MAX_HEIGHT;
	}

	/**
	 * Helper function for cropping an image.
	 */
	private static short[][][] getCroppedImage(
			BufferedImage image,
			int imageWidth,
			int imageHeight,
			ColorModel colorModel) {

		short[][][]cropped_image = new short[imageWidth][imageHeight][3];

		int i = 0; int i_done = imageWidth; int i_offset = 0;
		if(imageWidth < image.getWidth()) {
			i = (image.getWidth() - imageWidth) / 2;
			i_offset = i;
			i_done = i + imageWidth;
		}

		for(; i < i_done; i++) {
			int j = 0; int j_done = imageHeight; int j_offset = 0;
			if(imageHeight < image.getHeight()) {
				j = (image.getHeight() - imageHeight) / 2;
				j_offset = j;
				j_done = j + imageHeight;
			}

			for(; j < j_done; j++) {
				int rgb = image.getRGB(i,j);

				cropped_image[i - i_offset][j-j_offset][0] = (short)colorModel.getRed(rgb);;
				cropped_image[i - i_offset][j-j_offset][1] = (short)colorModel.getGreen(rgb);
				cropped_image[i - i_offset][j-j_offset][2] = (short)colorModel.getBlue(rgb);
			}
		}
		
		return cropped_image;
	}
}
