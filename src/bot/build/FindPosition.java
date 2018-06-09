package bot.build;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import bot.JohnDoe;
import bwapi.UnitType;

public class FindPosition extends Conditional {

	UnitType building;
	
	public FindPosition(String name, GameHandler gh, UnitType building) {
		super(name, gh);
		this.building = building;
	}

	@Override
	public State execute() {
		try{
			//Check if the total supplies are not 200.
//			if (building == UnitType.Terran_Supply_Depot) {
//				System.out.println("Supplie "+ ((JohnDoe)this.handler).supplies );
//				System.out.println("Total "+ ((JohnDoe)this.handler).totalSupplies );
//			}
			if (building == UnitType.Terran_Supply_Depot && 
				( ((JohnDoe)this.handler).barracks == 0 || 
				((JohnDoe)this.handler).supplies < ((JohnDoe)this.handler).totalSupplies-6 || 
					((JohnDoe)this.handler).totalSupplies >= 400) ){
				return State.FAILURE;
			}
			//Decorator for barracks.
			//2 per CC, Only 1 before refinery and academy
			if (building == UnitType.Terran_Barracks && 
					( ((JohnDoe)this.handler).barracks >= ((JohnDoe)this.handler).CCs.size()+2 ||
					(((JohnDoe)this.handler).barracks != 0 && ((JohnDoe)this.handler).refinery == 0) ||
					(((JohnDoe)this.handler).barracks > 1 && ((JohnDoe)this.handler).academy == 0) ||
					((JohnDoe)this.handler).barracks == 5) ) {
				return State.FAILURE;
			}
			//Decorator for refinery. Only after have built the barracks.
			if (building == UnitType.Terran_Refinery &&
					((JohnDoe)this.handler).barracks == 0 ) {
				return State.FAILURE;
			}
			//Decorator for academy. Only after have built the barracks and only 1.
			if (building == UnitType.Terran_Academy &&
					( ((JohnDoe)this.handler).barracks == 0 || 
					((JohnDoe)this.handler).academy > 0) ) {
				return State.FAILURE;
			}
			//Decorator for factory. Only after have built the barracks, 1 per CC.
			if (building == UnitType.Terran_Factory && 
					( ((JohnDoe)this.handler).academy == 0 ||
					((JohnDoe)this.handler).factory >= ((JohnDoe)this.handler).CCs.size()+1 || 
					((JohnDoe)this.handler).factory == 3 ) ) {
				return State.FAILURE;
			}
			//Decorator for engineering bay. Only after have built the barracks and academy
			if (building == UnitType.Terran_Engineering_Bay &&
					( ((JohnDoe)this.handler).barracks < 2 || 
					  ((JohnDoe)this.handler).bay > 0 || 
					  ((JohnDoe)this.handler).academy == 0 ) ) {
				return State.FAILURE;
			}
			//Decorator for expansions.
			//Only after have built the barracks.
			//Only when we have less than 2 minerals fields been gathered
			if ( building == UnitType.Terran_Command_Center &&
					( ((JohnDoe)this.handler).barracks == 0 ||
					((JohnDoe)this.handler).militaryUnits.size() < 15 ||
					((JohnDoe)this.handler).mineralNodes.size() > 2 ) ) {
				return State.FAILURE;
			}
			//Decorator for bunkers
			if ( building == UnitType.Terran_Bunker &&
					( ((JohnDoe)this.handler).barracks == 0 ||
					((JohnDoe)this.handler).bunkers.size() >= ((JohnDoe)this.handler).CCs.size() ) ) {
				return State.FAILURE;
			}
			
			//Decorator for missile turrets
			if (building == UnitType.Terran_Missile_Turret && ((JohnDoe)this.handler).bay == 0) {
				return State.FAILURE;
			}
			
			//Decorator for science facility. Only 1.
			if (building == UnitType.Terran_Science_Facility &&
					((JohnDoe)this.handler).labCient > 0) {
				return State.FAILURE;
			}
			//Decorator for starport. Only 1.
			if (building == UnitType.Terran_Starport &&
					(((JohnDoe)this.handler).starport == 1 && (((JohnDoe)this.handler).labCient == 0 ||
					((JohnDoe)this.handler).starport >= 2) ||
					!((JohnDoe)this.handler).expanded)) {
				return State.FAILURE;
			}
			if (((JohnDoe)this.handler).detectorFirst && 
					((((JohnDoe)this.handler).mineral < 500 && ((JohnDoe)this.handler).vespinGas < 200 )||
					(((JohnDoe)this.handler).barracks != 0 &&
					((JohnDoe)this.handler).factory != 0 &&
					(building != UnitType.Terran_Starport &&
					   building != UnitType.Terran_Science_Facility &&
					   building != UnitType.Terran_Command_Center &&
					   building != UnitType.Terran_Refinery)))) {
				return State.FAILURE;
			}
			//Decorator for armory. Only 1.
			if (building == UnitType.Terran_Armory &&
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
