/**
 * 
 */
package jarmos.app;

import jarmos.IMessageHandler;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * @author Ernst
 *
 */
public class ModelManagerProgressHandler extends Handler implements IMessageHandler {

	@Override
	public void sendMessage(String msg) {
		Message m = obtainMessage();
		Bundle b = new Bundle();
		b.putString("file", msg);
		m.setData(b);
		sendMessage(m);
	}

}
