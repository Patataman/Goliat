package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

public class MoveTo extends Action {
	
	public MoveTo(String name, GameHandler gh) {
		super(name, gh);
	}

	//Indica si existe un edificio en el que se pueda entrenar la unidad
	public State execute() {
		try{
			if (((JohnDoe)this.handler).moveTo()) {
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
