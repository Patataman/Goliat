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
			byte marines=0, fire_bat=0, vessel=0;
			for (Unit u : ((JohnDoe)this.handler).militaryUnits){
				if (u.getType() == UnitTypes.Terran_Marine) marines++;
				if (u.getType() == UnitTypes.Terran_Firebat) fire_bat++;
				if (u.getType() == UnitTypes.Terran_Science_Vessel) vessel++;
			}
			if (unit == UnitTypes.Terran_SCV){
				//Se mira a ver si es posible entrenar algún VCE
				if (((JohnDoe)this.handler).VCEs.get(0).size() >= ((JohnDoe)this.handler).max_vce) {
					return State.FAILURE; 					
				}
			}
			//Por cada 5 marines+fire_bat debe entrenarse un médico
			if (unit == UnitTypes.Terran_Medic && ((marines+fire_bat)%5 != 0 || marines+fire_bat == 0)) {
				return State.FAILURE;
			}
			//Por cada 3 marines 1 murcielago debe entrenarse
			if (unit == UnitTypes.Terran_Firebat && ((marines+fire_bat)%3 != 0 || marines+fire_bat == 0)) {
				return State.FAILURE;
			}
			//Se construirá una nave científica para ver invisibles.
			if (unit == UnitTypes.Terran_Science_Vessel && vessel>=1) {
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
