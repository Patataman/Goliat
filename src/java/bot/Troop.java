package bot;

import java.util.ArrayList;

import jnibwapi.Position;
import jnibwapi.Unit;

public class Troop {
	
	ArrayList<Unit> units;
	Position destination;
	byte distance;
	byte status;
	/*State's list:
	 *	0: Doing nothing.
	 *	1: Attacking.
	 *	2: Defending.
	 *	3: Regroup.
	 *	4: Retreat.
	 *	5: Waiting.
	 *	6: Exploring
	*/

	public Troop (){
		units = new ArrayList<Unit>(0);
		status = 0;
		destination = null;
		distance = 40;
	}
	
	/**
	 * Check if troops are in destination (or close to).
	 * @return true if they are in destination, false otherwise
	 */
	public boolean isInPosition() {
		if (destination == null) return true;
		int dist = 0;
		for (Unit u : units) {
			dist += u.getPosition().getApproxWDistance(destination);
			if (u.isIdle() && u.getPosition().getApproxWDistance(destination) > 30) {
				u.attack(destination, false);
			}
		}
		dist /= units.size();
		if (dist < 50) {
			status = 5;
			return true;
		}
		return false;
	}
	
	/**
	 * Check if first and last unit of "units" are too far.
	 * @return true if the first and last unit are too far.
	 */
	public boolean tooFar() {
		for (Unit u : units) {
			if (units.get(0).getPosition().getApproxWDistance(u.getPosition()) > distance) {
				distance = 20;
				return true;
			}
		}
		distance = 40;
		return false;
	}
	
}
