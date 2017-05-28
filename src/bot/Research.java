package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

import jnibwapi.types.UnitType;
import jnibwapi.types.UpgradeType;

public class Research extends Action {
	
	UpgradeType research;
	UnitType building;

	public Research(String name, GameHandler gh, UnitType building, UpgradeType research) {
		super(name, gh);
		this.research = research;
		this.building = building;
	}

	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).doResearch(building, research)) {
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
