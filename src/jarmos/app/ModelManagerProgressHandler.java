package jarmos.app;

import jarmos.IMessageHandler;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * A progress notification handler implementation for android platforms
 * 
 * @author Daniel Wirtz @date 2013-08-07
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
