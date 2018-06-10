package bot.resources;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

import bot.JohnDoe;

public class ClearGathering extends Action {

	public ClearGathering(String name, GameHandler gh) {
		super(name, gh);
		// TODO Auto-generated constructor stub
	}

	public State execute() {
		try{
			if (((JohnDoe)this.handler).clearGathering()) {
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
