package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

public class CheckStateUnits extends Conditional {

	public CheckStateUnits(String name, GameHandler gh) {
		super(name, gh);
	}

	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).checkStateUnits()) {
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
