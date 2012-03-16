/**
 * 
 */
package jarmos.app.io;

import jarmos.app.Const;
import jarmos.io.FileModelManager;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

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

	private DexHelper dh;

	/**
	 * @param c 
	 * 
	 */
	public SDModelManager(Context c) {
		super(SDModelsDir);
		dh = new DexHelper(c);
	}

	/**
	 * @see jarmos.io.FileModelManager#getClassLoader()
	 */
	@Override
	public ClassLoader getClassLoader() {
		try {
			return dh.getDexClassLoader(getInStream(Const.DEX_CLASSES_JARFILE));
		} catch (IOException e) {
			Log.e("SDModelManager", "I/O Exception during input stream creation for file "
					+ Const.DEX_CLASSES_JARFILE + " in model " + getModelDir() + ", loading from SD card", e);
			e.printStackTrace();
			return null;
		}
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
