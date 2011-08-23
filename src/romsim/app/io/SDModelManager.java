/**
 * 
 */
package romsim.app.io;

import java.io.File;

import rmcommon.io.FileModelManager;
import romsim.app.activity.MainActivity;
import android.os.Environment;
import dalvik.system.DexClassLoader;

/**
 * @author Ernst
 * 
 */
public class SDModelManager extends FileModelManager {

	/**
	 * SD-Card sub directory
	 */
	private static final String SDrbAppDir = "romsim_models";

	/**
	 * SD Card base directory string
	 */
	public static final String SDBase = Environment.getExternalStorageDirectory().toString();

	/**
	 * rbAppMIT's root folder on the SD-Card
	 */
	public static final String SDModelsDir = SDBase + File.separator
			+ SDrbAppDir;

	/**
	 * 
	 */
	public SDModelManager() {
		super(SDModelsDir);
	}

	/**
	 * @see rmcommon.io.FileModelManager#getClassLoader()
	 */
	@Override
	public ClassLoader getClassLoader() {
		// return new PathClassLoader(getFullModelPath(),
		// getClass().getClassLoader());//super.getClassLoader());
		return new DexClassLoader(getFullModelPath() + "AffineFunctions.jar", MainActivity.AppDataDirectory, null, getClass().getClassLoader());
	}

	/**
	 * 
	 * @return True if the directory could be ensured, false otherwise
	 */
	public static boolean ensureSDDir() {
		File f = new File(SDModelsDir);
		if (!f.exists()) return f.mkdirs();
		return true;
	}
}
