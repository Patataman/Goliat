package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

public class CheckPositionUnits extends Conditional {

	public CheckPositionUnits(String name, GameHandler gh) {
		super(name, gh);
		// TODO Ap�ndice de constructor generado autom�ticamente
	}

	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).checkPositionUnits()) {
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
