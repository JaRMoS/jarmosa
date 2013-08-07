package jarmos.app.misc.rb;

import android.content.Context;
import android.widget.Button;

/**
 * Utility class for a button that has an index attached
 * 
 * This class has been taken from the original @ref rbappmit package and modified to fit into the current JaRMoS
 * framework
 * 
 * @author Daniel Wirtz @date 07.08.2013
 *
 */
public class IndexedButton extends Button {

	private int index;

	public IndexedButton(Context c) {
		super(c);
		index = 0;
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}
}
