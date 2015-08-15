package detectors;

import io.ProcessedImage;

import java.util.Hashtable;

import preprocessing.BackgroundSubtraction;
import preprocessing.ColorReduction;
import preprocessing.cannyedge.CannyOps;
import preprocessing.patches.PatchHierarchy;

import retinopathy.structures.Patch;

import tools.math.Kernels;

/**
 * Runs a feature detector on an image specified by an input directory and
 * input file name. Outputs a text file describing the image with a sequence
 * of numbers into the specified output directory.
 * 
 * @author Peter Bugaj
 */
public class FeatureDetector {
	
	/**
	 * The image being processed.
	 */
	private ProcessedImage processedImage = null;

	/**
	 * Features detected so far the the processed image.
	 */
	private ImageFeatures features = null;


	/**
	 * Creates a new instance of the Feature Detector.
	 * 
	 * @param inputDirectory
	 * The input directory containing the image.
	 * @param outputDirectory
	 * The output directory for writing the feature file to.
	 * @param imageName
	 * The name of the input image to detect and output the features for.
	 */
	public FeatureDetector(
			String inputDirectory,
			String outputDirectory,
			String imageName
			) {

		this.init(inputDirectory + "/" + imageName);
		this.features = new ImageFeatures(outputDirectory + "/" + imageName);
	}
	
	/**
	 * Run the feature detector.
	 * 
	 * @param retinopathyRating
	 * The classification label for the image.
	 * @param writeImageToFile
	 * Whether or not to visualize the features detected and to output
	 * the visualization into an image file.
	 */
	public ImageFeatures computeFeatures(String retinopathyRating, boolean writeImageToFile) {
		this.features.addToFeatureLog("RATING#" + retinopathyRating);
		this.features.addToFeatureLog("");
		
		// Extract the eye radius and area of the eye and detect the
		// unnecessary black background located around it.
		boolean [][] non_eye_image =
				new boolean[this.processedImage.getWidth()][this.processedImage.getHeight()];
		int [] eye_data = BackgroundSubtraction.findBlackBackground(
				this.processedImage.getImageSource(),
				non_eye_image, 45, 30);
		
		float non_eye_pixel_size = Math.max(1, eye_data[0]);
		int eye_radius = eye_data[1];
		
		this.features.setEyeRadius(eye_radius);


		// Find the optic nerve within the eye.
		OpticNerveDetection.findNerve(
			this.processedImage.getImageSource(), eye_radius, non_eye_image);


		// Blur the image a bit as preprocessing step one.
		this.processedImage.setImageSource(
			CannyOps.convolve(this.processedImage.getImageSource(), Kernels.blur5, CannyOps.G));


		float scaling_fac = eye_radius / 1400.0f;
		short [][][] filtered_image = null;
		for(int flip = 0; flip <= 1; flip++) {

			// Subtract the uneven background from
			// the image as preprocessing step two.
			filtered_image = new short[this.processedImage.getWidth()][this.processedImage.getHeight()][3];
			BackgroundSubtraction.substract(
				filtered_image, processedImage.getImageSource(),
				(short)150, (int)( 70 * scaling_fac )
			);
			
			// Reduce the number of colors in the
			// image as preprocessing step three.
			ColorReduction.reduceColourMonotone(filtered_image, flip == 1);
			
			// Run the patch construction algorithm
			// and produce a foreground of the eye.
			Hashtable<String, Patch> patches = PatchHierarchy.constructPatches(filtered_image, flip == 1);

			// Create a shape skeleton of the produced
			// foreground.
			ShapeSkeletonization.produceSkeleton(filtered_image, non_eye_image);

			// Analyze the shape skeleton for veins
			// and log the features.
			if(flip == 0) {
				VeinAnalyzer.Analyze(features, filtered_image, 2, 30, non_eye_pixel_size, scaling_fac);
			}

			// Analyze the shape skeleton for microaneurisms
			// and log the features.
			PatchAnalysis.findMicroaneurisms(features, filtered_image, non_eye_image, patches, scaling_fac);
		}
		if(writeImageToFile) {
			this.processedImage.setImageSource(filtered_image);
			this.processedImage.updateBufferedImageWithSoure();	
		}
		
		return features;
	}
	
	/**
	 * Set up the feature detector for reading the image.
	 * 
	 * @param imageFileName
	 * The name of the image file used in initializing the feature detector.
	 */
	private void init(String imageFileName) {
		
		// Read and store the input image for processing and reading.
		this.processedImage = new ProcessedImage();
		this.processedImage.loadImageData(imageFileName);
	}
	
	/**
	 * Write the visualized features to an output image file.
	 * 
	 * @param outputFileName
	 * The name of the image file to write the visualized features to.
	 */
	public void writeToImageFile(String outputFileName) {
		this.processedImage.writeToFile(outputFileName);
	}
}
