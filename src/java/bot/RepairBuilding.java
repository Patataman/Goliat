package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

public class RepairBuilding extends Action {

	public RepairBuilding(String name, GameHandler gh) {
		super(name, gh);
	}

	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).repair()) {
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
