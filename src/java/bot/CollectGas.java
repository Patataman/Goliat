package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

public class CollectGas extends Action {

	public CollectGas(String name, GameHandler gh) {
		super(name, gh);
		// TODO Auto-generated constructor stub
	}

	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).aCurrarGas()) {
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
