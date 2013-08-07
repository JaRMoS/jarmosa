package jarmos.app.misc.rb;

import android.content.Context;
import android.widget.SeekBar;

/**
 * Utility class for a seekbar with index
 * 
 * This class has been taken from the original @ref rbappmit package and modified to fit into the current JaRMoS
 * framework
 * 
 * @author Daniel Wirtz @date 07.08.2013
 * 
 */
public class IndexedSeekBar extends SeekBar {

	private int mIndex;

	public IndexedSeekBar(Context c) {
		super(c);

		mIndex = 0;
	}

	public void setIndex(int index_in) {
		mIndex = index_in;
	}

	public int getIndex() {
		return mIndex;
	}
}
