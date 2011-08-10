/**
 * 
 */
package romsim.app.io;

import java.io.File;

import rmcommon.io.FileModelManager;
import android.os.Environment;

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
	public static final String SDBase = Environment
			.getExternalStorageDirectory().toString();

	/**
	 * rbAppMIT's root folder on the SD-Card
	 */
	public static final String SDModelsDir = SDBase + File.separator + SDrbAppDir;
	
	public SDModelManager() {
		super(SDModelsDir);
	}
	
	/**
	 * 
	 * @return
	 */
	public static boolean ensureSDDir() {
		File f = new File(SDModelsDir);
		if (!f.exists()) return f.mkdirs();
		return true;
	}

}
