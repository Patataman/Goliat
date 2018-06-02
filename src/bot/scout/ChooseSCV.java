package bot.scout;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

import bot.JohnDoe;

public class ChooseSCV extends Action {

	public ChooseSCV(String name, GameHandler gh) {
		super(name, gh);
	}

	public State execute() {
		try{
			if (((JohnDoe)this.handler).scouter != null && 
					((JohnDoe)this.handler).scouter.isMoving() &&
					(!((JohnDoe)this.handler).scouter.isGatheringMinerals() &&
					!((JohnDoe)this.handler).scouter.isGatheringGas())) {
				return State.FAILURE;
			}
			
			if (((JohnDoe)this.handler).chooseScouter()) {
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
