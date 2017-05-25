package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import jnibwapi.types.UpgradeType;

public class CheckResearch extends Conditional {

	UpgradeType research;
	
	public CheckResearch(String name, GameHandler gh, UpgradeType research) {
		super(name, gh);
		this.research = research;
	}

	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).checkResearch(research)) {
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
