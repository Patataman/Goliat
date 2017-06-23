package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

public class CheckTime extends Conditional {

	public CheckTime(String name, GameHandler gh) {
		super(name, gh);
	}

	public State execute() {
		try{
			if (((JohnDoe)this.handler).scouter != null) {
				return State.FAILURE;
			}
			if (((JohnDoe)this.handler).checkTime()) {
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
