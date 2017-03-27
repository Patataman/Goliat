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
	 *	3: Moving.
	 *	4: Waiting.
	 *	5: Exploring
	*/

	public Troop (){
		units = new ArrayList<Unit>(0);
		state = 0;
		destination = null;
	}
	
	/**
	 * Check if troops are in destination.
	 * @return true if they are in destination, false otherwise
	 */
	public boolean isInPosition() {
		if (destination == null) return true;
		if (units.get(units.size()-1).getPosition().getApproxWDistance(destination) < 20) {
			state = 4;
			return true;
		}
		return false;
	}
	
	/**
	 * Check if first and last troop of "units" are too far.
	 * @return true if the first and last troop are too far.
	 */
	public boolean tooFar() {
		System.out.println("Distancia: "+units.get(units.size()-1).getPosition().getApproxWDistance(units.get(0).getPosition()));
		if (units.get(units.size()-1).getPosition().getApproxWDistance(units.get(0).getPosition()) > 30 &&
				state != 4) {
			//if too far, group units
			for(Unit soldadito : units){
				soldadito.attack(units.get(0).getPosition().makeValid(), false);
			}
			state = 4;
			return true;
		}
		return false;
	}
	
}
