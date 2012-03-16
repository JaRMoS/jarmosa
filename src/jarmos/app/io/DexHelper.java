/**
 * Created on Aug 24, 2011 in Project ROMSim
 * Location: jarmos.app.io.DexHelper.java
 */
package jarmos.app.io;

import jarmos.app.Const;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.util.Log;
import dalvik.system.DexClassLoader;

/**
 * @author Daniel Wirtz
 * @date Aug 24, 2011
 * 
 */
public class DexHelper {

	private Context c;

	private String lastFile = null;

	/**
	 * @param c
	 */
	public DexHelper(Context c) {
		this.c = c;
	}

	/**
	 * @param in An input stream pointing to a dex jarfile.
	 * @return A class loader that can load clases from the dex-jar file provided via the input stream
	 */
	public ClassLoader getDexClassLoader(InputStream in) {
		// Create a local copy for dex optimization needs write access
		String file = "tmpdex" + System.currentTimeMillis() + ".jar";
		Log.d("DexHelper", "Using temporary dex-jarfile " + file);
		try {
			if (lastFile != null) c.deleteFile(lastFile);

			String[] files = c.fileList();
			System.out.println(files);
			FileOutputStream f = c.openFileOutput(file, Context.MODE_WORLD_READABLE);

			byte[] buffer = new byte[1024];
			int len1 = 0;
			while ((len1 = in.read(buffer)) > 0) {
				f.write(buffer, 0, len1);
			}
			in.close();
			Log.d("DexHelper", "Finished preparing local copy of jar-file input stream"
					+ file);
		} catch (IOException e) {
			Log.e("DexHelper", "I/O exception while preparing local copy of jar-file input stream", e);
		}
		lastFile = file;
		return new DexClassLoader(Const.APP_DATA_DIRECTORY + "/" + file, Const.APP_DATA_DIRECTORY, null, c.getClassLoader());
	}

	/**
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		if (lastFile != null) c.deleteFile(lastFile);
		super.finalize();
	}
	
}
