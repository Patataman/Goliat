package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

import bwapi.UnitType;

public class BuildAddon extends Action {

	UnitType addon;
	public BuildAddon(String name, GameHandler gh, UnitType addon) {
		super(name, gh);
		this.addon = addon;
	}

	@Override
	public State execute() {
		try {
			if (((JohnDoe)this.handler).buildAddon(addon)) {
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
