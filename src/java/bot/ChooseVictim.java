package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

public class ChooseVictim extends Action {

	public ChooseVictim(String name, GameHandler gh) {
		super(name, gh);
		// TODO Apéndice de constructor generado automáticamente
	}

	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).chooseVictim()) {
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
