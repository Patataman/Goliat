package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import jnibwapi.types.UnitType;

public class CheckResources extends Conditional {
	
	int gas, mineral;
	
	public CheckResources(String name, GameHandler gh, UnitType tipo) {
		super(name, gh);
		this.gas = tipo.getGasPrice();
		this.mineral = tipo.getMineralPrice();
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
