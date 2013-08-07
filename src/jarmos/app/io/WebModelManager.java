package jarmos.app.io;

import jarmos.app.Const;

import java.io.IOException;
import java.net.URL;

import android.content.Context;
import android.util.Log;

/**
 * A model manager implementation that allows to load models from web locations
 * 
 * @author Daniel Wirtz @date 2013-08-07
 *
 */
public class WebModelManager extends jarmos.io.WebModelManager {

	private DexHelper dh;
	
	public WebModelManager(URL rooturl, Context c) {
		super(rooturl);
		dh = new DexHelper(c);
	}

	/**
	 * Overrides the standard implementation as different class loaders (dex) are required within android.
	 * @see jarmos.io.WebModelManager#getClassLoader()
	 */
	@Override
	public ClassLoader getClassLoader() {
		try {
			return dh.getDexClassLoader(getInStream(Const.DEX_CLASSES_JARFILE));
		} catch (IOException e) {
			Log.e("WebModelManager", "I/O Exception during input stream creation for file "
					+ Const.DEX_CLASSES_JARFILE + " in model " + getModelDir() + ", loading from web location "+this.getModelURI(), e);
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	protected String getLoadingMessage() {
		return "Reading remote model info";
	}

}
