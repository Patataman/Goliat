package bot.resources;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import bot.JohnDoe;
import bwapi.UnitType;

public class CheckResources extends Conditional {
	
	int gas, mineral;
	
	public CheckResources(String name, GameHandler gh, UnitType tipo) {
		super(name, gh);
		this.gas = tipo.gasPrice();
		this.mineral = tipo.mineralPrice();
	}

	@Override
	public State execute() {
		try{
			if (((JohnDoe)this.handler).checkResources(mineral, gas)) {
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
