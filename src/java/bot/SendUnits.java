package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

public class SendUnits extends Action {

	public SendUnits(String name, GameHandler gh) {
		super(name, gh);
	}

	@Override
	public State execute() {
		return State.ERROR;
		/*try{
			if (((JohnDoe)this.handler).sendUnits()) {
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
