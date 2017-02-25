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
			byte soldados=0, murcielagos=0, nave_cientifica=0;
			for (Unit u : ((JohnDoe)this.handler).unidadesMilitares){
				if (u.getType() == UnitTypes.Terran_Marine) soldados++;
				if (u.getType() == UnitTypes.Terran_Firebat) murcielagos++;
				if (u.getType() == UnitTypes.Terran_Science_Vessel) nave_cientifica++;
			}
			if (unit == UnitTypes.Terran_SCV){
				//Se mira a ver si es posible entrenar algún VCE
				if (((JohnDoe)this.handler).VCEs.get(0).size() >= ((JohnDoe)this.handler).max_vce) {
					return State.FAILURE; 					
				}
			}
			//Por cada 5 soldados+murcielagos debe entrenarse un médico
			if (unit == UnitTypes.Terran_Medic && ((soldados+murcielagos)%5 != 0 || soldados+murcielagos == 0)) {
				return State.FAILURE;
			}
			//Por cada 3 soldados 1 murcielago debe entrenarse
			if (unit == UnitTypes.Terran_Firebat && ((soldados+murcielagos)%3 != 0 || soldados+murcielagos == 0)) {
				return State.FAILURE;
			}
			//Se construirá una nave científica para ver invisibles.
			if (unit == UnitTypes.Terran_Science_Vessel && nave_cientifica>=1) {
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
