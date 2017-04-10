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
				//Se mira a ver si es posible entrenar algún VCE
				if (((JohnDoe)this.handler).VCEs.get(
						((JohnDoe)this.handler).CCs.indexOf(
								((JohnDoe)this.handler).cc_select.getID())
													).size() >= ((JohnDoe)this.handler).max_vce) {
					return State.FAILURE; 					
				}
			}
			if ( ( ((JohnDoe)this.handler).detector_first || ((JohnDoe)this.handler).CCs.size() <= 1) && 
					((JohnDoe)this.handler).militaryUnits.size() > 20) {
				return State.FAILURE;
			}
			//Por cada 5 marines+fire_bat debe entrenarse un médico
			if (unit == UnitTypes.Terran_Medic && ((marines+fire_bat+medic)%4 != 0 || marines+fire_bat == 0)) {
				return State.FAILURE;
			}
			//Por cada 3 marines 1 murcielago debe entrenarse
			if (unit == UnitTypes.Terran_Firebat && ((marines+fire_bat)%3 != 0 || marines+fire_bat == 0)) {
				return State.FAILURE;
			}
			//Se construirá una nave científica por tropa.
			if (unit == UnitTypes.Terran_Science_Vessel && vessel>=((JohnDoe)this.handler).assaultTroop.size()) {
				return State.FAILURE;
			}
			//Tankes por cada 10 soldados en general.
			if (unit == UnitTypes.Terran_Siege_Tank_Tank_Mode && tank > (marines+fire_bat+medic)/8) {
				return State.FAILURE;
			}
			if (unit == UnitTypes.Terran_Science_Vessel && vessel>=((JohnDoe)this.handler).assaultTroop.size()) {
				return State.FAILURE;
			}
			//Los goliats, mientras mas mejor
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
