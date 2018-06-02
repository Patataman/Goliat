package bot.scout;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import bot.JohnDoe;

public class CheckTime extends Conditional {

	public CheckTime(String name, GameHandler gh) {
		super(name, gh);
	}

	public State execute() {
		try{
			if (((JohnDoe)this.handler).scouter != null ||
					((JohnDoe)this.handler).barracks == 0) {
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
