package detectors;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import preprocessing.ColorReduction;

import retinopathy.structures.Constants;
import retinopathy.structures.Patch;

/**
 * Class for running feature analysis algorithms on an image patch.
 * 
 * 
 * @author Peter Bugaj
 */
public class PatchAnalysis {
	
	/**
	 * The patch to be analyzed.
	 */
	private Patch patch = null;
	
	/**
	 * The curvature threshold of the patch to analyze against to help
	 * consider if this is a valid patch for an image feature of the eye.
	 */
	private double curvatureThreshold = 0;
	
	/**
	 * The average colour intensity of this patch.
	 */
	private double averageIntensity = 0;
	
	/**
	 * The size of this patchn in number of pixels.
	 */
	private int patchSize = 0;
	
	/**
	 * The strength value of this patch.
	 */
	private float patchStrength = 0;
	
	/**
	 * Indicate whether this patch being analyzed has been detected
	 * as noise within the image and not a feature of the eye.
	 */
	private boolean noise = false;
	
	/**
	 * Create a new instance of the PatchAnalysis class.
	 * 
	 * @param patch
	 * The patch to analyze.
	 * @param averageIntensity
	 * The average colour intensity of the patch.
	 * @param curvatureThreshold
	 * The curvature threshold of the patch to analyze against to help
	 * consider if this is a valid patch for an image feature of the eye.
	 * @param patchSize
	 * The size of this patch.
	 * @param patchStrength
	 * The strength value of this patch.
	 */
	public PatchAnalysis(
			Patch patch,
			double averageIntensity,
			double curvatureThreshold,
			int patchSize,
			float patchStrength) {

		this.patch = patch;
		this.averageIntensity = averageIntensity;
		this.curvatureThreshold = curvatureThreshold;
		this.patchSize = patchSize;
		this.patchStrength = patchStrength;
	}

	/**
	 * Return the patch being analyzed.
	 */
	public Patch getPatch() {
		return this.patch;
	}
	
	/**
	 * Return the curvature threshold of the patch used for considering
	 * if a patch is valid for an image feature of the eye.
	 */
	public double getCurvatureThreshold() {
		return this.curvatureThreshold;
	}
	
	/**
	 * Return the average colour intensity of the patch.
	 */
	public double getAverageIntensity() {
		return this.averageIntensity;
	}
	
	/**
	 * Return the size of the patch.
	 */
	public int getPatchSize() {
		return this.patchSize;
	}
	
	/**
	 * Return the strength of the patch.
	 */
	public float getPatchStrength() {
		return this.patchStrength;
	}
	
	/**
	 * Helper function for finding and drawing microaneurisms.
	 *
	 * @param features
	 * The data structure for recording the microaneurism features.
	 * @param filteredImage
	 * The image source matrix containing the RGB value for the image patches.
	 * @param nonEyeImage
	 * The map marking parts of the source image not belonging to the eye.
	 * @param idToPatch
	 * Structure for storing the patches found that have been marked
	 * as microaneurism eye features.
	 * @param scalingFac
	 * The scaling factor used when drawing the detected microaneurisms
	 * to an image for visualization.
	 */
	public static void findMicroaneurisms(
			ImageFeatures features,
			short[][][]filteredImage,
			boolean[][]nonEyeImage,
			Hashtable<String, Patch> idToPatch,
			float scaling_fac) {

		// Find all the patches with a high curvature value
		Vector<PatchAnalysis> analyzed_patches = analyzePatches(
				Constants.PATCH_HIGH_CURVATURE,
				idToPatch,
				nonEyeImage,
				scaling_fac);

		// Draw the patches and log their statistics.
		drawAndReportAnalyzedPatches(
				features,
				filteredImage,
				analyzed_patches);	
		features.writePatchesLogToFile();
	}
	
	/**
	 * Set the noise of this patch analyzed as being either true or false.
	 * 
	 * @param noise
	 * The noise value set.
	 */
	public void setNoise(boolean noise) {
		this.noise = noise;
	}
	
	/**
	 * Get an indication whether this patch analyzed represents noise or noise.
	 */
	public boolean isNoise() {
		return this.noise;
	}
	
	/**
	 * Analyze a set of patches given their properties and detect the ones
	 * that are microaneurisms belonging to the eye.
	 *
	 * @param curvatureThreshold
	 * The curvature threshold of the patch used to analyze against to help
	 * consider if this is a valid patch for an image feature of the eye.
	 * @param idToPatch
	 * Structure for storing the patches found that have been marked
	 * as microaneurism eye features.
	 * @param nonEyeImage
	 * The map marking parts of the source image not belonging to the eye.
	 * @param scalingFac
	 * The scaling factor used when drawing the detected microaneurisms
	 * to an image for visualization.
	 * @return
	 * The set of return patches that are classified as microaneurisms.
	 */
	public static Vector<PatchAnalysis> analyzePatches(
			double curvatureThreshold,
			Hashtable<String, Patch> idToPatch,
			boolean[][]nonEyeImage,
			float scalingFac) {

		Vector<PatchAnalysis> analyzed_patches = new Vector<PatchAnalysis>();
		Enumeration<Patch> patches = idToPatch.elements();
		Patch [] patches_array = new Patch[idToPatch.size()];
		int counter = 0;
		while(patches.hasMoreElements()) {
			Patch next_patch = patches.nextElement();	
			patches_array[counter++] = next_patch;
		}
		
		Arrays.sort(patches_array, new Comparator<Patch>(){
			public int compare(Patch a, Patch b) {
				return a.getLevel() > b.getLevel() ? -1 : a.getLevel() < b.getLevel() ? 1 : 0;
			}});

		for(int i = 0; i < patches_array.length; i++) {
			Patch next_patch = patches_array[i];
			if(isParentAnalyzed(next_patch)) {
				continue;
			} 
			//next_patch.isAnalyzed = true;
			
			float[]cent = next_patch.getCentroid();
			if(nonEyeImage[(int) cent[0]][(int) cent[1]]) continue;
	
			if(next_patch.getCurvature() < curvatureThreshold) continue;
			
			float fac = next_patch.getStackArea() / (next_patch.getArea());
			
			fac *= next_patch.getParentPatch() == null ?
				1 : next_patch.getParentPatch().getLevel() - next_patch.getLevel();

			// Get the intensity level to record.
			float average_intensity = next_patch.getAverageIntensity();
			int intensity_level = average_intensity <= ColorReduction.NUM_COLORS * 0.5 ?
				Constants.PATCH_intensity_strong : Constants.PATCH_intensity_weak;

			// Get the sharpness level to record.
			int sharpness_level = 
				1.2 < fac && fac <= 1.4 ? Constants.PATCH_strength_weak :
				1.4 < fac && fac <= 1.6 ? Constants.PATCH_strength_medium :
				1.6 < fac && fac <= 1.8 ? Constants.PATCH_strength_strong :
				1.8 < fac && fac <= 2.0 ? Constants.PATCH_strength_xstrong :
				2.0 < fac ? Constants.PATCH_strength_xxstrong : -1;
				
			if (sharpness_level == -1) continue;
			
			// Record the microaneurisms for different sizes, sharpness
			// and curvature values.
			
			// XXXXX-Large
			if(next_patch.getStackArea() * scalingFac > 6000 && next_patch.getStackArea() <= 12000 * scalingFac) {
				analyzed_patches.add(
					new PatchAnalysis(
						next_patch, intensity_level, curvatureThreshold, Constants.PATCH_size_xxxxxlarge, sharpness_level));
			}			
			
			// XXXX-Large
			if(next_patch.getStackArea() > 2500 * scalingFac && next_patch.getStackArea() <= 6000 * scalingFac) {
				analyzed_patches.add(
					new PatchAnalysis(
						next_patch, intensity_level, curvatureThreshold, Constants.PATCH_size_xxxxlarge, sharpness_level));
			}
			
			// XXX-Large
			if(next_patch.getStackArea() > 1200 * scalingFac && next_patch.getStackArea() <= 2500 * scalingFac) {
				analyzed_patches.add(
					new PatchAnalysis(
						next_patch, intensity_level, curvatureThreshold, Constants.PATCH_size_xxxlarge, sharpness_level));			
			}
			
			// XX-Large
			if(next_patch.getStackArea() > 500 * scalingFac && next_patch.getStackArea() <= 1200 * scalingFac) {
				analyzed_patches.add(
					new PatchAnalysis(
						next_patch, intensity_level, curvatureThreshold, Constants.PATCH_size_xxlarge, sharpness_level));
			}
			
			// X-Large
			if(next_patch.getStackArea() > 240 * scalingFac && next_patch.getStackArea() <= 500 * scalingFac) {
				analyzed_patches.add(
					new PatchAnalysis(
						next_patch, intensity_level, curvatureThreshold, Constants.PATCH_size_xlarge, sharpness_level));
			}
		
			// Large
			if(next_patch.getStackArea() > 120 * scalingFac && next_patch.getStackArea() <= 240 * scalingFac) {
				analyzed_patches.add(
					new PatchAnalysis(
						next_patch, intensity_level, curvatureThreshold, Constants.PATCH_size_large, sharpness_level));
			}
			
			// MEDIUM
			if(next_patch.getStackArea() > 50 * scalingFac && next_patch.getStackArea() <= 120 * scalingFac) {
				analyzed_patches.add(
					new PatchAnalysis(
						next_patch, intensity_level, curvatureThreshold, Constants.PATCH_size_medium, sharpness_level));
			}
			
			// SMALL
			if(next_patch.getStackArea() > 10 * scalingFac && next_patch.getStackArea() <= 50 * scalingFac) {
				analyzed_patches.add(
					new PatchAnalysis(
						next_patch, intensity_level, curvatureThreshold, Constants.PATCH_size_small, sharpness_level));
			}

			else {
				continue;
			}
		}
		
		return analyzed_patches;
	}
	
	/**
	 * Draw the patches and analyze information about them in the
	 * feature data structure.
	 * 
	 * @param features
	 * The feature data structure.
	 * @param filteredImage
	 * The image source matrix for the patches.
	 * @param analyzed_patches
	 * The set of patches analyzed.
	 */
	public static void drawAndReportAnalyzedPatches(
			ImageFeatures features,
			short[][][]filtered_image,
			Vector<PatchAnalysis> analyzed_patches) {

		short[] color = null;

		features.setAnalyzedPatches(analyzed_patches);
		for(int k = 0; k < analyzed_patches.size(); k++) {
			PatchAnalysis next_analyzed_patch = analyzed_patches.get(k);
			
			// Draw and log the 5X-Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_xxxxxlarge) {
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_weak) {
					color = new short[]{100, 100, 100};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_medium) {
					color = new short[]{150, 150, 150};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_strong) {
					color = new short[]{200, 200, 200};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xstrong ||
				   next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xxstrong) {
					color = new short[]{255, 255, 255};
				}
			}
			
			// Draw and log the XXXX-Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_xxxxlarge) {
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_weak) {
					color = new short[]{100, 100, 100};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_medium) {
					color = new short[]{150, 150, 150};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_strong) {
					color = new short[]{200, 200, 200};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xstrong ||
				   next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xxstrong) {
					color = new short[]{255, 255, 255};
				}
			}
			
			// Draw and log the XXX-Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_xxxlarge) {
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_weak) {
					color = new short[]{100, 100, 0};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_weak) {
					color = new short[]{150, 150, 0};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_medium) {
					color = new short[]{200, 200, 0};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xstrong ||
                   next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xxstrong) {
					color = new short[]{255, 255, 0};
				}
			}
			
			// Draw and log the XX-Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_xxlarge) {
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_weak) {
					color = new short[]{100, 0, 100};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_medium) {
					color = new short[]{150, 0, 150};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_strong) {
					color = new short[]{200, 0, 200};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xstrong ||
		           next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xxstrong) {
					color = new short[]{255, 0, 255};
				}
			}
			
			// Draw and log the X-Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_xlarge) {
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_weak) {
					color = new short[]{0, 100, 100};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_medium) {
					color = new short[]{0, 150, 150};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_strong) {
					color = new short[]{0, 200, 200};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xstrong ||
		           next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xxstrong) {
					color = new short[]{0, 255, 255};
				}
			}
			
			// Draw and log the Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_large) {
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_weak) {
					color = new short[]{0, 0, 100};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_medium) {
					color = new short[]{0, 0, 150};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_strong) {
					color = new short[]{0, 0, 200};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xstrong ||
		           next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xxstrong) {
					color = new short[]{0, 0, 255};
				}
			}
			
			// Draw and log the Medium patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_medium) {
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_weak) {
					color = new short[]{0, 100, 0};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_medium) {
					color = new short[]{0, 150, 0};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_strong) {
					color = new short[]{0, 200, 0};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xstrong ||
		           next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xxstrong) {
					color = new short[]{0, 255, 0};
				}
			}
			
			// Draw and log the Small patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_small) {
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_weak) {
					color = new short[]{100, 0, 0};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_medium) {
					color = new short[]{150, 0, 0};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_strong) {
					color = new short[]{200, 0, 0};
				}
				if(next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xstrong ||
		           next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xxstrong) {
					color = new short[]{255, 0, 0};
				}
			}

			// Draw the patch.
			drawPatch(
					filtered_image,
					next_analyzed_patch.getPatch().getBoundary().iterator(),
					next_analyzed_patch.getPatchStrength(),
					color);
		}
	}
	
	/**
	 * Helper function to determine whether the parents of the patch
	 * specified have been analyzed by previous algorithms.
	 * 
	 * @param patch
	 * The patch to check for.
	 */
	private static boolean isParentAnalyzed(Patch patch) {
		if (patch == null) {
			return false;
		}
		if(patch.getAnalysisStatus()) {
			return true;
		}
		return isParentAnalyzed(patch.getParentPatch());
	}
	
	/**Helper function for drawing a patch.
	 * 
	 * @param filteredImage
	 * The image matrix to draw the patch inside of.
	 * @param boundary
	 * The boundary of the patch to draw.
	 * @param patchStrength
	 * The strength used for drawing the patch.
	 * @param color
	 * The color to draw the patch with.
	 */
	private static void drawPatch(
			short[][][]filteredImage,
			Iterator<short[]> boundary,
			float patchStrength,
			short[]color) {

		int b_m = 3;
		int b_h = 1;
		if(patchStrength == Constants.PATCH_strength_medium) {
			b_m = 5; b_h = 2;
		}
		if (patchStrength == Constants.PATCH_strength_strong) {
			b_m = 7; b_h = 3;
		}

		while(boundary.hasNext()) {
			short[]next_point = boundary.next();
			short cx = next_point[0];
			short cy = next_point[1];

			for(int m = 0; m < b_m; m++) {
				for(int n = 0; n < b_m; n++) {

					if(n==b_h && m==b_h) continue;
					if(cx-b_h+m < 0 || cy-b_h+n < 0 || cx-b_h+m >= filteredImage.length || cy-b_h+n >= filteredImage[0].length) continue;


					if(cx-b_h+m < 0 || cx-b_h+m >= filteredImage.length) continue;
					if(cy-b_h+n < 0 || cy-b_h+n >= filteredImage[0].length) continue;
					filteredImage[cx-b_h+m][cy-b_h+n] = color;
				}
			}
		}
	}
}
