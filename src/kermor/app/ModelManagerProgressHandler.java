/**
 * 
 */
package kermor.app;

import kermor.java.IProgressHandler;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * @author Ernst
 *
 */
public class ModelManagerProgressHandler extends Handler implements IProgressHandler {

	@Override
	public void progress(String msg, int perc) {
		Message m = obtainMessage();
		Bundle b = new Bundle();
		b.putString("file", msg);
		b.putInt("perc", perc);
		m.setData(b);
		sendMessage(m);
	}

}
