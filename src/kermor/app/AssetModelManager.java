/**
 * 
 */
package kermor.app;

import java.io.IOException;
import java.io.InputStream;

import kermor.java.io.AModelManager;
import android.content.Context;

/**
 * Central new class in the extended Version of the rbAppMIT.
 * It wraps access to any model's data and configuration files.
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
	 * @param modeldir
	 *            The name of the model folder for ASSET and SD, otherwise the
	 *            full remote url path to the remote folder
	 */
	public AssetModelManager(Context c) {
		super();
		this.c = c;
	}

	@Override
	public InputStream getInStream(String filename) throws IOException {
		return c.getAssets().open(getModelDir() + "/" + filename);
	}

	@Override
	public String[] getModelList() throws IOException {
		return c.getAssets().list("");
	}

	@Override
	public String getInfoFileURL() {
		return "file:///android_asset/" + getModelDir() + "/" + info_filename;
	}

	@Override
	public boolean fileExists(String filename) {
		try {
			for (String f : c.getAssets().list(getModelDir())) {
				if (filename.equals(f)) return true;
			}
		} catch (IOException e) {
		}
		return false;
	}

}
