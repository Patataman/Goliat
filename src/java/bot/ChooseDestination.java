package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

public class ChooseDestination extends Action {

	public ChooseDestination(String name, GameHandler gh) {
		super(name, gh);
		// TODO Ap�ndice de constructor generado autom�ticamente
	}

	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).chooseDestination()) {
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
