package jarmos.app.io;

import jarmos.app.Const;
import jarmos.io.AModelManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import android.content.Context;
import android.util.Log;

/**
 * Class to load models from application assets
 * 
 * This class has been taken from the original @ref rbappmit package and modified to fit into the current JaRMoS
 * framework.
 * 
 * @author Daniel Wirtz @date 2013-08-07
 * 
 */
public class AssetModelManager extends AModelManager {

	private Context c;

	private DexHelper dh;

	/**
	 * Creates a new ModelManager.
	 * 
	 * @param c
	 * The context (i.e. current Activity)
	 */
	public AssetModelManager(Context c) {
		super();
		this.c = c;
		dh = new DexHelper(c);
	}

	/**
	 * @see jarmos.io.AModelManager#getClassLoader()
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
		// return new PathClassLoader("/data/data/jarmos.app/files/",
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
	 * @see jarmos.io.AModelManager#modelFileExists(java.lang.String)
	 */
	@Override
	public boolean modelFileExists(String filename) {
		try {
			// Faster that way
			getInStreamImpl(filename).close();
			// for (String f : c.getAssets().list(getModelDir())) {
			// if (f.equals(filename)) return true;
			// }
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	@Override
	public URI getModelURI() {
		return URI.create("file:///android_asset/" + getModelDir());
	}

	@Override
	protected String getLoadingMessage() {
		return "Reading asset models";
	}

}
