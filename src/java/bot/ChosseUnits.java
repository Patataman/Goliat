package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

public class ChosseUnits extends Action {

	public ChosseUnits(String name, GameHandler gh) {
		super(name, gh);
	}

	@Override
	public State execute() {
		return State.ERROR;
		/*try{
			if (((JohnDoe)this.handler).chosseUnits()) {
				return State.SUCCESS;
			} else {
				return State.FAILURE;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return State.ERROR;
		}*/
	}

}
