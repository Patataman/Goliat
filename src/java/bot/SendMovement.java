package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

public class SendMovement extends Action {

	public SendMovement(String name, GameHandler gh) {
		super(name, gh);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).sendMovement()) {
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
