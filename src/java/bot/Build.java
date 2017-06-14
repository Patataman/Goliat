package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

import jnibwapi.types.UnitType;

public class Build extends Action {
	
	UnitType edificio;
	
	public Build(String name, GameHandler gh, UnitType building) {
		super(name, gh);
		this.edificio = building;
	}

	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).buildUnit(edificio)) {
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
