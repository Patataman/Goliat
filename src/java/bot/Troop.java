package bot;

import java.util.ArrayList;

import jnibwapi.Position;
import jnibwapi.Unit;

public class Troop {
	
	ArrayList<Unit> units;
	Position destination;
	byte state;
	/*State's list:
	 *	0: Doing nothing.
	 *	1: Attacking.
	 *	2: Defending.
	 *	3: Moving (?).
	 *	4: Waiting.
	*/

	public Troop (){
		units = new ArrayList<Unit>();
		state = 0;
		destination = null;
	}
	
	/**
	 * Check if troops are in destination.
	 * @return true if they are in destination, false otherwise
	 */
	public boolean isInPosition() {
		if (units.get(units.size()-1).getPosition().getApproxWDistance(destination) < 15) {
			state = 0;
			return true;
		}
		return false;
	}
	
}
