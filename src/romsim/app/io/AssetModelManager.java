/**
 * 
 */
package romsim.app.io;

import java.io.IOException;
import java.io.InputStream;

import rmcommon.io.AModelManager;
import romsim.app.Const;
import android.content.Context;
import android.util.Log;

/**
 * Central new class in the extended Version of the rbAppMIT. It wraps access to
 * any model's data and configuration files.
 * 
 * Therefore, a Source (see the SRC_* constants)
 * 
 * @author dwirtz
 * 
 */
public class AssetModelManager extends AModelManager {

	private Context c;

	private DexHelper dh;

	/**
	 * Creates a new ModelManager.
	 * 
	 * @param c
	 *            The context (i.e. current Activity)
	 */
	public AssetModelManager(Context c) {
		super();
		this.c = c;
		dh = new DexHelper(c);
	}

	/**
	 * @see rmcommon.io.AModelManager#getClassLoader()
	 */
	@Override
	public ClassLoader getClassLoader() {
		try {
			return dh.getDexClassLoader(getInStream(Const.DEX_CLASSES_JARFILE));
		} catch (IOException e) {
			Log.e("AssetModelManager", "I/O Exception during input stream creationg for file "
					+ Const.DEX_CLASSES_JARFILE + " in model " + getModelDir() + ", loaded from application assets", e);
			e.printStackTrace();
			return null;
		}
		// return new PathClassLoader("/data/data/romsim.app/files/",
		// super.getClassLoader());
	}

	@Override
	protected InputStream getInStreamImpl(String filename) throws IOException {
		return c.getAssets().open(getModelDir() + "/" + filename);
	}

	@Override
	protected String[] getFolderList() throws IOException {
		return c.getAssets().list("");
	}

	/**
	 * @see rmcommon.io.AModelManager#modelFileExists(java.lang.String)
	 */
	@Override
	public boolean modelFileExists(String filename) {
		try {
			// Faster that way
			getInStreamImpl(filename).close();
//			for (String f : c.getAssets().list(getModelDir())) {
//				if (f.equals(filename)) return true;
//			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}
