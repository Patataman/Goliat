package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

public class FreeBuilder extends Conditional {

	public FreeBuilder(String name, GameHandler gh) {
		super(name, gh);
	}

	public State execute() {
		try{
			if (((JohnDoe)this.handler).getMasterBuilder()) {
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
