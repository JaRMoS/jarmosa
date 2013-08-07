package jarmos.app;

import jarmos.util.IProgressReporter;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;

/**
 * Helper class for progress dialogs on android platforms
 * 
 * @author Daniel Wirtz
 * 
 */
public class ProgressDialogWrapper extends Handler implements IProgressReporter {

	ProgressDialog pd;

	/**
	 * Creates a new ProgressDialogWrapper that wraps an Android ProgressDialog
	 * into the JaRMoS IProgressReporter interface.
	 * 
	 * @param pd
	 *            The ProgressDialog to wrap into an IProgressReporter
	 */
	public ProgressDialogWrapper(Activity activity) {
		pd = new ProgressDialog(activity);
		pd.setIndeterminate(false);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	}

	/**
	 * Handles the messages created in the IProgressReporter methods in the
	 * thread owning the ProgressDialog
	 */
	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case 1:
			pd.setMessage((String) msg.obj);
			break;
		case 2:
			pd.setProgress(msg.arg1);
			break;
		case 3:
			pd.setMax(msg.arg1);
			pd.setTitle((String) msg.obj);
			pd.show();
			break;
		case 4:
			pd.dismiss();
			break;
		case 5:
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jarmos.util.IProgressReporter#setMessage(java.lang.String)
	 */
	@Override
	public void setMessage(String msg) {
		sendMessage(obtainMessage(1, msg));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jarmos.util.IProgressReporter#progress(int)
	 */
	@Override
	public void progress(int value) {
		sendMessage(obtainMessage(2, value, 0));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jarmos.util.IProgressReporter#init(int)
	 */
	@Override
	public void init(String title, int total) {
		sendMessage(obtainMessage(3, total, 0, title));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jarmos.util.IProgressReporter#finish()
	 */
	@Override
	public void finish() {
		sendMessage(obtainMessage(4));
	}
}
