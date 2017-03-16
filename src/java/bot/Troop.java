package bot;

import java.util.ArrayList;

import jnibwapi.Position;
import jnibwapi.Unit;

public class Troop {
	
	ArrayList<Unit> units;
	Position destiny;
	byte state;
	/*State's list:
	 *	0: Doing nothing.
	 *	1: Attacking.
	 *	2: Defending.
	 *	3: Moving (?).
	*/

	public Troop (){
		units = new ArrayList<Unit>();
		state = 0;
		destiny = null;
	}
	
}
