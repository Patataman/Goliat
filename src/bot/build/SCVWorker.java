package bot.build;

import bwapi.Order;
import bwapi.Unit;
import bwapi.UnitType;

public class SCVWorker {
	
	public Unit scv;
	public UnitType building;
	public boolean isBase;

	public SCVWorker(Unit scv) {
		this.scv = scv;
		building = null;
		isBase = false;
	}
	
	public boolean isBusy() {
		if (scv.getOrder() == Order.PlaceBuilding ||
				scv.getOrder() != Order.ConstructingBuilding ||
				scv.getOrder() == Order.Move) {
			return true;
		}
		return false;
	}
	
	public boolean isGathering(){
		if (scv.getOrder() == Order.MoveToGas || scv.getOrder() == Order.MoveToMinerals ||
				scv.getOrder() == Order.ReturnGas || scv.getOrder() == Order.WaitForGas) {
			return true;
		}
		return false;
	}
	
	public boolean equals(SCVWorker other) {
		return this.scv.equals(other.scv);
	}
	
}
