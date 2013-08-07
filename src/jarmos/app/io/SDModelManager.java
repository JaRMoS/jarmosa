package jarmos.app.io;

import jarmos.app.Const;
import jarmos.io.FileModelManager;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * Class for model loading from the local SD card of the mobile device running android
 * 
 * @author Daniel Wirtz @date 2013-08-07
 * 
 */
public class SDModelManager extends FileModelManager {

	/**
	 * SD-Card sub directory
	 */
	private static final String JARMOSA_SD_DIR = "jarmosa_models";

	/**
	 * SD Card base directory string
	 */
	public static final String SD_BASE_DIR = Environment.getExternalStorageDirectory().toString();

	/**
	 * rbAppMIT's root folder on the SD-Card
	 */
	public static final String SD_MODEL_DIR = SD_BASE_DIR + File.separator
			+ JARMOSA_SD_DIR;

	private DexHelper dh;

	/**
	 * @param c 
	 * 
	 */
	public SDModelManager(Context c) {
		super(SD_MODEL_DIR);
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
		File f = new File(SD_MODEL_DIR);
		if (!f.exists()) return f.mkdirs();
		return true;
	}
	
	@Override
	protected String getLoadingMessage() {
		return "Reading SD card models";
	}
}
