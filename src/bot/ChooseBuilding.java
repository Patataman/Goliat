package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

import jnibwapi.types.UnitType;

public class ChooseBuilding extends Action {
	
	UnitType unit;
	
	public ChooseBuilding(String name, GameHandler gh, UnitType unit) {
		super(name, gh);
		this.unit = unit;
	}

	//Indica si existe un edificio en el que se pueda entrenar la unidad
	public State execute() {
		try{
			if (((JohnDoe)this.handler).canTrain(unit)) {
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
