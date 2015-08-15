package Experimentation;

import io.FilePair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import detectors.*;

/**
 * Runs an experimental feature detector on a set of training images
 * and outputs a tet file for each image containing the feature data.
 * 
 * @author Peter Bugaj
 */
public class FeatureExperimentation extends Thread{

	/**
	 * The name of the file containing the training labels.
	 */
	private static final String LABEL_FILE = "trainLabels.csv";

	/**
	 * Whether or not to write the visual results to an image.
	 */
	private static final boolean writeImageToFile = false;
	
	/**
	 * Whether or not to log the features detected.
	 */
	private static final boolean logFeatures = true;
	
	/**
	 * Whether or not test images are to be read.
	 */
	private static boolean testMode = false;

	
	/**
	 * Run test cases.
	 * 
	 * @param args
	 * [0] = Directory containing the input images.
	 * [1] = Directory to output the features and visualized data to.
	 */
	public static void main(String[] args) {

		// Read in the arguments.
		String inputDir = null;
		String outputDir = null;
		int st = 0;
		int end = -1;

		if(args.length > 0) {
			inputDir = args[0];
			outputDir = args[1];
			st = Integer.parseInt(args[2]);
			end = Integer.parseInt(args[3]);
			testMode = Boolean.parseBoolean(args[4].split("_")[1]);
		} else {
			inputDir = "./TestImages/retinopathy";
			outputDir = "./TestImages";
		}

		// Read the data file about all the images
		// that need to be processed for features.
		Vector<FilePair> filePairs = readImagesDataFile(inputDir);

		// Analyze the images in left/right eye pairs and print out
		// the detected features for each image out to a log files.			
		int lim = end == -1 ? filePairs.size() : end;
		for (int i = st; i < lim && i < filePairs.size(); i++) {

			System.out.println("Percent complete: " + (int)((i-st) /(lim-st+0.0) * 100) + " %");
			FilePair next = filePairs.get(i);
			
			// Process the left image
			ImageFeatures left_features = getFeaturesForImage(inputDir, outputDir, next.getLeftName(), next.getLeftRating());			

			// Write the features of the left image to a log file.
			if(left_features != null && logFeatures) {
				left_features.writeLogToFile();
			}
			
			// Process the right image
			ImageFeatures right_features = getFeaturesForImage(inputDir, outputDir, next.getRightName(), next.getRightRating());
			
			// Write the features of the right image to a log file.
			if(right_features != null && logFeatures) {
				right_features.writeLogToFile();
			}
		}
	}

	/**
	 * Get features for the image given the image name.
	 * 
	 * @param inputDirectory
	 * The directory containing the image to be processed.
	 * @param outputDirectory
	 * The directory to write the processed features to.
	 * @param imageName
	 * The name of the image to read.
	 * @param imageRating
	 * The rating associated to the image.
	 * @return
	 * The detected features for the image.
	 */
	private static ImageFeatures getFeaturesForImage(
			String inputDirectory,
			String outputDirectory,
			String imageName,
			String imageRating) {

		long timeRight = System.currentTimeMillis();
		System.out.print("File: " + imageName + "\t");
		
		if(new File(outputDirectory + "/" + imageName.replace(".jpeg", ".txt")).isFile()) {
			System.out.println(outputDirectory + "/" + imageName + " already exists!");
			return null;
		}
		
		// Detect the features within the image.
		FeatureDetector featureDetector = new FeatureDetector(
				inputDirectory,
				outputDirectory,
				imageName);
		ImageFeatures features = featureDetector.computeFeatures(imageRating, writeImageToFile);
		
		// Write the visualized features to an image if specified.
		if(writeImageToFile) {
			featureDetector.writeToImageFile(outputDirectory + "/" + imageName);
		}
		
		// Clean up some data.
		System.gc();
		System.out.println("Finished in: " + (System.currentTimeMillis() - timeRight) + " ms\n");
		
		// Return the detected features for the image.
		return features;
	}

	/**
	 * Read the data file about all the images
	 * that need to be processed for features.
	 * 
	 * @param inputDirectory
	 * The directory containing the data file about the images.
	 */
	private static Vector<FilePair> readImagesDataFile(String inputDirectory) {

		Vector<FilePair> filePairs = new Vector<FilePair>();

		if(!testMode) {
			
			int total_pairs = 0;
			int same_pairs = 0;
			
			BufferedReader br = null;
			FileInputStream fstream = null;
			try {

				fstream = new FileInputStream(inputDirectory + "/" + LABEL_FILE);
				br = new BufferedReader(new InputStreamReader(fstream));

				String line;
				br.readLine();
				while ((line = br.readLine()) != null) {

					// Read the left image data.
					String [] leftNameSplit = line.split(",");

					String leftImageName = leftNameSplit[0] + ".jpeg";
					String leftImageRating = leftNameSplit.length == 1 ? "-1" : leftNameSplit[1];

					String left_side = leftNameSplit[0].split("_")[1];
					if(!left_side.equals("left")) {
						System.out.println("Left side skipped for the current image!");
						System.exit(0);
					}


					line = br.readLine();
					if(line == null) {
						System.out.println("Right side skipped for the current image!");
						System.exit(0);
					}


					// Read the right image data.
					String [] rightNameSplit = line.split(",");

					String rightImageName = rightNameSplit[0] + ".jpeg";
					String rightImageRating = rightNameSplit.length == 1 ? "-1" : rightNameSplit[1];

					String right_side = rightNameSplit[0].split("_")[1];
					if(!right_side.equals("right")) {
						System.out.println("Right side skipped for the current image!");
						System.exit(0);
					}

					// Verify that both left and right versions of the same image exist.
					String stripped_left = leftImageName.split("_")[0];
					String stripped_right = rightImageName.split("_")[0];
					if(!stripped_left.equals(stripped_right)) {
						System.out.println("Left and right images not aligned!");
						System.exit(0);
					}

					filePairs.add(new FilePair(leftImageName, rightImageName, leftImageRating, rightImageRating));
					total_pairs++;
					if(leftImageRating.equals(rightImageRating)) {
						same_pairs++;	
					}
				}

				br.close();
			} catch(Exception  e) {
				System.out.println("Failed to read label file");
			}
			System.out.println(same_pairs + "\t" + total_pairs);
		}
		else {
			System.out.println("Reading test data.");

			File folder = new File(inputDirectory);
			File[] listOfFiles = folder.listFiles();
			
			Hashtable<String, Vector<String>> left_right_files = new Hashtable<String, Vector<String>>();
			for (File file : listOfFiles) {
			    if (file.isFile() && file.getName().contains(".jpeg")) {
			    	
			    	String fileName = file.getName();
			    	String[]number_side_split = fileName.split("_");
			    	if(number_side_split.length != 2) {
			    		System.out.println("Invalid image files: " + file.getName());
			    		System.exit(0);
			    	}
			    	String number = number_side_split[0];
			    	if(!left_right_files.containsKey(number)) {
			    		left_right_files.put(number, new Vector<String>());
			    	}
			    	left_right_files.get(number).add(file.getName());
			    }
			}
			
			Enumeration<Vector<String>> elements = left_right_files.elements();
			while(elements.hasMoreElements()) {
				Vector<String> next_pair = elements.nextElement();
				if(next_pair.size() != 2) {
					System.out.println("Invalid image pair!");
		    		System.exit(0);
				}
				filePairs.add(new FilePair(next_pair.get(0), next_pair.get(1), "-1", "-1"));
			}
		}
		
		return filePairs;
	}
}