package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;

public class FindPosition extends Conditional {

	UnitType building;
	
	public FindPosition(String name, GameHandler gh, UnitType building) {
		super(name, gh);
		this.building = building;
	}

	@Override
	public State execute() {
		try{
			//Check if the total supplies are not 200 or used supplies are less than the 70% of the total. 
			if (building == UnitTypes.Terran_Supply_Depot &&
					( ((JohnDoe)this.handler).supplies < ((JohnDoe)this.handler).totalSupplies*0.7 || 
					((JohnDoe)this.handler).totalSupplies >= 400) ){
				return State.FAILURE;
			}
			//Decorator for barracks. 2 per CC
			if (building == UnitTypes.Terran_Barracks && ( ((JohnDoe)this.handler).barracks > 4 ||
					((JohnDoe)this.handler).barracks >= ((JohnDoe)this.handler).CCs.size()+1 ||
					(((JohnDoe)this.handler).barracks != 0 && ((JohnDoe)this.handler).refinery == 0))) {
				return State.FAILURE;
			}
			//Decorator for refinery. Only after have built the barracks.
			if (building == UnitTypes.Terran_Refinery &&
					((JohnDoe)this.handler).barracks == 0 ) {
				return State.FAILURE;
			}
			//Decorator for academy. Only after have built the barracks and only 1.
			if (building == UnitTypes.Terran_Academy &&
					(((JohnDoe)this.handler).barracks == 0 || 
					((JohnDoe)this.handler).academy > 0)) {
				return State.FAILURE;
			}
			//Decorator for engineering bay. Only after have built the barracks and only 1
			if (building == UnitTypes.Terran_Engineering_Bay && (((JohnDoe)this.handler).bay > 0 
					|| ((JohnDoe)this.handler).barracks == 0)) {
				return State.FAILURE;
			}
			//Decorator for factory. Only after have built the barracks, 1 per CC and only after have at least 15 units.
			if (building == UnitTypes.Terran_Factory && (((JohnDoe)this.handler).barracks == 0 ||
					((JohnDoe)this.handler).factory >= ((JohnDoe)this.handler).CCs.size() ||
					((JohnDoe)this.handler).militaryUnits.size() < 15)) {
				return State.FAILURE;
			}
			//Decorator for expansions.
			//Only after have built the barracks.
			if (building == UnitTypes.Terran_Command_Center &&
					(((JohnDoe)this.handler).barracks == 0 ||
					((JohnDoe)this.handler).militaryUnits.size() < 15)) {
				return State.FAILURE;
			}
			//Decorator for bunkers
			if (building == UnitTypes.Terran_Bunker &&
					(((JohnDoe)this.handler).barracks == 0 ||
					((JohnDoe)this.handler).bunkers.size() >= ((JohnDoe)this.handler).CCs.size() )) {
				return State.FAILURE;
			}
			
			//Decorator for missile turrets
			if (building == UnitTypes.Terran_Missile_Turret && ((JohnDoe)this.handler).bay == 0) {
				return State.FAILURE;
			}
			
			//Decorator for science facility. Only 1.
			if (building == UnitTypes.Terran_Science_Facility &&
					((JohnDoe)this.handler).lab_cient > 0) {
				return State.FAILURE;
			}
			//Decorator for starport. Only 1.
			if (building == UnitTypes.Terran_Starport &&
					( ((JohnDoe)this.handler).CCs.size() <= 1 ||
					((JohnDoe)this.handler).starport >= 2)) {
				return State.FAILURE;
			}
			//Decorator for armory. Only 1.
			if (building == UnitTypes.Terran_Armory &&
					( ((JohnDoe)this.handler).CCs.size() <= 1 ||
					(((JohnDoe)this.handler).factory == 0 ||
					((JohnDoe)this.handler).armory > 0))) {
				return State.FAILURE;
			}
			
			if (((JohnDoe)this.handler).findPosition(building)) {
				return State.SUCCESS;
			} else {
				return State.FAILURE;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return State.ERROR;
		}
	}

}
