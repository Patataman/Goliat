package bot;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.iaie.btree.util.GameHandler;

import bot.attack.Troop;
import bot.others.NewInfluence;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;
import bwapi.Color;
import bwapi.Game;
import bwapi.Order;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.Race;
import bwapi.TilePosition;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwapi.Utils;

public class JohnDoe extends GameHandler {
	
	//Inner control lists
	public List<Unit> CCs;								//List for counting number of CCs
	public List<Unit> remainingUnits; 					//List to know which units are being trained.
	public List<Unit> militaryUnits;					//List to know all military unit trained (alive).
	public List<Unit> boredSoldiers;					//List of unit which are defending base.
	public List<Unit> intruders;						//List with enemies inside the perimeter
	public List<Unit> militia;							//List of units chasing the intruders.
	public List<Unit> damageBuildings;					//List to know which buildings are being attacked and being able to fix it later.
	public List<Unit> workers;							//List to control which SCVs are assigned to building.
	public List<Unit> finishedBuildings; 				//List to know all finished (and alive) buildings.
	public List<Unit> bunkers;							//List to know the bunkers I have
	public List<Unit> repairer;						    //List of SCVs reparing buildings
	public List<ArrayList<Unit>> VCEs;					//List for counting number of SCVs from each CC.
	public List<ArrayList<Unit>> mineralNodes;	 		//List for control the number of mineral nodes next to the CC
	public List<ArrayList<Unit>> workersMineral; 		//List for counting number of SVCs gathering minerals.
	public List<ArrayList<Unit>> workersVespin; 		//List for counting number of SCVs gathering vespin gas.
	public List<Troop> assaultTroop;					//List of groups of troops created.
	public Troop attackGroup;							//Group of attacking units currently selected.
	public Troop defendGroup;							//Group of defending units.
	public List<UnitType> remainingBuildings;			//List to know which buildings are being builded.
	public List<UpgradeType> researching;				//List to know which researches are being researched.
	public Unit current_worker;						    //Variable to know which SCV is currently selected.
	public Unit addonBuilding;							//Variable to get (without searching again) the building which addon we're going to build
	
	public List<UnitType> unitsToTrain;				    //This list contains all military units that can be trained 
	
	public int supplies, totalSupplies, mineral, vespinGas;
	public byte barracks, refinery, factory, 
		academy, armory, bay, maxVce, 
		labCient, starport, numberChokePoints,
		limit, vessels;
	
	public boolean detectorFirst = false;
	public boolean expanded = false;
	public Race enemyRace;
	public UnitType buildingType;
	
	//Variables for controlling initial cc and expansions.
	public Unit cc, ccSelect, scouter;
	
	//Position where the last building is going to be built.
	public TilePosition posBuild;
	//Position to attack.
	public TilePosition objective;
	
	private int[][] map;
	
	private NewInfluence influenceMap;

	public JohnDoe(Game bwapi, Player self) {
		super(bwapi, self);

		cc 						= null;
		ccSelect 				= null;
		scouter					= null;
		addonBuilding			= null;
		buildingType			= null;
		enemyRace 				= Race.Unknown;
		workers 				= new ArrayList<Unit>(2);
		militaryUnits			= new ArrayList<Unit>(0);
		boredSoldiers			= new ArrayList<Unit>(0);
		finishedBuildings 		= new ArrayList<Unit>(0);
		damageBuildings			= new ArrayList<Unit>(0);
		bunkers 				= new ArrayList<Unit>(0);
		CCs 					= new ArrayList<Unit>(0);
		intruders				= new ArrayList<Unit>(0);
		militia					= new ArrayList<Unit>(0);
		remainingUnits 			= new ArrayList<Unit>(0);
		repairer 				= new ArrayList<Unit>(0);
		VCEs 					= new ArrayList<ArrayList<Unit>>(0);
		mineralNodes			= new ArrayList<ArrayList<Unit>>(0);
		workersMineral 			= new ArrayList<ArrayList<Unit>>(0);
		workersVespin 			= new ArrayList<ArrayList<Unit>>(0);
		researching				= new ArrayList<UpgradeType>(0);
		remainingBuildings 		= new ArrayList<UnitType>(0);
		unitsToTrain			= new ArrayList<UnitType>(0);
		assaultTroop			= new ArrayList<Troop>(0);
		attackGroup				= new Troop();
		defendGroup				= new Troop();
		barracks = refinery = factory = 
			academy = armory = bay = labCient = 
			starport = vessels = 0;
		mineral 				= this.self.minerals();
		vespinGas 				= this.self.gas();
		numberChokePoints 		= analyzeCP();
		limit 					= 4;
		influenceMap 			= new NewInfluence(this.connector.mapHeight(), this.connector.mapWidth());
	}
	
	private byte analyzeCP() {
		byte number_cp = (byte) BWTA.getRegion(BWTA.getStartLocation(this.self).getTilePosition()).getChokepoints().size();
		return number_cp;
	}

	/**
	 * Adds the corresponding lists to the new CC
	 * @param cc_pos
	 */
	public void addCC(Unit cc) {
		VCEs.add(new ArrayList<Unit>());
		mineralNodes.add((ArrayList<Unit>) BWTA.getNearestBaseLocation(cc.getTilePosition()).getMinerals());
		workersMineral.add(new ArrayList<Unit>());
		workersVespin.add(new ArrayList<Unit>());
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean selectCC() {
		if (CCs.size() > 0) {
			//Default, ccSelect is randomly select
			ccSelect = CCs.get((int)Math.random()*CCs.size());
			maxVce = (byte)(mineralNodes.get(CCs.indexOf(ccSelect)).size()*2);
			for (Unit cc : CCs) {
				if (cc.isCompleted() && 
					mineralNodes.get(CCs.indexOf(cc)).size() > 0 &&
					(VCEs.get(CCs.indexOf(cc)).size() < maxVce || 
					workersVespin.get(CCs.indexOf(cc)).size() < 3)) {
					ccSelect = cc;				
				}
			}			
		}
		return false;
	}
	
	/**
	 * Get a SCV which is free.
	 * A SCV is free when he isn't gathering mineral/vespin or idle
	 * @return
	 */
	public boolean getWorker() {
		for (ArrayList<Unit> vces_cc : VCEs) {
			for (Unit vce : vces_cc) {
				if ((!workersMineral.get(VCEs.indexOf(vces_cc)).contains(vce) &&
						 !workersVespin.get(VCEs.indexOf(vces_cc)).contains(vce)) &&
						 vce.isIdle() &&
						 vce.isCompleted() &&
						 CCs.size() > 0 &&
						 !vce.equals(scouter) &&
						 !militia.contains(vce)) {
						if (workers.contains(vce)) workers.remove(vce);
						current_worker = vce;
						return true;
				}
			}
			
		}
		return false;
	}
	
	/**
	 * Gets a SCV to build. Only gets a SCV who is gathering minerals or idle. Always chooses a SCV from the original CC.
	 * @return
	 */
	public boolean getMasterBuilder() {
		if (workers.size() < 2) {
			for (Unit vce : workers){
				//If SCV in workers is idle, return true
				if (vce.getOrder() != Order.ConstructingBuilding ||
						vce.getOrder() == Order.PlaceBuilding){
					workers.remove(vce);
					workers.add(0, vce);
					return true;
				}
			}
			//Checks if the worker is in mineral lists, return true and remove him from the list
			if (workersMineral.size() > 0 && ccSelect != null) {
				for (Unit vce : workersMineral.get(CCs.indexOf(ccSelect))) {
					//If it isn't in the "workers" list...
					if (!workers.contains(vce) && scouter != vce) {
						//Adds the SCV to the "workers" list
						workersMineral.get(CCs.indexOf(ccSelect)).remove(vce);
						workers.add(0, vce);
						return true;					
					}
				}				
			}

			//There isn't any free SCV
			return false;
		}

		//If "workers" is full, check if there's any free SCV
		else {
			for (Unit vce : workers) {
				//If the vce is idle, move him to the beginning of the list
				if (vce.getOrder() == Order.PlaceBuilding || 
						vce.getOrder() != Order.ConstructingBuilding){
					workers.remove(vce);
					workers.add(0, vce);
					return true;
				}
			}
			//"workers" is full and the 2 SCV are busy.
			return false;
		}
	}
	
	/**
	 * Checks if it's time to scout the enemy
	 * @return
	 */
	public boolean checkTime() {
		if ((enemyRace == Race.Unknown) ||
				(this.connector.elapsedTime() > 10 &&
				this.connector.elapsedTime()/60 % 10 == 0))
			return true;
		else
			return false;
	}

	/**
	 * select a SCV to scout
	 * @return
	 */
	public boolean chooseScouter() {
		if (scouter == null && ccSelect != null) {
			if (workersMineral.get(CCs.indexOf(ccSelect)) != null &&
					!workersMineral.get(CCs.indexOf(ccSelect)).isEmpty()) {
				for (Unit u : workersMineral.get(CCs.indexOf(ccSelect))) {
					if (!workers.contains(u) && u.isGatheringMinerals() && u.isCompleted()) {
						scouter = u;
						workersMineral.get(CCs.indexOf(ccSelect)).remove(u);
						return true;
					}
				}
			}			
		}
		return false;
	}

	/**
	 * Send to scout
	 * @return
	 */
	public boolean sendToScout() {
		if (scouter == null) return false;
		scouter.move(ccSelect.getPosition(),false);
		if (this.connector.elapsedTime() > 10) {			
			for (BaseLocation bl : BWTA.getBaseLocations()) {
				if (bl.isStartLocation() && !bl.getTilePosition().equals(self.getStartLocation())) {
					scouter.move(bl.getPosition().makeValid(), true);
				}
			}
			if (this.connector.elapsedTime() > 500){ 
				for (BaseLocation bl : BWTA.getBaseLocations()) {
					if (!bl.isStartLocation() && BWTA.isConnected(bl.getTilePosition(), scouter.getTilePosition())) {
						scouter.move(bl.getPosition().makeValid(), true);
					}
				}
			}
		}
		scouter.move(ccSelect.getPosition().makeValid(), true);
		return true;
	}

	
	/**
	 * Send to gather minerals to the "current_worker" 
	 * @return true if can, false otherwise
	 */
	public boolean gatherMinerals(){
		//Verifies that the number of SCVs is less than the 3 per mineral node.
		//The scv must be completed
		if (!workersMineral.get(CCs.indexOf(ccSelect)).contains(current_worker) &&
				(workersMineral.get(CCs.indexOf(ccSelect)).size() < maxVce-workers.size()) && 
				current_worker.isCompleted()){
			//Gets mineral nodes
			for (Unit mineralNode : mineralNodes.get(CCs.indexOf(ccSelect))) {
				//Send the SCV to gather it
				if (current_worker.rightClick(mineralNode, false)) {
					workersMineral.get(CCs.indexOf(ccSelect)).add(current_worker);
					current_worker = null;
					return true;					
				}
			}	
		}
		return false;
	}
	
	/**
	 * Same as GatherMinerals, but with vespin gas
	 * @return
	 */
	public boolean gatherGas(){
		//Remove workers from vespin
		for (Unit u : workers) {
			if (u.getOrder() == Order.HarvestGas) {
				u.stop();
				u.move(ccSelect.getPosition());
			}
		}
		if (!workersVespin.get(CCs.indexOf(ccSelect)).contains(current_worker) &&
				workersVespin.get(CCs.indexOf(ccSelect)).size() < 2 && 
				current_worker.isCompleted() && !workers.contains(current_worker)) {
			for (Unit refinery : this.finishedBuildings) {
				if (refinery.getType() == UnitType.Terran_Refinery &&
						BWTA.getRegion(refinery.getPosition()) == 
						BWTA.getRegion(ccSelect.getPosition()) &&
						refinery.isCompleted()) {
					if (current_worker.rightClick(refinery, false)) {
						workersVespin.get(CCs.indexOf(ccSelect)).add(current_worker);
						current_worker = null;
						return true;	
					}
				}
			}			
		}
		return false;
	}
	
	/**
	 * Checks if player has more or equals mineral and gas than the passed in the args.
	 * @param mineral
	 * @param gas
	 * @return true if player has more or equal, false if not
	 */
	public boolean checkResources(int mineral, int gas){
		if (this.mineral >= mineral &&
				this.vespinGas >= gas){
			return true;
		}
		return false;
	}
	
	/**
	 * Checks if can train/build an unit
	 * @param unit
	 * @return check if player can make the unit
	 */
	public boolean canTrain(UnitType unit) {
		for (Unit u : finishedBuildings) {
			if (u.isCompleted() && u.canTrain(unit) && !u.isTraining()) {
				return true;
			}
		}
		return false;

	}
	
	/**
	 * Train an unit. 
	 * @param building
	 * @param unit
	 * @return true if can train the unit, false if not
	 */
	public boolean trainUnit(UnitType building, UnitType unit){
		for (Unit u : finishedBuildings){
			if (u.getType().equals(building) &&
					u.isCompleted() &&
					!u.isTraining()){
				if (u.train(unit)) {
					mineral -= unit.mineralPrice();
					vespinGas -= unit.gasPrice();
					return false;
				}
			}
		}
		return false;
	}
	
	/**
	 * Move a SCV to the "posBuild" position
	 * @return True if can move the SCV, false if not
	 */
	public boolean moveTo() {
		if (workers.get(0) != null){
			if (workers.get(0).getTilePosition().getDistance(posBuild) > 10)
				return workers.get(0).move(posBuild.toPosition().makeValid(), false);
			else
				return true;
		}
		return false;
	}
	
	/**
	 * Build a building.
	 * @param building
	 * @return false if the building is already being building.
	 * 		   true if can build the building
	 */
	public boolean buildUnit(UnitType building) {
		if (remainingBuildings.contains(building)) {
			return false;
		}
		if (workers.get(0).getOrder() == Order.PlaceBuilding) return true;
		if (workers.get(0).build(building, posBuild)) {
			mineral -= building.mineralPrice();
			vespinGas -= building.gasPrice();
			workers.get(0).move(ccSelect.getPosition(), true);
			return true;
		}
		return false;
	}
	
	/**
	 * Check if the "father" building exists
	 * @param building
	 * @return
	 */
	public boolean findBuilding(UnitType building) {
		for (Unit u : finishedBuildings) {
			if (u.isCompleted() && 
					u.getType() == building &&
					u.getAddon() == null) {
				addonBuilding = u;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Build an addon.
	 * @param addon
	 * @return false if the building is already being building.
	 * 		   true if can build the building
	 */
	public boolean buildAddon(UnitType addon) {
		if (addonBuilding.buildAddon(addon)) {
			mineral -= addon.mineralPrice();
			vespinGas -= addon.gasPrice();
			return true;
		}
		return false;
	}
	
	/**
	 * Checks if can research a new tech.
	 * @param res
	 * @return true if can upgrade, false is already being researched or if can't upgrade
	 */
	public boolean checkResearch(UpgradeType res) {
		if (this.researching.contains(res)) {
			return false;
		}
		if (this.connector.canUpgrade(res)){
			return true;
		}
		return false;
	}
	
	/**
	 * Do a research
	 * @param building
	 * @param res
	 * @return true if can research, false if not.
	 */
	public boolean doResearch(UnitType building, UpgradeType res) {
		for (Unit u : finishedBuildings) {
			if (u.getType() == building) {
				if (u.upgrade(res)) {
					mineral -= res.mineralPrice();
					vespinGas -= res.gasPrice();
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check if there is more than 10 units in the CP waiting.
	 * Used in Attack tree
	 * @return true if there's more than 10, false otherwise
	 */
	public boolean checkStateUnits(){
		if (boredSoldiers.size() > 10) {
			return true;
		}
		return false;
	}
	
	/**
	 * Check troops' status.
	 * Used in Attack tree
	 * @return Always True.
	 */
	public boolean checkStateTroops(){
		ArrayList<Troop> remove = new ArrayList<Troop>(0);
		for (Troop t : assaultTroop) {
			//If all units are dead, resets status and destination
			if (t.units.size() == 0 && t.status != 0) {
				remove.add(t);
			} else {
				ArrayList<Unit> removeU = new ArrayList<Unit>(0);
				boolean detector = false;
				for (Unit u : t.units) {
					if (u.isLoaded()) {
						removeU.add(u);
					}
				}
				t.units.removeAll(removeU);
				
				//To avoid a troop only form by vessels
				for (Unit u : t.units) {
					if (u.getType() == UnitType.Terran_Science_Vessel) {
						t.hasDetector = true;
						detector = true;
						break;
					}
				}
				if (!detector) { t.hasDetector = false; }
				
				t.isInPosition();
				//If the attackGroup fell, retreat.
				if (t.status != 0 && t.surrounded(connector) && 
						t.units.get(0).getDistance(defendGroup.destination.toPosition()) > 200) {
					t.destination = defendGroup.destination;
					for (Unit u : t.units) {
						u.move(t.destination.toPosition().makeValid(),false);
					}
					t.status = 4;
					t.lastChange = this.connector.elapsedTime();
				}
			}
			
		}
		assaultTroop.removeAll(remove);
		return true;
	}
	
	/**
	 * Compact troops.
	 * For example, if 2 troops' status == 4 && its units size < 10, merge both units list to make 1 troop.
	 * @return Always false, because it's only extra control, not a condition
	 */
	public boolean redistribuiteTroops() {
		//Predicate to filter all troops
		Predicate<Troop> predicate = new Predicate<Troop>() {
			public boolean test(Troop t) {
				return t.units.size() < 10 && (t.status == 4 || t.status == 5);
				
			}
		};
		Predicate<Troop> predicate2 = new Predicate<Troop>() {
			public boolean test(Troop t) {
				return t.units.size() > 0 && t.units.size() < 10 &&
						(t.status == 4 || t.status == 5 || t.status == 0);
				
			}
		};
		for (Object t : assaultTroop.stream().filter(predicate).toArray()) {
			for (Object t2 : assaultTroop.stream().filter(predicate2).toArray()) {
				//If aux doesn't contains any unit from t2 && their status it's the same -> merge units list.
				if (!((Troop ) t).equals((Troop) t2) &&
						((Troop) t).units.size() + ((Troop) t2).units.size() < 20) {
					((Troop) t).units.addAll(((Troop) t2).units);
					((Troop) t).status = 4;
					((Troop) t).destination = defendGroup.destination;
					
					if (((Troop) t2).hasDetector){
						((Troop) t).hasDetector = true;
					}
					assaultTroop.remove((Troop)t2);
					((Troop) t).lastChange = this.connector.elapsedTime();
				}
			}
		}
		return false;
	}
	
	/**
	 * Select military units to make an assault troop.
	 * Used in "attack".
	 * @return Always true.
	 */
	public boolean createTroop() {
		int i = 0;
		for (; i<assaultTroop.size(); i++) {
			if (assaultTroop.get(i).units.size() < 10 && assaultTroop.get(i).status != 4) {
				ArrayList<Unit> auxList = new ArrayList<Unit>(0);
				
				for (Unit u : boredSoldiers) {
					//Unit is idle, complete and not in a bunker
					if(u.isCompleted() && !u.isLoaded()) {
						if (u.getType() == UnitType.Terran_Science_Vessel) {
							if (!assaultTroop.get(i).hasDetector){
								assaultTroop.get(i).units.add(u);
								assaultTroop.get(i).hasDetector = true;
								auxList.add(u);
							}
						} else {
							assaultTroop.get(i).units.add(u);
							auxList.add(u);
						}
					}
					if (assaultTroop.get(i).units.size() > 15)
						break;
				}
				
				boredSoldiers.removeAll(auxList);
				return true;
			}
		}
		//All troops are full, so it's needed a new troop.
		if (i == assaultTroop.size()) {
			Troop t = new Troop();
			t.lastChange = this.connector.elapsedTime();
			assaultTroop.add(t);
		}
		return true;
	}
	
	/**
	 * Selects a group which don't do anything.
	 * @return True if can,  False otherwise.
	 */
	public boolean selectGroup() {
		int time = connector.elapsedTime();
		
		if (!expanded && militaryUnits.size() < 15) {
			return false;
		}
		//Troops with status == 0
		for (Troop t : assaultTroop){
			if (t.status == 0 && t.units.size() >= 10) {
				attackGroup = t;
				return true;				
			}
		}
		
		//Troops with status >= 5
		for (Troop t : assaultTroop) {
			if (t.status >= 4) {
				if (t.units.size() >= 10 || (time - t.lastChange) > 100) {
					attackGroup = t;
					return true;
				}
			}			
		}
		
		//Troops with status == 1
		for (Troop t : assaultTroop) {
			if (t.status == 1 && t.units.size() >= 0) {
				attackGroup = t;
				return true;
			}
		}
		
		for (Troop t : assaultTroop) {
			if ((time - t.lastChange) > 100) {
				attackGroup = t;
				return true;
			}
		}
		
		return false;
	}

	/**
	* Select position to attack/move
	* @return true if find a valid position, false otherwise.
	*/
	public boolean chooseDestination() {
		objective = getPosToAttack();
		if (objective.getX() == -1) return false;
		return true;
	}
	
	/**
	 * Calculates position to attack.
	 * Depends of influence and distance to CC.
	 * Distance to CC is more important.
	 * the closer, more important.
	 * @return Attacking position.
	 */
	public TilePosition getPosToAttack() {
		if (!intruders.isEmpty()) {
			return intruders.get(0).getTilePosition();
		}
		
		//Using units influence
		boolean units_map = true;
		//Found enemy units influence
		ArrayList<TilePosition> positions = this.influenceMap.getTempEnemyPositions();
		//If there's no enemy units positions, find the building positions
		if (positions.size() == 0) {
			//Set units_map false because we're using building map now
			units_map = false;
			//Found enemy building positions
			positions = influenceMap.getEnemyPositions();
			//If no enemy buildings either, return "error"
			if (positions.size() == 0)
				return new TilePosition(-1,-1);
		}
		//Now positions must contains a list of TilePositions.
		//Get the "default" tile to attack
		TilePosition attackPos = positions.get(0);
		//And its influence
		double tileInfluence = (units_map) ? this.influenceMap.getUnitTileInfluence(attackPos) : 
								this.influenceMap.getBuildingTileInfluence(attackPos);
		//Distance to the ccSelect
		int distCC = (int) ccSelect.getTilePosition().getDistance(attackPos); //Initial distance
		
		//Iterate over all the positions returned to get the one with biggest influence
		//based in its value and distance to the base
		for (TilePosition t : positions) {
			//The method to get the influence if different for each map
			if (units_map) {
				attackPos = (this.influenceMap.getUnitTileInfluence(t) < tileInfluence*1.5 &&
							ccSelect.getTilePosition().getDistance(t) < distCC) ? t : attackPos;
			} else {
				attackPos = (this.influenceMap.getBuildingTileInfluence(t) < tileInfluence*1.5 &&
							ccSelect.getTilePosition().getDistance(t) < distCC) ? t : attackPos;
			}
			tileInfluence = (units_map) ? this.influenceMap.getUnitTileInfluence(t) :
								this.influenceMap.getBuildingTileInfluence(t);
			distCC = (int) ccSelect.getTilePosition().getDistance(t);
		}
		return attackPos;
	}
	
	/**
	 * Checks if there's enemies inside the CC perimeter
	 * @return true if there is, false in other case
	 */
	public boolean checkPerimeter() {
		ArrayList<Unit> remove = new ArrayList<Unit>();
		for (Unit u : intruders) {
			for (Unit cc : CCs) {
				if (!connector.getUnitsInRadius(cc.getPosition(), 800).contains(u)) {
					remove.add(u);					
				} else if (!u.isAttacking() && u.getType().isWorker()) {
					remove.add(u);
				}
			}
		}
		intruders.removeAll(remove);
		if (intruders.isEmpty() && !militia.isEmpty()){
			for (Unit u : militia) {
//				u.move(ccSelect.getPosition(), false);
				if (!u.getType().isWorker()) {
					boredSoldiers.add(u);
				}
			}
			militia.clear();
		}
		for (Unit cc : CCs) {
			for (Unit u : connector.getUnitsInRadius(cc.getPosition(), 800)) {
				if (u.getPlayer().getID() != self.getID() && 
						!u.getType().isNeutral() && 
						!intruders.contains(u) &&
						BWTA.getRegion(cc.getPosition()).equals(BWTA.getRegion(u.getPosition()))) {
					
					if (u.getType().isWorker() && (u.isAttacking() || u.isStartingAttack()))
						intruders.add(u);
					else if (!u.getType().isWorker())
						intruders.add(u);
				}
			}
		}
		if (!intruders.isEmpty()){
			return true;
		}
		return false;
	}

	/**
	 * Send all military units to attack the enemies.
	 * If there's no military units, send 2 SCV.
	 * @return true if can attack, false otherwise
	 */
	public boolean attackIntruders() {
		for (Troop t : assaultTroop) {
			t.status = 2;
			for (Unit u : t.units) {
				militia.add(u);
			}
			t.units.clear();
		}
		for (Unit u : boredSoldiers) {
			if (!militia.contains(u)) {
				militia.add(u);
			}
		}
		for (Unit u : militaryUnits) {
			if (!militia.contains(u)) {
				militia.add(u);
			}
		}
		if (!militia.isEmpty()){
			for (Unit u : militia){
				if (boredSoldiers.contains(u)) boredSoldiers.remove(u);
				u.attack(intruders.get(0).getPosition(), true);
			}
			return true;			
		} else if (militia.size() < intruders.size()) {
			if (!workersMineral.get(0).isEmpty()) {
				militia.add(workersMineral.get(0).get(0));
				workersMineral.get(0).get(0).attack(intruders.get(0), false);
				workersMineral.get(0).remove(0);				
				return true;				
			}
		}
		return false;
	}
	
	/**
	 * Defend chokePoint/base
	 * @return true if there isn't enough units defending, false otherwise
	 */
	public boolean sendDefend(){
		for (Unit u : boredSoldiers) {
			if (u.isCompleted() && !defendGroup.units.contains(u) &&
					!u.getType().isMechanical() && !u.isFlying()) {
				if (defendGroup.units.size() < 8) {
					defendGroup.units.add(u);
				}
				if (numberChokePoints == 1) {
					defendGroup.destination = (Math.random() < 0.5) ? 
												BWTA.getNearestChokepoint(ccSelect.getPosition()).getSides().first.toTilePosition():
													BWTA.getNearestChokepoint(ccSelect.getPosition()).getSides().second.toTilePosition();
				}
				u.attack(defendGroup.destination.makeValid().toPosition(), true);
			}
		}
		if (boredSoldiers.size() > 10) {
			return false;
		}
		return true;
	}
	
	/**
	 * Send bored unit to the bunker, while there is space.
	 * @return True if can send unit to the bunker, false otherwise
	 */
	public boolean sendToBunker() {
		//Predicate to filter by type.
		Predicate<Unit> predicate = new Predicate<Unit>() {
			public boolean test(Unit u) {
				return u.getType() == UnitType.Terran_Marine && u.isCompleted();
				
			}
		};
		//Checks if some bunker has room for 1 more marine
		for (Unit b : bunkers) {
			if (b.getLoadedUnits().size() < 4) {
				for (Object u : boredSoldiers.stream().filter(predicate).toArray()) {
					b.load((Unit) u, false);
					for (Troop t : assaultTroop) {
						if (t.units.contains((Unit) u)) {t.units.remove((Unit) u ); }
					}
					return true;
				}
			}
			
		}
		return false;
	}
	
	/**
	 * If the objective it's in range, send the units to attack.
	 * @return true if sends unit to attack, false otherwise
	 */
	public boolean sendAttack(){
		if (attackGroup.tooFar() && ((this.connector.elapsedTime() - attackGroup.lastChange) < 100)) {
			return false;
		}
		if (attackGroup.status != 2 && attackGroup.status != 4) {
			attackGroup.status = 1;
			attackGroup.lastChange = this.connector.elapsedTime();
			attackGroup.destination = objective;
			for (Unit u : attackGroup.units) {
				if (!u.isAttacking()) {
					if (u.getType() == UnitType.Terran_Science_Vessel ||
							u.getType() == UnitType.Terran_Medic) {
						for (Unit unitToFollow : attackGroup.units) {
							if (unitToFollow.getType() != UnitType.Terran_Science_Vessel &&
							unitToFollow.getType() != UnitType.Terran_Medic) {
								u.follow(unitToFollow, false);
								break;
							}
						}
					} else {
						u.attack(objective.toPosition(), true);						
					}
				}
			}			
			return true;
		}
		return false;
	}
	
	/**
	 * If the objective it isn't in range, send the units to move to a closer point.
	 * @return true if it isn't in range, false otherwise
	 */
	public boolean sendRegroup(){
		attackGroup.status = 3;
		attackGroup.destination = attackGroup.units.get(0).getTilePosition();
		//if too far, group units
		for (Unit u : attackGroup.units) {
			u.attack(attackGroup.destination.toPosition().makeValid(), false);
		}
		
		return true;
	}
	
	/**
	 * Building "center" it's the ccSelect. It'll 4 attempts to find a valid position.
	 * In each attempt the radius will be increased.
	 * 
	 * @return true if can find a valid position, false otherwise
	 */
	public boolean findPosition(UnitType building) {
		if (posBuild != null && buildingType == building) {
			return true;
		}
		if (remainingBuildings.contains(building)) {
			return false;
		}
		//Seven minutes of game and not expanded yet, focus on expand.
		if (connector.elapsedTime() > 700 &&
				(!expanded && !remainingBuildings.contains(UnitType.Terran_Command_Center)) && 
				building != UnitType.Terran_Command_Center) {
			return false;
		}
		//Special case: Refinery
		if (building == UnitType.Terran_Refinery) {
			return findPositionRefinery();
		}
		//Special case: Expansion
		if (building == UnitType.Terran_Command_Center) {
			return findPositionCC();
		}
		//Special case: Buildings away from the CP
		if (building == UnitType.Terran_Barracks ||
				building == UnitType.Terran_Factory ||
						building == UnitType.Terran_Starport) {
			return findPositionAwayCP(building);
		}
		//Special case: Missile turrets and bunkers
		if (building == UnitType.Terran_Missile_Turret || 
				building == UnitType.Terran_Bunker){
			return findPositionBunkerTurret(building);
		}
		//No special case: others.
		if (numberChokePoints == 1) {
			//Gets the CP
			Chokepoint cp = BWTA.getNearestChokepoint(ccSelect.getPosition());
			Position cp_position = cp.getCenter().makeValid();
			//Variables to control the direction of the building
			byte x, y;
			//If CP is on the left/right side of the CC
			x = (ccSelect.getTilePosition().getX() < cp_position.getX()) ? (byte)1 : (byte)-1;
			//If CP is on the top/bottom of the CC
			y = (ccSelect.getTilePosition().getY() < cp_position.getY()) ? (byte)1 : (byte)-1;
			byte [][] pruebas = {{x,0},{x,y},{0,y},{(byte)(-1*x), (byte)(-1*y)},{(byte)(-1*x), 0},{0, (byte)(-1*y)}};
			//Looks for a place to build, testing all the directions and increasing the range up to x4
			for (int i=1; i<4; i++){
				for (int j=0; j<pruebas.length; j++) {
					//Point origen, Point maximo, UnitType building
					TilePosition pos = findPlace(new Point(ccSelect.getTilePosition().getX(), ccSelect.getTilePosition().getY()),
							new Point((ccSelect.getTilePosition().getX()+pruebas[j][0]*building.tileWidth()*i),
									(ccSelect.getTilePosition().getY()+pruebas[j][1]*building.tileHeight()*i)),
							building);
					//If the position is valid...
					if (this.connector.canBuildHere(pos, building) && 
							this.connector.isBuildable(pos, true) &&
							//Same region, do not go too far
							this.connector.getRegionAt(pos.toPosition()).getDistance(ccSelect.getRegion()) < 800 &&
							BWTA.getNearestBaseLocation(pos).getTilePosition().getDistance(pos) > 3 &&
							cp_position.toTilePosition().getDistance(pos) > 2){
						posBuild = pos;
						buildingType = building;
						return true;
					}				
				}
			}
		} else {
			byte [][] pruebas = {{1,0},{1,1},{0,1},{-1,0},{-1,-1},{0,-1}};
			for (int i=4; i>1; i--){
				for (int j=0; j<pruebas.length; j++) {
					//Point origen, Point maximo, UnitType building
					TilePosition pos = findPlace(new Point(ccSelect.getTilePosition().getX(), ccSelect.getTilePosition().getY()),
							new Point((ccSelect.getTilePosition().getX()+pruebas[j][0]*building.tileWidth()*i),
									(ccSelect.getTilePosition().getY()+pruebas[j][1]*building.tileHeight()*i)),
									building);
					//If the position is valid...
					if (this.connector.canBuildHere(pos, building) && 
							this.connector.isBuildable(pos, true) &&
							//Same region, do not go too far
							this.connector.getRegionAt(pos.toPosition()).getDistance(ccSelect.getRegion()) < 800 &&
							BWTA.getNearestBaseLocation(pos).getTilePosition().getDistance(pos) > 2){
						posBuild = pos;
						buildingType = building;
						return true;
					}				
				}
			}
		}
		//Can't find a position
		return false;
	}
	
	/**
	 * For build refineries
	 * @return true if finds a position, false if not.
	 */
	public boolean findPositionRefinery() {
		//Find the geiser
		for (Unit vespeno : this.connector.getNeutralUnits()){
			//It must be in the same region as the cc
			for (Unit cc : CCs) {
				if (vespeno.getType() == UnitType.Resource_Vespene_Geyser &&
						BWTA.getRegion(vespeno.getPosition()) ==
						BWTA.getRegion(cc.getPosition())) {
					
					posBuild = vespeno.getTilePosition();
					return true;
				}				
			}
		}
		//Don't find position to build the refinery
		return false;
	}
	
	/**
	 * For build expansions. Gets the closer empty BaseLocation.
	 * @return true if finds a position, false if not.
	 */
	public boolean findPositionCC() {
		//Selects the closer position to the original CC
		int dist = 9999;
		BaseLocation pos = null;
		for (BaseLocation aux : BWTA.getBaseLocations()) {
			//Checks to select a different BaseLocation, 
			//the distance is lower than the previous 
			//and the position is buildable.
			if (!aux.isStartLocation() &&
					cc.getTilePosition().getDistance(aux.getTilePosition()) < dist &&
					this.connector.canBuildHere(aux.getTilePosition(), UnitType.Terran_Command_Center) &&
					!aux.isIsland()) {
				//If closer, updates "dist" and "pos".
				dist = (int) cc.getTilePosition().getDistance(aux.getTilePosition());
				pos = aux;
			}
		}
		//It has found a position
		if (pos != null) {
			posBuild = pos.getTilePosition();
			return true;
		} else { 
			return false;
		}
	}
	
	/**
	 * For build specifics buildings away from the Choke Point
	 * @param building: Building to build (barracks, Factories and Starports)
	 * @return true if finds a position, false if not
	 */
	public boolean findPositionAwayCP(UnitType building) {
		//If numberChokePoints == 1, build away from the CP
		if (numberChokePoints == 1) {
			//Gets the CP
			Chokepoint cp = BWTA.getNearestChokepoint(ccSelect.getPosition());
			Position cp_position = cp.getCenter().makeValid();
			//Variables to control the direction of the building
			byte x, y;
			//If CP is on the left/right side of the CC
			x = (ccSelect.getTilePosition().getX() > cp_position.getX()) ? (byte)1 : (byte)-1;
			//If CP is on the top/bottom of the CC
			y = (ccSelect.getTilePosition().getY() > cp_position.getY()) ? (byte)1 : (byte)-1;
			byte [][] tests = {{0,y},{x,y},{x,0},{(byte)(-1*x), 0},{0, (byte)(-1*y)},{(byte)(-1*x), (byte)(-1*y)}};
			//Looks for a place to build, testing all the directions and increasing the range up to x4
			for (int i=1; i<4; i++){
				for (int j=0; j<tests.length; j++) {
					//Point origen, Point maximo, UnitType building
					TilePosition pos = findPlace(new Point(ccSelect.getTilePosition().getX(), ccSelect.getTilePosition().getY()),
							new Point((ccSelect.getTilePosition().getX()+tests[j][0]*building.tileWidth()*i),
									(ccSelect.getTilePosition().getY()+tests[j][1]*building.tileHeight()*i)),
							building);
					//If the position is valid...
					if (this.connector.canBuildHere(pos, building) && 
							this.connector.isBuildable(pos, true) &&
							this.connector.getRegionAt(pos.toPosition()).getDistance(ccSelect.getRegion()) < 800 &&
							BWTA.getNearestBaseLocation(pos).getTilePosition().getDistance(pos) > 3){
						posBuild = pos;
						buildingType = building;
						return true;
					}				
				}
			}
//			limit++;
		//If there's more than 1 CP, builds closer to the CC.
		} else {
			byte [][] tests = {{1,0},{1,1},{0,1},{-1,0},{-1,-1},{0,-1}};
			//Looks for a place to build, testing all the directions and increasing the range up to x4
			for (int i=1; i<4; i++){
				for (int j=0; j<tests.length; j++) {
					//Point origen, Point maximo, UnitType building
					TilePosition pos = findPlace(new Point(ccSelect.getTilePosition().getX(), ccSelect.getTilePosition().getY()),
							new Point((ccSelect.getTilePosition().getX()+tests[j][0]*building.tileWidth()*i),
									(ccSelect.getTilePosition().getY()+tests[j][1]*building.tileHeight()*i)),
							building);
					//If the position is valid...
					if (this.connector.canBuildHere(pos, building) && 
							this.connector.isBuildable(pos, true) &&
							this.connector.getRegionAt(pos.toPosition()).getDistance(ccSelect.getRegion()) < 800 &&
							BWTA.getNearestBaseLocation(pos).getTilePosition().getDistance(pos) > 2){
						posBuild = pos;
						buildingType = building;
						return true;
					}				
				}
			}
//			limit++;
		}
		return false;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean findPositionBunkerTurret(UnitType building) {
		byte [][] pruebas = {{1,0},{1,1},{0,1},{-1,0},{-1,-1},{0,-1}};
		if (numberChokePoints == 1) {
			Chokepoint cp = BWTA.getNearestChokepoint(ccSelect.getPosition());
			TilePosition cp_position = cp.getCenter().toTilePosition();
			for (int i=1; i<=4; i++){
				for (int j=0; j<pruebas.length; j++) {
					//Point origen, Point maximo, UnitType building
					TilePosition pos = findPlace(new Point(cp_position.getX(), cp_position.getY()),
											     new Point((cp_position.getX()+pruebas[j][0]*building.tileWidth()*i),
													(cp_position.getY()+pruebas[j][1]*building.tileHeight()*i) ),
											     building);
					boolean pass = false;
					if (pos.getX() != -1) {
						//The bunkers/turrets have to be spread.
						for (Unit u : finishedBuildings) { 
							if (pos.getDistance(cp_position) > 10 ||
									(u.getType() == building && 
									u.getTilePosition().getDistance(pos) < 10)) {
								pass = true;
								break;
							}	
						}
						//If the position is valid...
						if (!pass && this.connector.canBuildHere(pos, building) && 
								this.connector.isBuildable(pos, true) &&
								this.connector.getGroundHeight(pos) == this.connector.getGroundHeight(ccSelect.getTilePosition())){
//								this.connector.getRegionAt(pos.toPosition()).getDistance(ccSelect.getRegion()) < 800){
							posBuild = pos;
							buildingType = building;
							return true;
						}				
					}
				}
			}
		} else {
			for (int i=1; i<=1; i++){
				for (int j=0; j<pruebas.length; j++) {
					//Point origen, Point maximo, UnitType building
					TilePosition pos = findPlace(new Point(ccSelect.getTilePosition().getX(), ccSelect.getTilePosition().getY()),
											new Point((ccSelect.getTilePosition().getX()+pruebas[j][0]*building.tileWidth()*i),
													(ccSelect.getTilePosition().getY()+pruebas[j][1]*building.tileHeight()*i)),
											building);
					boolean pass = false;
					//The bunkers/turrets have to be spread.
					for (Unit u : finishedBuildings) { 
						if (u.getType() == building && 
								u.getTilePosition().getDistance(pos) < 10) {
							pass = true;
						}	
					}
					//If the position is valid...
					if (!pass && this.connector.canBuildHere(pos, building) && 
							this.connector.isBuildable(pos, true) &&
							this.connector.getGroundHeight(pos) == this.connector.getGroundHeight(ccSelect.getTilePosition())){
//							this.connector.getRegionAt(pos.toPosition()).getDistance(ccSelect.getRegion()) < 800){
						posBuild = pos;
						buildingType = building;
						return true;
					}				
				}
			}			
		}
		
		return false;
	}
	
	/**
	 * Updates the influence map
	 */
	public void updateInfluences(){
		influenceMap.updateMap(this.connector);
	}
	
	/**
	 * Checks if there is any damaged building
	 * @return true if there's at least 1 building, false otherwise.
	 */
	public boolean checkBuildings() {
		//To save time, if the list isn't empty, return true
		ArrayList<Unit> remove = new ArrayList<Unit>();
		for (Unit u : damageBuildings) {
			if (u.getHitPoints() - u.getType().maxHitPoints() == 0 || !u.exists()) {
				remove.add(u);
			}
		}
		damageBuildings.removeAll(remove);
		//If the list it's empty, look for damaged building
		for (Unit u : self.getUnits()) {
			if (u.getType().isBuilding()) {
				//If the building it's damaged, not being repaired and isn't in the damageBuildings list
				if ( ((u.getHitPoints() - u.getType().maxHitPoints() != 0) && !u.isBeingConstructed()) &&
						!damageBuildings.contains(u)) {
					damageBuildings.add(((Unit) u));
				}
			}
		}
		
		if (damageBuildings.isEmpty()) {
			repairer.clear();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sends a SCV to repair a building
	 * @return true if can, false otherwise
	 */
	public boolean repair() {
		byte cont = 0;
		for (Unit vce : repairer) {
			if (vce.rightClick(damageBuildings.get(0),false)) {
				//If it's going to be repaired, removes it from the list
				return true;
			}
		}
		if (repairer.size() < 3) {
			for (ArrayList<Unit> UL : workersMineral) {
				for (Unit vce : UL) {
					if (vce.rightClick(damageBuildings.get(0),false)) {
						//If it's going to be repaired, removes it from the list
						damageBuildings.remove(0);
						UL.remove(vce);
						repairer.add(vce);
						return true;
					}
				}
			}			
		}
		return false;
	}
	
    /**
     * Add enemy units influence to the unit map
     * @param unit: unit whose influence will be add
     */
	public void newUnitInfluence(Unit unit) {
		byte gh = (byte) this.connector.getGroundHeight(unit.getTilePosition());
		this.influenceMap.newUnit(unit);
	}

	/**
	 * Add building influence to the map
	 * @param building: Unit to add the influence
	 * @param b: true if ally - false if not
	 */
	public void newBuildingInfluence(Unit building, boolean b) {
		this.influenceMap.newBuilding(building, b, connector);
	}

	/**
	 * Removes the influence of a building
	 * @param unit: building destroyed
	 */
	public void removeInfluenceBuilding(Unit unit) {
		this.influenceMap.removeBuilding(unit);		
	}

	/**
	 * Removes the influence of an enemy unit
	 * @param unit: unit destroyed
	 */
//	public void removeInfluenceUnit(Unit unit) {
//		byte gh = (byte) this.connector.getGroundHeight(unit.getTilePosition());
//		this.influenceMap.removeUnit(unit, gh);		
//	}
	
	/**
     * Método que genera un mapa con los tamaños máximos
     * de edificios que se pueden construir desde una casilla
     * hacia abajo a la derecha.
     * 
     * V -> Vespeno
     * M -> Minerales
     * 0 -> No se puede construir
     * 
     * Se genera una posición inicial (0,0) y se va recorriendo
     * todo el mapa mirando si es construible/caminable una posición
     * 
     * Una vez se tiene generado el mapa se miran todos los recursos
     * y se sitúan en el mapa.
     */
    public void createMap() {
    	//La posición hay que desplazarla de 32 en 32 (tamaño de las casillas)
    	//Altura máxima del mapa en pixeles (Build)
		int maxHeight = this.connector.mapHeight();
		//Anchura máxima del mapa en pixeles (Build)
		int maxWidth = this.connector.mapWidth();
		//Mapa a generar
		map = new int[maxHeight][maxWidth];
		//Tamaño máximo del edificio que se puede construir
		int dimension;

		for(int f = 0; f < maxHeight; f++){
			for(int c = 0; c < maxWidth; c++){
				//Posición a evaluar
				TilePosition pos_aux = new TilePosition(c, f);
				//Tamaño máximo posible a construir
				dimension = 0;
				//Variable que detiene la búsqueda si se encuentra un obstáculo
				boolean zonaLibre = true;
				
				//para cada posición se mira si se está en los límites del mapa y
				//hay que verificar si la posición es construible
				if (f+1>=maxHeight || c+1>=maxWidth ||
					!this.connector.isBuildable(pos_aux)){ zonaLibre = false; }
				
				//Se obtiene la altura de la posición
				int altura = this.connector.getGroundHeight(pos_aux);
				while(zonaLibre && dimension <= 6){
					dimension++;
					//Se verifica vertical, horizontal y diagonalmente si son válidas las posiciones.
					//Si alguna no lo es, se sale del while y se guarda el valor en el mapa
					for(int i = 0; i < dimension; i++){
						//matriz[i+f][c+dimension]	Comprueba columnas
						if (this.connector.isBuildable(new TilePosition(c+dimension, f+i), true)){ // ¿Es construible?
							if(this.connector.getGroundHeight(new TilePosition(c+dimension, f+i)) != altura){ // ¿Están a diferente altura?
								zonaLibre = false;
							}
						}
						else{ zonaLibre = false; }
						
						//matriz[f+dimension][i+c]    Comprueba filas
						if (this.connector.isBuildable(new TilePosition(c+i, f+dimension), true)) {
							if(this.connector.getGroundHeight(new TilePosition(c+i, f+dimension)) != altura){
								zonaLibre = false;
							}
						}
						else{ zonaLibre = false; }
					
						//matriz[f+dimension][c+dimension]   Comprueba en la diagonal (se podría cambiar cambiando la condicion del for a <=)
						if (this.connector.isBuildable(new TilePosition(c+dimension, f+dimension), true)) {
							if(this.connector.getGroundHeight(new TilePosition(c+dimension, f+dimension)) != altura){
								zonaLibre = false;
							}
						}
						else{ zonaLibre = false; }
					}
					//si se está en los límites del mapa en la próxima iteración, se sale del bucle.
					//creo que no hace falta porque isBuildable ya lo comprueba
					if (f+1+dimension>=maxHeight || c+1+dimension>=maxWidth){ zonaLibre = false;	}

				}
				// Se resta 1 porque hemos aumentado en 1 la dimensión suponiendo que la siguiente posición es válida
				if (dimension != 0) { dimension--; }
				// Se actualiza la posición
				map[f][c] = (dimension);	
			}
		}
		
		// Ahora se buscan los nodos de recursos y se les pone valores especiales:
		// -1 Para minerales
		// -2 Para vespeno.
		// getTilePosition devuelve la posición superior izquierda
		for (Unit u : this.connector.getNeutralUnits()){
			if (u.getType() == UnitType.Resource_Mineral_Field ||
					u.getType() == UnitType.Resource_Mineral_Field_Type_2 ||
					u.getType() == UnitType.Resource_Mineral_Field_Type_3) {
				//para recolectar minerales vale con que el vce vaya a cualquiera de sus casillas.
				map[u.getTilePosition().getY()][u.getTilePosition().getX()] = -1;
			}
			if (u.getType() == UnitType.Resource_Vespene_Geyser) {
				//Para construir la refinería nos vale la casilla arriba a la izquierda.
				map[u.getTilePosition().getY()][u.getTilePosition().getX()] = -2;
			}
		}
		
		//Los chokePoint se tratan de forma distinta y se ponen como
		//ocupados para evitar problemas en mapas donde son construibles
		for (Chokepoint cp : BWTA.getChokepoints()) {
			updateMap(cp.getCenter().toTilePosition(), UnitType.Terran_Command_Center);
		}
		
		//De igual forma con las localizaciones de las bases
		for (BaseLocation cp : BWTA.getBaseLocations()) {
			updateMap(cp.getTilePosition(), UnitType.Terran_Command_Center);
		}
	}
    
    /**
     * Para poder crear la matriz deben ser en diagonal.
     * Para contemplar casos en el que origen y máximo sean en horizontal (mismo Y)
     * cuando ocurra eso, se toma Y como la Y del edificio. 
     * 
     * El método devolverá un Position que indica la casilla superior izquierda válida donde construir el edificio.
     * Si se devuelve -1 en X no hay posición válida.
     */
    public TilePosition findPlace(Point origen, Point maximo, UnitType building){
    	//Si no se pasan valores correctos, se devuelve posición inválida
    	if (origen.x < 0 || origen.y < 0 ||
    			maximo.x < 0 || maximo.y < 0) {
    		return new TilePosition(-2,0);
    	}
    	
    	int xMaximo, xOrigen, yOrigen, yMaximo;
    	
    	int width = (building == UnitType.Terran_Factory ||
				building == UnitType.Terran_Starport ||
				building == UnitType.Terran_Science_Facility) ? building.tileWidth()+2 : building.tileWidth();
    	int height = building.tileHeight();
    	
    	//Se considera que sea misma fila o columna.
    	if (origen.x == maximo.x && maximo.x + width < map[0].length) {
    		maximo.x += width;
    	}
    	if (origen.y == maximo.y && maximo.y + height < map.length) {
    		maximo.y += height;
    	}
    	
    	//Limites de la submatriz X e Y
    	//Eje X
    	if (origen.x < maximo.x) {
    		//Origen está antes que el maximo
    		xMaximo = (maximo.x > map[0].length ? map[0].length : maximo.x);
    		xOrigen = origen.x;
    	} else {
    		//Maximo está antes que el origen
    		xMaximo = (origen.x > map[0].length ? map[0].length : origen.x);
    		xOrigen = maximo.x;
    	}
    	//Lo mismo con el eje Y
    	if (origen.y < maximo.y) {
    		yMaximo = (maximo.y > map.length ? map.length : maximo.y);
    		yOrigen = origen.y;
    	} else {
    		yMaximo = (origen.y > map.length ? map.length : origen.y);
    		yOrigen = maximo.y;
    	}
    	
    	//Valor a buscar de posiciones
    	int max = (height > width) ? height : width;
    	//Variable de control para la búsqueda
    	boolean found = false;
    	//Se recorre el mapa entre las posiciones dadas
    	for (int y = yOrigen; y < yMaximo-1 && !found; y++){
    		for (int x = xOrigen; x < xMaximo-1 && !found; x++){
    			//si encuentra una posición válida sale.
    			if (map[y][x] >= max) {return new TilePosition(x, y);}
    			//Este bucle busca en vertical hacia abajo
    			for (int y2=0; y2<=(x-xOrigen) && y+y2 < map.length;y2++) {
    				if (map[y+y2][x] >= max) {
    					return new TilePosition(x, y+y2);
    				}
    			}
    			//Este bucle mira desde la y que se quedó el bucle anterior y va horizontalmente hacia atrás
    			for (int x2=0; x2<=(x-xOrigen) && y+(x-xOrigen) < map.length;x2++) {
    				if (map[y+(x-xOrigen)][x-x2] >= max) {
    					return new TilePosition(x-x2, y+(x-xOrigen));
    				}
    			}
    			/*No hace falta mirar exclusivamente la diagonal 
    			 * porque se mira mediante las dos comprobaciones anteriores
    			 * ya que se va aumentando poco a poco el área
    			 */
    		}
    	}
    	
    	//Si llega aquí no ha encontrado nada
		return new TilePosition(-1,0);
    }
    
    /**
     * Se da por supuesto que las posiciones indicadas son posiciones correctas.
     * La posición origen ha sido obtenida mediante el método findPlace y la posición
     * destino ha sido calculada con el tamaño del edificio + la posición origen
     */
    public void updateMap(TilePosition origen, UnitType building) {
    	
    	TilePosition destino = new TilePosition(origen.getX()+building.tileWidth(),
    											origen.getY()+building.tileHeight());
    	int extraX = 0;
    	int extraY = (destino.getY()+1 < this.map.length && 
    			(building != UnitType.Terran_Bunker || building != UnitType.Terran_Missile_Turret)) ? 1 : 0;
    	if (destino.getX()+1 < this.map[0].length &&
    			(building == UnitType.Terran_Factory || 
    			building == UnitType.Terran_Starport || 
				building == UnitType.Terran_Science_Facility || 
				building == UnitType.Terran_Command_Center)) {
    		extraX = 1;
    	}
    	
    	//se recorre la matriz entre las posiciones dadas
    	for (int i = origen.getY(); i <= destino.getY()+extraY; i++){
    		for(int j = origen.getX(); j <= destino.getX()+extraX; j++){
    			//se ponen como ocupadas las casillas
    			map[i][j] = 0;
    		}
    	}
    	/*
    	 *  Para actualizar el resto de la matriz, tendremos que explorar las casillas superiores y por la izquierda.
    	 *  Dado que también hay que tener en cuenta las diagonales, se hará de tal forma que primero se actualicen
    	 *  todas las superiores incluidas las diagonales y después las de la izquierda. 
    	 */
    	// Esta variable se usará para saber si hemos terminado la actualización
    	boolean parada = true;
    	// Esta variable servirá para desplazarnos verticalmente y tambien saber que dimensión maxima puede tener el edificio de esa casilla
    	int iv = 1;
    	// Esta variable servirá para desplazarnos horizontalmente
    	int ih = 0;
    	
    	// Bucle de actualización vertical
    	while (parada){
    		//Si no nos salimos del mapa, el valor actual de la dimensión no es 4 (máximo)
    		if (((origen.getY()-iv >= 0 && destino.getX()+extraX-ih >= 0) && 
    				map[origen.getY()-iv][destino.getX()+extraX-ih] > iv) && 
    				(iv < 6) ) { // Si llegamos a 4 no es necesario seguir
    			map[origen.getY()-iv][destino.getX()+extraX-ih] = iv;
    			iv++;
    		} else{ // Hemos terminado con la columna, pasamos a la siguiente (hacia atrás en el mapa)
    			if (iv == 1){
    				parada = false; // Si en la primera casilla no hay que actualizar, significa que hemos terminado.
    			} else {
    				ih++;
        			iv = 1;
    			}
    		}
    	}
    	//Resetear valores
    	parada = true;
    	ih = 1;
    	iv = 0;
  
    	// Bucle horizontal
    	while (parada){
    		if (((origen.getY()+iv < map.length && origen.getX()-ih >= 0) &&
    				map[origen.getY()+iv][origen.getX()-ih] > ih) && 
    				(ih < 6) ) { // Si llegamos a 4 no es necesario seguir
    			map[origen.getY()+iv][origen.getX()-ih] = ih;
    			ih++;
    		} else { // Hemos terminado con la fila, pasamos a la siguiente (hacia abajo en el mapa)
    			if (ih == 1 || origen.getY()+iv == destino.getY()+extraY){
    				parada = false; // Si en la primera casilla no hay que actualizar, significa que hemos terminado.
    			} else {
    				iv++;
        			ih = 1;
    			}
    		}
    	}
    }
    
    public void debug() {
    	if (scouter != null) {
    		connector.drawTextMap(scouter.getPosition(), "Mr. Stalker");
    		connector.drawTextScreen(250, 20, Utils.formatText("Mr. Stalker on the way", Utils.Turquoise));
    	}
    	for (Unit db : damageBuildings) {
    		connector.drawTextMap(db.getPosition(), Utils.formatText("FIRE FIRE!",Utils.Red_P1));
    	}
    	
		for (Chokepoint cp : BWTA.getChokepoints()) {
			connector.drawCircleMap(cp.getCenter(), 80, Color.Blue);
		}
		
		for (Unit cc : CCs) {
			connector.drawCircleMap(cc.getPosition(), 800, Color.Blue);
		}
		for (Troop t : assaultTroop) {
			for (Unit u : t.units) {
				connector.drawTextMap(new Position(u.getPosition().getX(),u.getPosition().getY()+10), ""+assaultTroop.indexOf(t));
			}
		}
		for (ArrayList<Unit> wm : workersMineral) {
			for (Unit u : wm) {
				connector.drawTextMap(new Position(u.getPosition().getX(),u.getPosition().getY()+10), "M");
			}
		}
		for (ArrayList<Unit> vm : workersVespin) {
			for (Unit u : vm) {
				connector.drawTextMap(new Position(u.getPosition().getX(),u.getPosition().getY()+10), "V");
			}
		}
		for (Unit u : repairer) {
			connector.drawCircleMap(u.getPosition(), 4, Color.Purple, true);
		}
		
		for (ArrayList<Unit> mn : mineralNodes) {
			for (Unit m : mn) {
				connector.drawCircleMap(m.getPosition(), 4, Color.Cyan, true);
			}
		}
		for (Unit u : workers){
			connector.drawCircleMap(u.getPosition(), 4, Color.Orange, true);
			connector.drawTextMap(new Position(u.getPosition().getX(),u.getPosition().getY()+10), ""+u.getOrder());
		}
		if (posBuild != null) {
			connector.drawBoxMap(posBuild.toPosition(), new Position(posBuild.toPosition().getX()+32,
												posBuild.toPosition().getY()+32), Color.Green);
		}
		
		connector.drawTextScreen(250, 10, ""+this.connector.elapsedTime());
    	
    	connector.drawTextScreen(10, 10, "Enemy Race = "+enemyRace);
    	ArrayList <UnitType> txtList = new ArrayList<UnitType>();
    	for (UnitType ut : remainingBuildings) {
    		if (!txtList.contains(ut)) txtList.add(ut);
    	}
    	
    	connector.drawTextScreen(10, 20, "MineralNodes = "+ Utils.formatText(""+mineralNodes.size(), Utils.Blue));
    	
    	String txt = "";
    	for (UnitType ut : txtList) {
    		txt += ut +", ";
    	}
    	connector.drawTextScreen(10, 30, "RemainingBuildingds: " + Utils.formatText(txt, Utils.Red));
    	txtList.clear();
    	for (Unit u : remainingUnits) {
    		if (!txtList.contains(u)) txtList.add(u.getType());
    	}
    	txt = "";
    	for (UnitType ut : txtList) {
    		txt += ut +", ";
    	}
    	txtList.clear();
    	connector.drawTextScreen(10, 40, "RemainingUnits: " + Utils.formatText(txt, Utils.Orange));  
    	for (Unit u : finishedBuildings) {
    		if (!txtList.contains(u.getType())) txtList.add(u.getType());
    	}
    	connector.drawTextScreen(10, 50, "BoredUnits size: " + Utils.formatText(""+boredSoldiers.size(), Utils.Turquoise));
    	connector.drawTextScreen(10, 60, "MilitaryUnits size: " + Utils.formatText(""+militaryUnits.size(), Utils.Pinkish));
    	connector.drawTextScreen(10, 70, "Number of troops: " + Utils.formatText(""+assaultTroop.size(), Utils.Greygreen));
    	byte i = 0;
		if (assaultTroop.size() > 0){
			for (Troop t : assaultTroop){
				connector.drawTextScreen(20, 80+i*10, "Status:"+t.status+", Units: "+t.units.size() + ", Change: " + t.lastChange + ", Detector: "+t.hasDetector);
				i++;
			}		
		}
		connector.drawTextScreen(10, 90+i*10, Utils.formatText("Intruders: " + intruders.size(),Utils.Purple));
		if (intruders.size() > 0){
			for (Unit intr : intruders){
				connector.drawCircleMap(intr.getPosition(), 4, Color.Red, true);
			}
		}
		connector.drawTextScreen(10, 100+i*10, Utils.formatText("Militia: " + militia.size(),Utils.Green));
		if (militia.size() > 0){
			for (Unit milt : militia){
				connector.drawCircleMap(milt.getPosition(), 4, Color.Blue, true);
			}
		}
		connector.drawTextScreen(10, 110+i*10, Utils.formatText("Repairers: "+repairer.size(), Utils.Pinkish));
		i = 0;
    	connector.drawTextScreen(450, 20+i*10, "FinishedBuildings: ");
    	for (UnitType ut : txtList) {
    		byte repet = 1;
    		if (ut == UnitType.Terran_Barracks) repet =  barracks;
			if (ut == UnitType.Terran_Engineering_Bay) repet = bay;
			if (ut == UnitType.Terran_Academy) repet = academy;
			if (ut == UnitType.Terran_Factory) repet = factory;
			if (ut == UnitType.Terran_Armory) repet = armory;
			if (ut == UnitType.Terran_Bunker) repet = (byte) bunkers.size();
			if (ut == UnitType.Terran_Science_Facility) repet = labCient;
			if (ut == UnitType.Terran_Starport) repet = starport;
			if (ut == UnitType.Terran_Command_Center) repet = (byte) CCs.size();
    		connector.drawTextScreen(460, 30+i*10, Utils.formatText(""+ut,Utils.Orange)+" - x"+repet);
    		i++;
    	}
    	connector.drawTextScreen(450, 40+i*10, "DamageBuildings: ");
    	for (Unit u : damageBuildings) {
    		connector.drawTextScreen(460, 50+i*10, Utils.formatText(""+u.getType(),Utils.Yellow));
    		i++;
    	}
    	
    	if (objective.getX() > 0) {
    		connector.drawCircleMap(objective.toPosition(), 6, Color.Red, true);
    	}
    	//Draw regions
//    	for (bwapi.Region r : connector.getAllRegions()) {
//    		connector.drawCircleMap(r.getPoint(), 10, Color.Yellow, true);
//    	}
    	//Draw buildings influence
//    	for (int y=0; y<influenceMap.buildMap.length; y++) {
//    		for (int x=0; x<influenceMap.buildMap[0].length; x++){
//    			if (influenceMap.buildMap[y][x] > 0 || influenceMap.buildMap[y][x] < 0 )
//    				connector.drawTextMap(x*32, y*32, Utils.formatText(""+influenceMap.buildMap[y][x],Utils.Red));
//    		}
//    	}
    	//Draw units influence
//    	for (int y=0; y<influenceMap.unitMap.length; y++) {    //Height in 3d
//    		for (int x=0; x<influenceMap.unitMap[y].length; x++){    //width 
//				for (int z=0; z<influenceMap.unitMap[y][x].length; z++){    //height in 2d
//	    			if (influenceMap.unitMap[y][x][z] > 0 || influenceMap.unitMap[y][x][z] < 0 )
//	    				connector.drawTextMap(x*32, z*32, Utils.formatText(""+influenceMap.unitMap[y][x][z],Utils.Orange));
//    			}
//    		}
//    	}
    	//Draw construction spaces
		for (int y=0; y<map.length; y++) {
			for (int x=0; x<map[0].length; x++){
				if (map[y][x] != 0)
					connector.drawTextMap(x*32, y*32, Utils.formatText(""+map[y][x],Utils.Brown));
			}
		}
    	
    }

}
