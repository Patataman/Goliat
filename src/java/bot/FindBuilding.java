package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import jnibwapi.types.UnitType;

public class FindBuilding extends Conditional {

	UnitType building;
	
	public FindBuilding(String name, GameHandler gh, UnitType building) {
		super(name, gh);
		this.building = building;
	}

	@Override
	public State execute() {
		try {
			if (((JohnDoe)this.handler).findBuilding(building)) {
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
