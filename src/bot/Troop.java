package bot;

import java.util.ArrayList;

import bwapi.Game;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class Troop {
	
	ArrayList<Unit> units;
	TilePosition destination;
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
	//To avoid a troop only form by vessels
	boolean hasDetector;
	//To avoid blockage of movement
	int lastChange;

	public Troop (){
		units = new ArrayList<Unit>(0);
		status = 0;
		lastChange = 0;
		destination = null;
		hasDetector = false;
	}
	
	/**
	 * Check if troops are in destination (or close to).
	 * @return true if they are in destination, false otherwise
	 */
	public boolean isInPosition() {
		if (destination == null || units.size() > 20) return true;
		int dist = 0;
		for (Unit u : units) {
			dist += u.getTilePosition().getDistance(destination);
			if (u.isIdle() && u.getTilePosition().getDistance(destination) > 6) {
				u.attack(destination.toPosition(), false);
			}
		}
		dist /= units.size();
		if (dist < 6) {
			status = 5;
			return true;
		}
		return false;
	}
	
	/**
	 * Check if unit average distant is > 50.
	 * @return true if > 50, false if not.
	 */
	public boolean tooFar() {
		if (units.size() > 20 || units.isEmpty()) return false;
		int dist = 0;
		TilePosition dest = units.get((int) units.size()/2).getTilePosition();
		for (Unit u : units) {
			dist += u.getTilePosition().getDistance(dest);
		}
		dist /= units.size();
		if (dist > 3) {
			return true;
		}
		return false;
	}

	public boolean surrounded(Game connector) {
		if (units.size() > 0) {
			int allies = 0, enemies = 0;
			for (Unit u : connector.getUnitsInRadius(units.get(0).getPosition(), 300)) {
				if (units.get(0).getPlayer().getUnits().contains(u)) allies++;
				else if (u.getPlayer().getID() != units.get(0).getPlayer().getID() && 
						!u.getType().isNeutral() && (!u.getType().isBuilding() || 
													u.getType() == UnitType.Protoss_Photon_Cannon ||
													u.getType() == UnitType.Zerg_Spore_Colony ||
													u.getType() == UnitType.Zerg_Sunken_Colony))
					enemies++;
			}
			
			if (allies > enemies) return false;
			else return true;
		}
		return false;
	}
	
}
