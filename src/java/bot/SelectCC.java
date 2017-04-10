package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

public class SelectCC extends Action {

	public SelectCC(String name, GameHandler gh) {
		super(name, gh);
	}

	public State execute() {
		try{
			if (((JohnDoe)this.handler).selectCC()) {
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
