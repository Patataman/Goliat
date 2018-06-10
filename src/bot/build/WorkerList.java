package bot.build;

import java.util.ArrayList;

import bwapi.Unit;

public class WorkerList {
	
	public ArrayList<SCVWorker> workers;
	
	public WorkerList() {
		workers = new ArrayList<SCVWorker>(0);
	}
	
	/**
	 * Check if the SCV is in the list
	 * @param u: SCV BWAPI unit
	 * @return true if it's, false otherwise
	 */
	public boolean contains(Unit u) {
		for (SCVWorker w : workers) {
			if (w.scv.equals(u)) return true;
		}
		return false;
	}
	
	/**
	 * Add new worker to the list
	 * @param u: SCV BWAPI unit
	 */
	public void add(Unit u) {
		workers.add(new SCVWorker(u));
	}
	
	/**
	 * Add new worker to the list
	 * @param index: index, obviously
	 * @param u: SCV BWAPI unit
	 */
	public void add(int index, Unit u) {
		workers.add(index, new SCVWorker(u));
	}
	
	/**
	 * Remove a SCV from the list of workers
	 * @param u: SCV BWAPI unit
	 */
	public void remove(Unit u) {
		SCVWorker remove = null;
		for (SCVWorker w : workers) {
			if (w.scv.equals(u)) {
				remove = w;
				break;
			}
		}
		workers.remove(remove);
	}
	
	/**
	 * Check if the SCV is in the list
	 * @param u: SCVWorker
	 * @return true if it's, false otherwise
	 */
	public boolean contains(SCVWorker u) {
		return workers.contains(u);
	}
	
	/**
	 * Add new worker to the list
	 * @param u: SCVWorker
	 */
	public void add(SCVWorker u) {
		workers.add(u);
	}
	
	/**
	 * Add new worker to the list
	 * @param index: index, obviously
	 * @param u: SCVWorker
	 */
	public void add(int index, SCVWorker u) {
		workers.add(index, u);
	}
	
	/**
	 * Remove a SCV from the list of workers
	 * @param u: SCVWorker
	 */
	public void remove(SCVWorker u) {
		workers.remove(u);
	}
	
	/**
	 * @return ArrayList size() result
	 */
	public int size() {
		return this.workers.size();
	}
	
	/**
	 * @param index
	 * @return ArrayList get(int) result
	 */
	public SCVWorker get(int index) {
		return workers.get(index);
	}
}
