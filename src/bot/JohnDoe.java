package bot;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.iaie.btree.util.GameHandler;

import jnibwapi.BaseLocation;
import jnibwapi.ChokePoint;
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.Position.PosType;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType;

public class JohnDoe extends GameHandler {
	
	//Inner control lists
	List<Integer> CCs;							//List for counting number of CCs
	List<ArrayList<Unit>> VCEs;					//List for counting number of SCVs from each CC.
	List<ArrayList<Integer>> workersMineral; 	//List for counting number of SVCs gathering minerals.
	List<ArrayList<Integer>> workersVespin; 	//List for counting number of SCVs gathering vespin gas.
	List<UnitType> remainingUnits; 				//List to know which units are being trained.
	List<Unit> militaryUnits;					//List to know all military unit trained (alive).
	List<Unit> boredSoldiers;					//List of unit which are defending base.
	List<Troop> assaultTroop;					//List of groups of troops created.
	Troop attackGroup;							//Group of attacking units currently selected.
	Troop defendGroup;							//Group of defending units.
	List<UnitType> remainingBuildings;			//List to know which buildings are being builded.
	List<Unit> finishedBuildings; 				//List to know all finished (and alive) buildings.
	List<UpgradeType> researching;				//List to know which researches are being researched.
	List<Unit> damageBuildings;					//List to know which buildings are being attacked and being able to fix it later.
	List<Unit> workers;							//List to control which SCVs are assigned to building.
	Unit current_worker;						//Variable to know which SCV is currently selected.
	List<Unit> bunkers;							//List to know the bunkers I have
		
	int supplies, totalSupplies;
	byte barracks, refinery, factory, 
		academy, armory, bay, max_vce, lab_cient, starport, number_chokePoints;
	
	List<ChokePoint>[][] chokePoints;
	
	//Variables for controlling initial cc and expansions.
	Unit cc, cc_select;
	
	//Position where the last building is going to be built.
	Position posBuild;
	//Position to attack.
	Position objective;
	
	int[][] map;
	
	InfluenceMap dah_map;

	public JohnDoe(JNIBWAPI bwapi) {
		super(bwapi);

		cc 						= null;
		cc_select 				= null;
		workers 				= new ArrayList<Unit>(3);
		militaryUnits			= new ArrayList<Unit>(0);
		boredSoldiers			= new ArrayList<Unit>(0);
		finishedBuildings 		= new ArrayList<Unit>(0);
		damageBuildings			= new ArrayList<Unit>(0);
		bunkers 				= new ArrayList<Unit>(0);
		CCs 					= new ArrayList<Integer>(0);
		VCEs 					= new ArrayList<ArrayList<Unit>>(0);
		workersMineral 			= new ArrayList<ArrayList<Integer>>(0);
		workersVespin 			= new ArrayList<ArrayList<Integer>>(0);
		researching				= new ArrayList<UpgradeType>(0);
		remainingUnits 			= new ArrayList<UnitType>(0);
		remainingBuildings 		= new ArrayList<UnitType>(0);
		assaultTroop			= new ArrayList<Troop>(0);
		attackGroup				= new Troop();
		defendGroup				= new Troop();
		barracks = refinery = factory = 
		academy = armory = bay = lab_cient = starport = 0;
		number_chokePoints 		= (byte) this.connector.getMap().getRegion(this.connector.getSelf().getStartLocation()).getChokePoints().size();
		max_vce = 20;
		dah_map 				= new InfluenceMap(bwapi.getMap().getSize().getBY(), bwapi.getMap().getSize().getBX());
	}
	
	/**
	 * Adds the corresponding lists to the new CC
	 * @param cc_pos
	 */
	public void addCC(int cc_pos) {
		VCEs.add(new ArrayList<Unit>());
		workersMineral.add(new ArrayList<Integer>());
		workersVespin.add(new ArrayList<Integer>());
	}
	
	/**
	 * Get a SCV which is free.
	 * A SCV is free when he isn't gathering mineral/vespin or idle
	 * @return
	 */
	public boolean getWorker() {
		for (ArrayList<Unit> vces_cc : VCEs) {
			for (Unit vce : vces_cc) {
				//Checks if the unit type equals SCV and is idle.
				if ((!workersMineral.get(VCEs.indexOf(vces_cc)).contains(vce.getID()) &&
					 !workersVespin.get(VCEs.indexOf(vces_cc)).contains(vce.getID())) &&
					 vce.isIdle() && vce.isCompleted() && CCs.size() > 0) {
					current_worker = vce;
					cc_select = this.connector.getUnit(CCs.get(VCEs.indexOf(vces_cc)));
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
		if (workers.size() < 3){
			//If there's a free SCV from the list "workers", return true
			for (Unit vce : workers) {
				if (!vce.isConstructing()){
					return true;
				}
			}
			//Chooses 1 SCV from the "workersMineral" list (from the original CC).
			for (int vce : this.workersMineral.get(0)) {
				//If it isn't in the "workers" list...
				if (!workers.contains(this.connector.getUnit(vce))) {
					//Adds the SCV to the "workers" list
					workers.add(this.connector.getUnit(vce));
					return true;					
				}
			}
			//There isn't any free SCV
			return false;
		}
		//If "workers" is full, check if there's any free SCV
		else if (workers.size() <= 3){
			for (Unit vce : workers) {
				if (!vce.isConstructing()){
					return true;
				}
			}
			return false;
		}
		//"workers" is full and the 3 SCV are busy.
		else {return false;}
	}
	
	/**
	 * Se manda a recolectar minerales al trabajador seleccionado,
	 * ya que antes de llamar a esta función se llama a getWorker 
	 * @return
	 */
	public boolean gatherMinerals(){
		//Se verifica que no se pase del número de trabajadores y que el VCE está
		//completado, ya que a veces se selecciona sin haber completado el entrenamiento.
		if ((workersMineral.get(CCs.indexOf(cc_select.getID())).size() < max_vce-2) && current_worker.isCompleted()){
			//Se buscan los minerales cercanos a la base.
			for (Unit recurso : this.connector.getNeutralUnits()) {
				if (recurso.getType().isMineralField()) {
					if (this.connector.getMap().getRegion(recurso.getPosition()) == 
							this.connector.getMap().getRegion(cc_select.getPosition())) {
						//Se manda al VCE a recolectar
						this.connector.getUnit(current_worker.getID()).rightClick(recurso, false);
						workersMineral.get(CCs.indexOf(cc_select.getID())).add(current_worker.getID());
						current_worker = null;
						return true;
					}
				}
			}	
		}
		return false;
	}
	
	/**
	 * Same as GatherMinerals
	 * @return
	 */
	public boolean gatherGas(){
		if (workersVespin.get(CCs.indexOf(cc_select.getID())).size() < 2 && current_worker.isCompleted()) {
			for (Unit refinery : this.connector.getMyUnits()) {
				if (refinery.getType() == UnitTypes.Terran_Refinery && refinery.isCompleted()){
					this.connector.getUnit(current_worker.getID()).rightClick(refinery, false);
					workersVespin.get(CCs.indexOf(cc_select.getID())).add(current_worker.getID());
					current_worker = null;
					return true;	
				}
			}			
		}
		return false;
	}
	
	/**
	 * Checks if player has more or equals mineral and gas than the passed in the args.
	 * @param mineral
	 * @param gas
	 * @return
	 */
	public boolean checkResources(int mineral, int gas){
		if (this.connector.getSelf().getMinerals() >= mineral &&
				this.connector.getSelf().getGas() >= gas){
			return true;
		}
		return false;
	}
	
	/**
	 * Checks if can train/build an unit
	 * @param unidad
	 * @return
	 */
	public boolean canTrain(UnitType unidad) {
		if (this.connector.canMake(unidad)) {
			return true;
		}
		return false;

	}
	
	/**
	 * Train an unit. 
	 * @param edificio
	 * @param unidad
	 * @return
	 */
	public boolean trainUnit(UnitType edificio, UnitType unidad){
		for (Unit u : finishedBuildings){
			if (u.getType() == edificio && !u.isTraining()){
				u.train(unidad);
				remainingUnits.add(unidad);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Método que mueve un VCE a la posición a la que se quiere construir el edificio
	 * @return True si manda que se mueva. False si no se puede mandar
	 */
	public boolean moveTo() {
		for (Unit vce : workers){
			if (vce.getPosition().getApproxBDistance(posBuild) < 10) {
				return true;
			} else {
				if (!vce.isMoving()){
					return vce.move(posBuild.makeValid(), false);					
				} else {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Build a building.
	 * @param edificio
	 * @return
	 */
	public boolean buildUnit(UnitType edificio) {
		if (remainingBuildings.contains(edificio)) {
			return false;
		}
		for (Unit vce : workers){
			if (vce.getPosition().getApproxBDistance(posBuild) < 10){
				return vce.build(posBuild, edificio);
			}
		}
		return false;
	}
	
	/**
	 * Checks if can research a new tech.
	 * @param res
	 * @return
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
	 * @return
	 */
	public boolean doResearch(UnitType building, UpgradeType res) {
		for (Unit u : finishedBuildings) {
			if (u.getType() == building) {
				return u.upgrade(res);
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
		if (boredSoldiers.size() > 0) {
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
				t.isInPosition();
				//If the attackGroup fell, retreat.
				if (t.status != 0 && t.units.size() <= 5 && t.units.size() > 0) {
					t.destination = defendGroup.destination;
					for (Unit u : t.units) {
						u.move(t.destination.makeValid(), false);
					}
					t.status = 4;
//				return true;
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
				return t.units.size() < 10 && (t.status == 4 || t.status == 5 || t.status == 0);
				
			}
		};
//		ArrayList <Troop> remove = new ArrayList<Troop>(0);
		for (Object t : assaultTroop.stream().filter(predicate).toArray()) {
			for (Object t2 : assaultTroop.stream().filter(predicate2).toArray()) {
				//If aux doesn't contains any unit from t2 && their status it's the same -> merge units list.
				if (!((Troop ) t).equals((Troop) t2) &&
						((Troop) t2).status == ((Troop) t).status &&
						((Troop) t).units.size() + ((Troop) t2).units.size() < 20) {
					((Troop) t).units.addAll(((Troop) t2).units);
					((Troop) t).status = 4;
					((Troop) t).destination = defendGroup.destination;
					assaultTroop.remove((Troop)t2);
				}
			}
		}
//		assaultTroop.removeAll(remove);
		
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
					if(u.isIdle() && u.isCompleted() && !u.isLoaded()) {
						assaultTroop.get(i).units.add(u);
						auxList.add(u);
					}
				}
				
				boredSoldiers.removeAll(auxList);
				return true;
			}
		}
		//All troops are full, so it's needed a new troop.
		if (i == assaultTroop.size()) {
			assaultTroop.add(new Troop());
		}
		return true;
	}
	
	/**
	 * Selects a group which don't do anything.
	 * @return True if can,  False otherwise.
	 */
	public boolean selectGroup() {
//		if (assaultTroop.size() > 0){
//			System.out.println("-------------------");
//			for (Troop t : assaultTroop){
//				System.out.println("Estado:"+t.status+", Tropas: "+t.units.size()+", Bored: "+boredSoldiers.size());
//			}
//			System.out.println("+++++++++++++++++++");			
//		}
		
		//Troops with status == 0
		for (Troop t : assaultTroop){
			if (t.status == 0) {
				if (t.units.size() >= 10) {
					attackGroup = t;
					return true;
				}				
			}
		}
		
		//Troops with status >= 5
		for (Troop t : assaultTroop) {
			if (t.status >= 5 && t.units.size() >= 10) {
				attackGroup = t;
				return true;
			}			
		}
		
		//Troops with status == 1
		for (Troop t : assaultTroop) {
			if (t.status == 1 && t.units.size() >= 10) {
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
		return true;
	}
	
	/**
	 * Calculates position to attack.
	 * Depends of influence and distance to CC.
	 * Distance to CC is more important.
	 * the closer, more important.
	 * @return Attacking position.
	 */
	public Position getPosToAttack() {
		ArrayList<int[]> positions = dah_map.getEnemyPositions(); //Enemy positions
		Position ret = new Position(positions.get(0)[1], positions.get(0)[0], PosType.BUILD); //Default position
		double infl = dah_map.mapa[positions.get(0)[0]][positions.get(0)[1]]; //Default influence
		int dist = cc_select.getPosition().getApproxWDistance(ret); //Initial distance
		
		for (int[] i : positions) {
			Position aux = new Position(i[1], i[0], PosType.BUILD);
			if (dah_map.mapa[i[0]][i[1]] < -1 && dah_map.mapa[i[0]][i[1]] < infl && cc_select.getPosition().getApproxWDistance(aux) < dist) {
				//updates values
				dist = cc_select.getPosition().getApproxBDistance(aux);
				ret = aux;
				infl = dah_map.mapa[i[0]][i[1]];
			}
		}
		return ret;
	}
	
	/**
	 * Defend chokePoint/base
	 * @return true if there isn't enough units defending, false otherwise
	 */
	public boolean sendDefend(){
		for (Unit u : boredSoldiers) {
			if (u.isCompleted()) {
				if (defendGroup.units.size() < 8) {
					defendGroup.units.add(u);
				}
				if (u.getPosition().getApproxWDistance(defendGroup.destination) > 50) {
					if (u.isIdle()) {
						u.attack(defendGroup.destination, false);
					} else {
						u.attack(defendGroup.destination.makeValid(), false);					
					}
				}
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
				return u.getType() == UnitTypes.Terran_Marine && u.isCompleted();
				
			}
		};
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
		if (attackGroup.tooFar()) {
			return false;
		}
		if (attackGroup.status != 2 && attackGroup.status != 4) {
			attackGroup.status = 1;
			attackGroup.destination = objective;
			for (Unit u : attackGroup.units) {
				if (!u.isAttacking() && !u.isMoving()) {
					u.attack(objective, true);
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
//		if (attackGroup.status == 3) {
//			return false;
//		}
		attackGroup.status = 3;
		attackGroup.destination = attackGroup.units.get((int)attackGroup.units.size()/2).getPosition();
		//if too far, group units
		for (Unit u : attackGroup.units) {
			if (u.getPosition().getApproxWDistance(attackGroup.units.get((int)attackGroup.units.size()/2).getPosition()) > 50) {
				u.attack(attackGroup.units.get((int)attackGroup.units.size()/2).getPosition().makeValid(), false);					
			} else {
				u.attack(attackGroup.units.get((int)attackGroup.units.size()/2).getPosition(), false);
			}
		}
		
		return true;
	}
	
	/**
	 * Send troops to BaseLocations to locate enemies.
	 * @return true if it can send a troop, false otherwise
	 */
	public boolean sendExplorer(){
		return false;
		/*attackGroup.state = 5;
		attackGroup.destination = objective;
		for(Unit soldadito : attackGroup.units){
			if(!soldadito.isAttacking() && !soldadito.isMoving()){
				soldadito.attack(objective, false); // <----- REVISAR ESTE TRUE
			}
		}
		
		return false;*/
	}

	
	/**
	 * Building "center" it's the cc_select. It'll 4 attempts to find a valid position.
	 * In each attempt the radius will be increased.
	 * 
	 * @return true if can find a valid position, false otherwise
	 */
	public boolean findPosition(UnitType building) {
		//Special case: Refinery
		if (building == UnitTypes.Terran_Refinery) {
			return findPositionRefinery();
		}
		//Special case: Expansion
		if (building == UnitTypes.Terran_Command_Center) {
			return findPositionCC();
		}
		//Special case: Buildings away from the CP
		if (building == UnitTypes.Terran_Barracks ||
				building == UnitTypes.Terran_Factory ||
						building == UnitTypes.Terran_Starport) {
			return findPositionAwayCP(building);
		}
		//Special case: Missile turrets and bunkers
		if (building == UnitTypes.Terran_Missile_Turret || 
				building == UnitTypes.Terran_Bunker){
			return findPositionBunkerTurret(building);
		}
		//No special case: others.
		if (number_chokePoints == 1) {
			//Gets the CP
			ChokePoint cp = (ChokePoint) this.connector.getMap().getRegion(cc_select.getPosition()).getChokePoints().toArray()[0];
			Position cp_position = cp.getCenter();
			//Variables to control the direction of the building
			byte x, y;
			//If CP is on the left/right side of the CC
			x = (cc_select.getPosition().getBX() < cp_position.getBX()) ? (byte)1 : (byte)-1;
			//If CP is on the top/bottom of the CC
			y = (cc_select.getPosition().getBY() < cp_position.getBY()) ? (byte)1 : (byte)-1;
			byte [][] pruebas = {{x,0},{x,y},{0,y},{(byte)(-1*x), (byte)(-1*y)},{(byte)(-1*x), 0},{0, (byte)(-1*y)}};
			//Looks for a place to build, testing all the directions and increasing the range up to x4
			for (int i=1; i<4; i++){
				for (int j=0; j<pruebas.length; j++) {
					//Point origen, Point maximo, UnitType building
					Position pos = findPlace(new Point(cc_select.getPosition().getBX(), cc_select.getPosition().getBY()),
							new Point((cc_select.getPosition().getBX()+pruebas[j][0]*building.getTileWidth()*i),
									(cc_select.getPosition().getBY()+pruebas[j][1]*building.getTileHeight()*i)),
							building);
					//If the position is valid...
					if (this.connector.canBuildHere(pos, building, true) && 
							this.connector.getMap().isBuildable(pos) &&
							this.connector.isBuildable(pos, true)){
						posBuild = pos;
						return true;
					}				
				}
			}
		} else {
			byte [][] pruebas = {{1,0},{1,1},{0,1},{-1,0},{-1,-1},{0,-1}};
			for (int i=4; i>1; i--){
				for (int j=0; j<pruebas.length; j++) {
					//Point origen, Point maximo, UnitType building
					Position pos = findPlace(new Point(cc_select.getPosition().getBX(), cc_select.getPosition().getBY()),
							new Point((cc_select.getPosition().getBX()+pruebas[j][0]*building.getTileWidth()*i),
									(cc_select.getPosition().getBY()+pruebas[j][1]*building.getTileHeight()*i)),
									building);
					//If the position is valid...
					if (this.connector.canBuildHere(pos, building, true) && 
							this.connector.getMap().isBuildable(pos) &&
							this.connector.isBuildable(pos, true)){
						posBuild = pos;
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
			if (vespeno.getType() == UnitTypes.Resource_Vespene_Geyser &&
					this.connector.getMap().getRegion(vespeno.getPosition()) ==
					this.connector.getMap().getRegion(cc_select.getPosition())) {
				
					posBuild = vespeno.getTopLeft();
					return true;
			}
		}
		//Don't find position to build the refinery
		return false;
	}
	
	/**
	 * For build expansions.
	 * @return true if finds a position, false if not.
	 */
	public boolean findPositionCC() {
		//Selects the closer position to the original CC
		int dist = 9999;
		BaseLocation pos = null;
		for (BaseLocation aux : this.connector.getMap().getBaseLocations()) {
			//Checks to select a different BaseLocation, 
			//the distance is lower than the previous 
			//and the position is buildable.
			if (!aux.isStartLocation() &&
					cc.getPosition().getApproxWDistance(aux.getCenter()) < dist &&
					this.connector.canBuildHere(aux.getPosition(), UnitTypes.Terran_Command_Center, false) &&
					!(aux.isIsland() || aux.isMineralOnly())) {
				//If closer, updates "dist" and "pos".
				dist = cc.getPosition().getApproxWDistance(aux.getCenter());
				pos = aux;
			}
		}
		//It has found a position
		if (pos != null) {
			posBuild = pos.getPosition();
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
		//If number_chokePoints == 1, build away from the CP
		if (number_chokePoints == 1) {
			//Gets the CP
			ChokePoint cp = (ChokePoint) this.connector.getMap().getRegion(cc_select.getPosition()).getChokePoints().toArray()[0];
			Position cp_position = cp.getCenter();
			//Variables to control the direction of the building
			byte x, y;
			//If CP is on the left/right side of the CC
			x = (cc_select.getPosition().getBX() > cp_position.getBX()) ? (byte)1 : (byte)-1;
			//If CP is on the top/bottom of the CC
			y = (cc_select.getPosition().getBY() > cp_position.getBY()) ? (byte)1 : (byte)-1;
			byte [][] tests = {{0,y},{x,y},{x,0},{(byte)(-1*x), 0},{0, (byte)(-1*y)},{(byte)(-1*x), (byte)(-1*y)}};
			//Looks for a place to build, testing all the directions and increasing the range up to x4
			for (int i=0; i<4; i++){
				for (int j=0; j<tests.length; j++) {
					//Point origen, Point maximo, UnitType building
					Position pos = findPlace(new Point(cc_select.getPosition().getBX(), cc_select.getPosition().getBY()),
							new Point((cc_select.getPosition().getBX()+tests[j][0]*building.getTileWidth()*i),
									(cc_select.getPosition().getBY()+tests[j][1]*building.getTileHeight()*i)),
							building);
					//If the position is valid...
					if (this.connector.canBuildHere(pos, building, true) && 
							this.connector.getMap().isBuildable(pos) &&
							this.connector.isBuildable(pos, true)){
						posBuild = pos;
						return true;
					}				
				}
			}
			//If there's more than 1 CP, builds closer to the CC.
		} else {
			byte [][] tests = {{1,0},{1,1},{0,1},{-1,0},{-1,-1},{0,-1}};
			//Looks for a place to build, testing all the directions and increasing the range up to x4
			for (int i=1; i<4; i++){
				for (int j=0; j<tests.length; j++) {
					//Point origen, Point maximo, UnitType building
					Position pos = findPlace(new Point(cc_select.getPosition().getBX(), cc_select.getPosition().getBY()),
							new Point((cc_select.getPosition().getBX()+tests[j][0]*building.getTileWidth()*i),
									(cc_select.getPosition().getBY()+tests[j][1]*building.getTileHeight()*i)),
							building);
					//If the position is valid...
					if (this.connector.canBuildHere(pos, building, true) && 
							this.connector.getMap().isBuildable(pos) &&
							this.connector.isBuildable(pos, true)){
						posBuild = pos;
						return true;
					}				
				}
			}
		}
		return false;
	}

//	/**
//	 * 
//	 * @return
//	 */
//	public boolean findPositionTurret() {
//		byte [][] pruebas = {{1,0},{1,1},{0,1},{-1,0},{-1,-1},{0,-1}};
//		for (int i=4; i>1; i--){
//			for (int j=0; j<pruebas.length; j++) {
//				//Point origen, Point maximo, UnitType building
//				Position pos = findPlace(new Point(cc_select.getPosition().getBX(), cc_select.getPosition().getBY()),
//						new Point((cc_select.getPosition().getBX()+pruebas[j][0]*UnitTypes.Terran_Missile_Turret.getTileWidth()*i),
//								(cc_select.getPosition().getBY()+pruebas[j][1]*UnitTypes.Terran_Missile_Turret.getTileHeight()*i)),
//								UnitTypes.Terran_Missile_Turret);
//				boolean pass = false;
//				//The bunkers have to be spread.
//				for (Unit u : finishedBuildings) { 
//					if (u.getType() == UnitTypes.Terran_Missile_Turret && 
//							u.getDistance(pos) < 300) {
//						pass = true;
//					}	
//				}
//				//If the position is valid...
//				if (!pass && this.connector.canBuildHere(pos, UnitTypes.Terran_Missile_Turret, true) && 
//						this.connector.getMap().isBuildable(pos) &&
//						this.connector.isBuildable(pos, true) &&
//						dah_map.mapa[pos.getBY()][pos.getBX()] < 3){
//					posBuild = pos;
//					return true;
//				}				
//			}
//		}
//		return false;
//	}
	
	/**
	 * 
	 * @return
	 */
	public boolean findPositionBunkerTurret(UnitType building) {
		byte [][] pruebas = {{1,0},{1,1},{0,1},{-1,0},{-1,-1},{0,-1}};
		if (number_chokePoints == 1) {
			ChokePoint cp = (ChokePoint) this.connector.getMap().getRegion(cc_select.getPosition()).getChokePoints().toArray()[0];
			Position cp_position = cp.getCenter();
			for (int j=0; j<pruebas.length; j++) {
				//Point origen, Point maximo, UnitType building
				Position pos = findPlace(new Point(cp_position.getBX(), cp_position.getBY()),
										new Point((cp_position.getBX()+pruebas[j][0]*building.getTileWidth()),
												(cp_position.getBY()+pruebas[j][1]*building.getTileHeight())),
										building);
				boolean pass = false;
				//The bunkers/turrets have to be spread.
				for (Unit u : finishedBuildings) { 
					if (u.getType() == building && 
							u.getDistance(pos) < 300) {
						pass = true;
					}	
				}
				//If the position is valid...
				if (!pass && this.connector.canBuildHere(pos, building, true) && 
						this.connector.getMap().isBuildable(pos) &&
						this.connector.isBuildable(pos, true) &&
						dah_map.mapa[pos.getBY()][pos.getBX()] < 3){
					posBuild = pos;
					return true;
				}				
			}
		} else {
			for (int i=4; i>1; i--){
				for (int j=0; j<pruebas.length; j++) {
					//Point origen, Point maximo, UnitType building
					Position pos = findPlace(new Point(cc_select.getPosition().getBX(), cc_select.getPosition().getBY()),
											new Point((cc_select.getPosition().getBX()+pruebas[j][0]*building.getTileWidth()*i),
													(cc_select.getPosition().getBY()+pruebas[j][1]*building.getTileHeight()*i)),
											building);
					boolean pass = false;
					//The bunkers/turrets have to be spread.
					for (Unit u : finishedBuildings) { 
						if (u.getType() == building && 
								u.getDistance(pos) < 300) {
							pass = true;
						}	
					}
					//If the position is valid...
					if (!pass && this.connector.canBuildHere(pos, building, true) && 
							this.connector.getMap().isBuildable(pos) &&
							this.connector.isBuildable(pos, true) &&
							dah_map.mapa[pos.getBY()][pos.getBX()] < 3){
						posBuild = pos;
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
		this.dah_map.updateMap(this.connector);
	}
	
	/**
	 * Checks if there is any damaged building
	 * @return true if there's at least 1 building, false otherwise.
	 */
	public boolean checkBuildings() {
		//To save time, if the list isn't empty, return true
		if (!damageBuildings.isEmpty()) {
			return true;
		}
		//If the list it's empty, look for damaged building
		for (Unit u : finishedBuildings){
			//If the building it's damaged, not being repaired and isn't in the damageBuildings list
			if ((u.getHitPoints() - u.getType().getMaxHitPoints() != 0) &&
					!u.isRepairing() && !damageBuildings.contains(u)) {
				damageBuildings.add(u);
			}
		}
		
		if (damageBuildings.isEmpty()) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sends a SCV to repair a building
	 * @return true if can, false otherwise
	 */
	public boolean repair() {
		for (Unit vce : workers){
			if (vce.isIdle()) {
				boolean ret = vce.repair(damageBuildings.get(0), false);
				//If it's going to be repaired, removes it from the list
				if (ret)
					damageBuildings.remove(0);
				return ret;
			}
		}
		return false;
	}
	
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
		int maxHeight = this.connector.getMap().getSize().getBY();
		//Anchura máxima del mapa en pixeles (Build)
		int maxWidth = this.connector.getMap().getSize().getBX();
		//Mapa a generar
		map = new int[maxHeight][maxWidth];
		//Tamaño máximo del edificio que se puede construir
		int dimension;
		//Variable que detiene la búsqueda si se encuentra un obstáculo
		//boolean zonaLibre;

		for(int f = 0; f < maxHeight; f++){
			for(int c = 0; c < maxWidth; c++){
				//Posición a evaluar
				Position pos_aux = new Position(c, f, PosType.BUILD);
				//Tamaño máximo posible a construir
				dimension = 0;
				//Variable que detiene la búsqueda si se encuentra un obstáculo
				boolean zonaLibre = true;
				
				//para cada posición se mira si se está en los límites del mapa y
				//hay que verificar si la posición es construible
				if (f+1>=maxHeight || c+1>=maxWidth ||
					!this.connector.getMap().isBuildable(pos_aux)){ zonaLibre = false; }
				
				//Se obtiene la altura de la posición
				int altura = this.connector.getMap().getGroundHeight(pos_aux);
				while(zonaLibre && dimension <= 4){
					dimension++;
					//Se verifica vertical, horizontal y diagonalmente si son válidas las posiciones.
					//Si alguna no lo es, se sale del while y se guarda el valor en el mapa
					for(int i = 0; i < dimension; i++){
						//matriz[i+f][c+dimension]	Comprueba columnas
						if (this.connector.isBuildable(new Position(c+dimension, f+i, PosType.BUILD), true)){ // ¿Es construible?
							if(this.connector.getMap().getGroundHeight(new Position(c+dimension, f+i, PosType.BUILD)) != altura){ // ¿Están a diferente altura?
								zonaLibre = false;
							}
						}
						else{ zonaLibre = false; }
						
						//matriz[f+dimension][i+c]    Comprueba filas
						if (this.connector.isBuildable(new Position(c+i, f+dimension, PosType.BUILD), true)) {
							if(this.connector.getMap().getGroundHeight(new Position(c+i, f+dimension, PosType.BUILD)) != altura){
								zonaLibre = false;
							}
						}
						else{ zonaLibre = false; }
					
						//matriz[f+dimension][c+dimension]   Comprueba en la diagonal (se podr�a cambiar cambiando la condicion del for a <=)
						if (this.connector.isBuildable(new Position(c+dimension, f+dimension, PosType.BUILD), true)) {
							if(this.connector.getMap().getGroundHeight(new Position(c+dimension, f+dimension, PosType.BUILD)) != altura){
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
			if (u.getType() == UnitTypes.Resource_Mineral_Field ||
					u.getType() == UnitTypes.Resource_Mineral_Field_Type_2 ||
					u.getType() == UnitTypes.Resource_Mineral_Field_Type_3) {
				//para recolectar minerales vale con que el vce vaya a cualquiera de sus casillas.
				map[u.getTilePosition().getBY()][u.getTilePosition().getBX()] = -1;
			}
			if (u.getType() == UnitTypes.Resource_Vespene_Geyser) {
				//Para construir la refinería nos vale la casilla arriba a la izquierda.
				map[u.getTilePosition().getBY()][u.getTilePosition().getBX()] = -2;
			}
		}
	}
    
    /**
     * Para poder crear la matriz deben ser en diagonal.
     * Para contemplar casos en el que origen y máximo sean en horizontal (mismo Y)
     * cuando ocurra eso, se toma Y como la Y del edificio. 
     * 
     * El método devolverá un Position que indica la casilla superior izquierda v�lida donde construir el edificio.
     * Si se devuelve -1 en X no hay posición válida.
     */
    public Position findPlace(Point origen, Point maximo, UnitType building){
    	//Si no se pasan valores correctos, se devuelve posición inválida
    	if (origen.x < 0 || origen.y < 0 ||
    			maximo.x < 0 || maximo.y < 0) {
    		return new Position(-2,0,PosType.BUILD);
    	}
    	
    	int xMaximo, xOrigen, yOrigen, yMaximo;
    	
    	//Se considera que sea misma fila o columna.
    	if (origen.x == maximo.x && maximo.x + building.getTileWidth() < map[0].length) {
    		maximo.x += building.getTileWidth();
    	}
    	if (origen.y == maximo.y && maximo.y + building.getTileHeight() < map.length) {
    		maximo.y += building.getTileHeight();
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
    	int max = (building.getTileHeight() > building.getTileWidth()) ? building.getTileHeight() : building.getTileWidth();
    	//Variable de control para la búsqueda
    	boolean found = false;
    	//Se recorre el mapa entre las posiciones dadas
    	/******************************************
    	 * **************************************
    	 */
    	for (int y = yOrigen; y < yMaximo && !found; y++){
    		for (int x = xOrigen; x < xMaximo && !found; x++){
    			//si encuentra una posición válida sale.
    			if (map[y][x] >= max) {return new Position(x, y, PosType.BUILD);}
    			//Este bucle busca en vertical hacia abajo
    			for (int y2=0; y2<=(x-xOrigen) && y+y2 < map.length;y2++) {
    				if (map[y+y2][x] >= max) {
    					return new Position(x, y+y2, PosType.BUILD);
    				}
    			}
    			//Este bucle mira desde la y que se quedó el bucle anterior y va horizontalmente hacia atrás
    			for (int x2=0; x2<=(x-xOrigen) && y+(x-xOrigen) < map.length;x2++) {
    				if (map[y+(x-xOrigen)][x-x2] >= max) {
    					return new Position(x-x2, y+(x-xOrigen), PosType.BUILD);
    				}
    			}
    			/*No hace falta mirar exclusivamente la diagonal 
    			 * porque se mira mediante las dos comprobaciones anteriores
    			 * ya que se va aumentando poco a poco el área
    			 */
    		}
    	}
    	
    	//Si llega aquí no ha encontrado nada
		return new Position(-1,0, PosType.BUILD);
    }
    
    /**
     * Se da por supuesto que las posiciones indicadas son posiciones correctas.
     * La posición origen ha sido obtenida mediante el m�todo findPlace y la posición
     * destino ha sido calculada con el tama�o del edificio + la posición origen
     */
    public void updateMap(Position origen, Position destino) {
    	if (destino.getBX() > map[0].length) {
    		if (destino.getBY() > map.length) {
    			destino = new Position(map[0].length-1,map.length-1);
    		} else {
    			destino = new Position(map[0].length-1,destino.getBY());
    		}
    	}
    	//se recorre la matriz entre las posiciones dadas
    	for (int i = origen.getBY(); i < destino.getBY(); i++){
    		for(int j = origen.getBX(); j < destino.getBX(); j++){
    			//se ponen como ocupadas las casillas
    			map[i][j] = 0;
    		}
    	}
    	/*
    	 *  Para actualizar el resto de la matriz, tendremos que explorar las casillas superiores y por la izquierda.
    	 *  Dado que también hay que tener en cuenta las diagonales, se hará de tal forma que primero se actualicen
    	 *  todas las superiores incluidas las diagonales y despu�s las de la izquierda. 
    	 */
    	
    	// Esta variable se usará para saber si hemos terminado la actualización
    	boolean parada = true;
    	// Esta variable servirá para desplazarnos verticalmente y tambien saber que dimensión maxima puede tener el edificio de esa casilla
    	int iv = 1;
    	// Esta variable servirá para desplazarnos horizontalmente
    	int ih = 0;
    	
    	// Bucle de actualización vertical
    	while (parada){
    		int extra = (destino.getBX()-origen.getBX() <= ih ? ih-(destino.getBX()-origen.getBX()) : 0);
    		//Si no nos salimos del mapa, el valor actual de la dimensión no es 4 (máximo)
    		if (((origen.getBY()-iv >= 0 && destino.getBX()-ih >= 0) && map[origen.getBY()-iv][destino.getBX()-ih] > iv) && (iv+extra < 4)){ // Si llegamos a 4 no es necesario seguir
    			map[origen.getBY()-iv][destino.getBX()-ih] = (iv == 1 ? iv+extra : iv);
    			iv++;
    		}
    		else{ // Hemos terminado con la columna, pasamos a la siguiente (hacia atrás en el mapa)
    			if (iv == 1){
    				parada = false; // Si en la primera casilla no hay que actualizar, significa que hemos terminado.
    			}
    			else{
    				ih++;
        			iv = 1;
    			}
    		}
    	}
    	
    	ih = 1;
    	iv = 0;
  
    	parada = true;
    	// Bucle horizontal
    	while (parada){
    		if (((origen.getBY()+iv >= 0 && origen.getBX()-ih >= 0) && map[origen.getBY()+iv][origen.getBX()-ih] > ih) && (ih < 4)){ // Si llegamos a 4 no es necesario seguir
    			map[origen.getBY()+iv][origen.getBX()-ih] = ih;
    			ih++;
    		}
    		else{ // Hemos terminado con la fila, pasamos a la siguiente (hacia abajo en el mapa)
    			if (ih == 1 || origen.getBY()+iv == destino.getBY()){
    				parada = false; // Si en la primera casilla no hay que actualizar, significa que hemos terminado.
    			}
    			else{
    				iv++;
        			ih = 1;
    			}
    		}
    	}	
    }

}
