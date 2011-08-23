/**
 * 
 */
package romsim.app.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import dalvik.system.DexClassLoader;

import rmcommon.io.AModelManager;
import romsim.app.activity.MainActivity;
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

	/**
	 * Creates a new ModelManager.
	 * 
	 * @param c
	 *            The context (i.e. current Activity)
	 */
	public AssetModelManager(Context c) {
		super();
		this.c = c;
	}

	/**
	 * @see rmcommon.io.AModelManager#getClassLoader()
	 */
	@Override
	public ClassLoader getClassLoader() {
		// Create a local copy for dex optimization needs write access
		String file = "AffineFunctions.jar";
		try {
			FileOutputStream f = c.openFileOutput(file, Context.MODE_WORLD_READABLE);

			byte[] buffer = new byte[1024];
			int len1 = 0;
			InputStream in = getInStream(file);
			while ((len1 = in.read(buffer)) > 0) {
				f.write(buffer, 0, len1);
			}
			in.close();
			Log.d("AndroidModelClassLoader", "Finished preparing local copy of model's "
					+ file);
		}
		catch (IOException e) {
			Log.e("AssetModelManager", "Failed preparing local copy of model's "
					+ file + " (" + getModelDir() + ")", e);
		}

		return new DexClassLoader(MainActivity.AppDataDirectory + "/" + file, MainActivity.AppDataDirectory, null, getClass().getClassLoader());
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
			for (String f : c.getAssets().list(getModelDir())) {
				if (filename.equals(f)) return true;
			}
		}
		catch (IOException e) {}
		return false;
	}

}
