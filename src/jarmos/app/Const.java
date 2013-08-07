/**
 * Created on Aug 24, 2011 in Project JaRMoSA
 * Location: jarmos.app.Const.java
 */
package jarmos.app;

import jarmos.app.io.AssetModelManager;
import jarmos.app.io.SDModelManager;
import jarmos.app.io.WebModelManager;
import jarmos.io.AModelManager;
import jarmos.io.AModelManager.ModelManagerException;
import jarmos.io.CachingModelManager;

import java.net.URL;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Class that contains miscellaneous JaRMoS specific constants and static functions
 * 
 * @author Daniel Wirtz
 * @date Aug 24, 2011
 * 
 */
public class Const {

	/**
	 * The file name within a model directory that contains any runtime-loadable
	 * classes.
	 * 
	 * Used e.g. for the AffineFunctions class of JRB models or the input
	 * functions from JKerMor models.
	 */
	public static final String DEX_CLASSES_JARFILE = "dexclasses.jar";

	/**
	 * The directory where the application may write data to and read from.
	 * 
	 * At the moment, this is /data/data/jarmos.app/files.
	 * 
	 * This value gets set first in the onCreate method of the MainActivity
	 * class.
	 */
	public static String APP_DATA_DIRECTORY = null;
	
	/**
	 * The string describing the class name which denotes the corret
	 * AModelManager subclass to be instantiated upon creation.
	 */
	public static final String EXTRA_MODELMANAGER_CLASSNAME = "amodelmanager_classname";
	
	/**
	 * The string extra in intents to tell which model directory is currently used.
	 */
	public static final String EXTRA_MODELMANAGER_MODELDIR = "amodelmanager_modeldir";
	
	/**
	 * The filename for the application preferences.
	 */
	public static final String PREFERENCES_FILENAME = "jarmos.app.prefs";
	
	/**
	 * The name for the preference storing information about whether models should be cached when loaded from a web location.
	 */
	public static final String PREF_MODELCACHING = "modelCaching";
	
	/**
	 * The name for the preference storing information about whether existing model data is overwritten when caching remote models.
	 */
	public static final String PREF_MODELCACHING_OVERWRITE = "modelCachingOverwrite";
	
	/**
	 * Returns a model manager instance for the current intent.
	 * 
	 * The intent has to contain the class names of the ModelManagers in the string extra CLASSNAME_EXTRA,
	 * any extras like a URL for the WebModelManager have to be present, too.
	 * 
	 * @param c The current context - only needed for asset access (can be getApplicationContext() at calling point)
	 * @param i The current Activities/Services Intent given by getIntent()
	 * @return A model manager suitable for the given intent
	 * @throws ModelManagerException Gets thrown when setting a model directory fails.
	 */
	public static AModelManager getModelManager(Context c, Intent i) throws ModelManagerException {
		AModelManager res = null;
		String classname = i.getStringExtra(EXTRA_MODELMANAGER_CLASSNAME);
		if ("AssetModelManager".equals(classname)) {
			res = new AssetModelManager(c);
		} else if ("SDModelManager".equals(classname)) {
			res = new SDModelManager(c);
		} else if ("WebModelManager".equals(classname)) {
			res = new WebModelManager((URL)i.getSerializableExtra("URL"),c);
			SharedPreferences p = c.getSharedPreferences(PREFERENCES_FILENAME, 0);
			if (p.getBoolean(PREF_MODELCACHING, false)) {
				res = new CachingModelManager(res, new SDModelManager(c), p.getBoolean(PREF_MODELCACHING_OVERWRITE, false));
			}
		} else {
			if (classname == null) {
				throw new RuntimeException("ModelManagerService: Intent string extra '"
						+ EXTRA_MODELMANAGER_CLASSNAME + "' is null");
			} else
				throw new RuntimeException("ModelManagerService: Class "
						+ classname + " not known as AModelManager");
		}
		String md = i.getStringExtra(EXTRA_MODELMANAGER_MODELDIR);
		if (md != null) {
			res.useModel(md);
		}
		return res;
	}
	
//	public static boolean showQuestion(final Activity a, String text) {
//		
//		class Res {
//			public boolean result = false;
//		}
//		final Res r = new Res();
//		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
//		    @Override
//		    public void onClick(DialogInterface dialog, int which) {
//		        switch (which){
//		        case DialogInterface.BUTTON_POSITIVE:
//		            r.result = true;
//		            break;
//		        case DialogInterface.BUTTON_NEGATIVE:
//		            r.result = false;
//		            break;
//		        }
//		        dialog.dismiss();
//		        a.notify();
//		    }
//		};
//
//		new AlertDialog.Builder(a).setMessage(text).setPositiveButton("Yes", dialogClickListener)
//		    .setNegativeButton("No", dialogClickListener).show();
//		try {
//			a.wait();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return r.result;
//	}

}
