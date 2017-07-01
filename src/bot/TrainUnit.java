package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

import bwapi.Unit;
import bwapi.UnitType;

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
				if (u.getType() == UnitType.Terran_Marine) marines++;
				if (u.getType() == UnitType.Terran_Firebat) fire_bat++;
				if (u.getType() == UnitType.Terran_Medic) medic++;
				if (u.getType() == UnitType.Terran_Science_Vessel) vessel++;
				if (u.getType() == UnitType.Terran_Siege_Tank_Tank_Mode) tank++;
			}
			if (unit == UnitType.Terran_SCV){
				//Checks if it's possible train new SCVs
				if (((JohnDoe)this.handler).CCs.size() > 2 || 
					(((JohnDoe)this.handler).VCEs.get(
							((JohnDoe)this.handler).CCs.indexOf(
									((JohnDoe)this.handler).cc_select)
														).size() >= ((JohnDoe)this.handler).max_vce)) {
					return State.FAILURE; 					
				}
			}
			else {
				//If unit to train it's contained in the unitsToTrain, then check other stuff
				if (!((JohnDoe)this.handler).unitsToTrain.contains(this.unit)){
					return State.FAILURE;
				}
				//1 Medic for each 5 marine+fire_bat
				if (unit == UnitType.Terran_Medic && (marines+fire_bat < medic*4 || marines+fire_bat == 0)) {
					return State.FAILURE;
				}
				//1 firebat for each 3 marines
				if (unit == UnitType.Terran_Firebat && ((marines+fire_bat)%3 != 0 || marines+fire_bat == 0)) {
					return State.FAILURE;
				}
				//1 science vessel for each troop
				if (unit == UnitType.Terran_Science_Vessel && vessel >= ((JohnDoe)this.handler).assaultTroop.size()) {
					return State.FAILURE;
				}
				if (unit != UnitType.Terran_Science_Vessel && 
						((JohnDoe)this.handler).canTrain(UnitType.Terran_Science_Vessel) &&
						((JohnDoe)this.handler).militaryUnits.size() > 20) {
					return State.FAILURE;
				}
				//1 siege tank for each 8 marines+fire_bat
				if (unit == UnitType.Terran_Siege_Tank_Tank_Mode && tank > (marines+fire_bat+medic)/8) {
					return State.FAILURE;
				}
				//Same as tanks
				if (unit == UnitType.Terran_Vulture && tank > (marines+fire_bat+medic)/8) {
					return State.FAILURE;
				}
			}
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
