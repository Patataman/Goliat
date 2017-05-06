package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

import jnibwapi.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;

public class TrainUnit extends Action {
	
	UnitType unit, building;
	
	public TrainUnit(String name, GameHandler gh, UnitType unit, UnitType building) {
		super(name, gh);
		this.unit = unit;
		this.building = building;
	}

	@Override
	public State execute() {
		try{
			byte marines=0, fire_bat=0, medic = 0, vessel=0, tank = 0;
			for (Unit u : ((JohnDoe)this.handler).militaryUnits){
				if (u.getType() == UnitTypes.Terran_Marine) marines++;
				if (u.getType() == UnitTypes.Terran_Firebat) fire_bat++;
				if (u.getType() == UnitTypes.Terran_Medic) medic++;
				if (u.getType() == UnitTypes.Terran_Science_Vessel) vessel++;
				if (u.getType() == UnitTypes.Terran_Siege_Tank_Tank_Mode) tank++;
			}
			if (unit == UnitTypes.Terran_SCV){
				//Checks if it's possible train new SCVs
				if (((JohnDoe)this.handler).VCEs.get(
						((JohnDoe)this.handler).CCs.indexOf(
								((JohnDoe)this.handler).cc_select.getID())
													).size() >= ((JohnDoe)this.handler).max_vce) {
					return State.FAILURE; 					
				}
			} else if ( (!((JohnDoe)this.handler).expanded) && 
					((JohnDoe)this.handler).supplies > ((JohnDoe)this.handler).totalSupplies*0.7) {
				return State.FAILURE;
			} else {
				//If unit to train it's contained in the unitsToTrain, then check other stuff
				if (!((JohnDoe)this.handler).unitsToTrain.contains(this.unit)){
					return State.FAILURE;
				}
				//1 Medic for each 5 marine+fire_bat
				if (unit == UnitTypes.Terran_Medic && (marines+fire_bat < medic*4 || marines+fire_bat == 0)) {
					return State.FAILURE;
				}
				//1 firebat for each 3 marines
				if (unit == UnitTypes.Terran_Firebat && ((marines+fire_bat)%3 != 0 || marines+fire_bat == 0)) {
					return State.FAILURE;
				}
				//1 science vessel for each troop
				if (unit == UnitTypes.Terran_Science_Vessel && vessel >= ((JohnDoe)this.handler).assaultTroop.size()) {
					return State.FAILURE;
				}
				//1 siege tank for each 8 marines+fire_bat
				if (unit == UnitTypes.Terran_Siege_Tank_Tank_Mode && tank > (marines+fire_bat+medic)/8) {
					return State.FAILURE;
				}
				//Same as tanks
				if (unit == UnitTypes.Terran_Vulture && tank > (marines+fire_bat+medic)/8) {
					return State.FAILURE;
				}
			}
//			if (unit == UnitTypes.Terran_Science_Vessel && vessel>=((JohnDoe)this.handler).assaultTroop.size()) {
//				return State.FAILURE;
//			}
			if (((JohnDoe)this.handler).trainUnit(building, unit)) {
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
