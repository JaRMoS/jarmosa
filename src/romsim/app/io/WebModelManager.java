/**
 * 
 */
package romsim.app.io;

import java.io.IOException;
import java.net.URL;

import romsim.app.Const;
import android.content.Context;
import android.util.Log;

/**
 * @author dwirtz
 *
 */
public class WebModelManager extends rmcommon.io.WebModelManager {

	private DexHelper dh;
	
	public WebModelManager(URL rooturl, Context c) {
		super(rooturl);
		dh = new DexHelper(c);
	}

	/**
	 * Overrides the standard implementation as different class loaders (dex) are required within android.
	 * @see rmcommon.io.WebModelManager#getClassLoader()
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

}
