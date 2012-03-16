/**
 * 
 */
package jarmos.app;

import java.io.File;
import java.lang.reflect.Method;

import kermor.java.dscomp.IInputFunctions;
import dalvik.system.DexClassLoader;

/**
 * @author Ernst
 *
 */
public class CompiledInputFunctions implements IInputFunctions {

	private Method numIn;
	private Class<?> cl;
	
	public CompiledInputFunctions(String filename) {
		File f = new File(filename);
		
		DexClassLoader dl = new DexClassLoader(f.getName(), f.getAbsolutePath(), null,
				ClassLoader.getSystemClassLoader());
	}
	
	/* (non-Javadoc)
	 * @see kermor.java.IInputFunctions#getNumFunctions()
	 */
	@Override
	public int getNumFunctions() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see kermor.java.IInputFunctions#evaluate(double, int)
	 */
	@Override
	public double[] evaluate(double t, int idx) {
		return null;
	}

}
