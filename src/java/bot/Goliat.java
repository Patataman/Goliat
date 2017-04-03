package bot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Predicate;

import org.iaie.Agent;
import org.iaie.btree.BehavioralTree;
import org.iaie.btree.task.composite.Selector;
import org.iaie.btree.task.composite.Sequence;
import org.iaie.btree.util.GameHandler;
import org.iaie.tools.Options;

import jnibwapi.BWAPIEventListener;
import jnibwapi.ChokePoint;
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.Position.PosType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType.UpgradeTypes;
import jnibwapi.util.BWColor;

public class Goliat extends Agent implements BWAPIEventListener {

	BehavioralTree CollectTree, BuildTree, TrainTree, AttackTree, DefenseTree, UpdateTroopsTree;
	Unit buildingTree;
	JohnDoe gh;
	int frames;
	
	public Goliat() {            

        // Generación del objeto de tipo agente

        // Creación de la superclase Agent de la que extiende el agente, en este método se cargan            
        // ciertas variables de de control referentes a los parámetros que han sido introducidos 
        // por teclado. 
        super();
        // Creación de una instancia del connector JNIBWAPI. Esta instancia sólo puede ser creada
        // una vez ya que ha sido desarrollada mediante la utilización del patrón de diseño singlenton.
        this.bwapi = new JNIBWAPI(this, true);
        // Inicia la conexión en modo cliente con el servidor BWAPI que está conectado directamente al videojuego.
        // Este proceso crea una conexión mediante el uso de socket TCP con el servidor. 
        this.bwapi.start();
    }
	
	public void connected() {}

	public void matchStart() {
		 
        // Mediante este metodo se puede obtener información del usuario. 
        if (Options.getInstance().getUserInput()) this.bwapi.enableUserInput();

        if (Options.getInstance().getInformation()) this.bwapi.enablePerfectInformation();
        // Mediante este método se define la velocidad de ejecución del videojuego. 
        // Los valores posibles van desde 0 (velocidad estándar) a 10 (velocidad máxima).
        this.bwapi.setGameSpeed(Options.getInstance().getSpeed());
        
        this.bwapi.drawTargets(true);
		
		gh = new JohnDoe(bwapi);
		
		//Se establece la variable del centro de mando,
		//ya que se va a usar bastante y así evitamos recorrer
		//la lista de myUnits
		for (Unit cc : bwapi.getMyUnits()){
			if (cc.getType() == UnitTypes.Terran_Command_Center){
				gh.cc = cc;
				gh.cc_select = cc;
				gh.CCs.add(cc.getID());
				gh.addCC(0);
				gh.finishedBuildings.add(cc);
			}
		}

		if (gh.number_chokePoints == 1) {
			for (ChokePoint cp : bwapi.getMap().getRegion(gh.cc_select.getPosition()).getChokePoints()){
				gh.defendGroup.destination = cp.getCenter();				
			}
		} else {
			gh.defendGroup.destination = gh.cc.getPosition().makeValid();
		}
		
		bwapi.drawCircle(gh.defendGroup.destination, 10, BWColor.Blue, true, true);
		gh.supplies = bwapi.getSelf().getSupplyUsed();
		gh.totalSupplies = bwapi.getSelf().getSupplyTotal();
		
		gh.createMap();
		
		frames = 0;
		
		
		Selector<GameHandler> CollectResources = new Selector<>("Minerals or Vespin gas");
		CollectResources.addChild(new CollectGas("Vespin gas", gh));
		CollectResources.addChild(new CollectMineral("Minerals", gh));
		
		Sequence collect = new Sequence("Gather");
		collect.addChild(new FreeWorker("Free Worker", gh));
		collect.addChild(CollectResources);
		
		// -------- Training sequences ---------
		//Train SCVs
		Sequence TrainVCE = new Sequence("Train SCV");
		TrainVCE.addChild(new CheckResources("Check resources SCV", gh, UnitTypes.Terran_SCV));
		TrainVCE.addChild(new ChooseBuilding("Check training SCV", gh, UnitTypes.Terran_SCV));
		TrainVCE.addChild(new TrainUnit("Train SCV", gh, UnitTypes.Terran_SCV, UnitTypes.Terran_Command_Center));
		//Train marines
		Sequence TrainMarine = new Sequence("Train Marine");
		TrainMarine.addChild(new CheckResources("Check resources marine", gh, UnitTypes.Terran_Marine));
		TrainMarine.addChild(new ChooseBuilding("Check training marine", gh, UnitTypes.Terran_Marine));
		TrainMarine.addChild(new TrainUnit("Train marine", gh, UnitTypes.Terran_Marine, UnitTypes.Terran_Barracks));
		//Train medics
		Sequence TrainMedic = new Sequence("Train medic");
		TrainMedic.addChild(new CheckResources("Check resources medic", gh, UnitTypes.Terran_Medic));
		TrainMedic.addChild(new ChooseBuilding("Check training medic", gh, UnitTypes.Terran_Medic));
		TrainMedic.addChild(new TrainUnit("Train medic", gh, UnitTypes.Terran_Medic, UnitTypes.Terran_Barracks));
		//Train firebats
		Sequence TrainFirebat = new Sequence("Train Firebat");
		TrainFirebat.addChild(new CheckResources("Check resources firebat", gh, UnitTypes.Terran_Firebat));
		TrainFirebat.addChild(new ChooseBuilding("Check training firebat", gh, UnitTypes.Terran_Firebat));
		TrainFirebat.addChild(new TrainUnit("Train Firebat", gh, UnitTypes.Terran_Firebat, UnitTypes.Terran_Barracks));
		//Train siege tanks
		/*Sequence TrainSiegeTank = new Sequence("Train Siege Tank");
		TrainSiegeTank.addChild(new CheckResources("Check resources siege tank", gh, UnitTypes.Terran_Siege_Tank_Tank_Mode));
		TrainSiegeTank.addChild(new ChooseBuilding("Check training siege tank", gh, UnitTypes.Terran_Siege_Tank_Tank_Mode));
		TrainSiegeTank.addChild(new TrainUnit("Train siege tank", gh, UnitTypes.Terran_Siege_Tank_Tank_Mode, UnitTypes.Terran_Siege_Tank_Tank_Mode));*/
		//Train goliats
		Sequence TrainGoliat = new Sequence("Train Goliat");
		TrainGoliat.addChild(new CheckResources("Check resources goliat", gh, UnitTypes.Terran_Goliath));
		TrainGoliat.addChild(new ChooseBuilding("Check training goliat", gh, UnitTypes.Terran_Goliath));
		TrainGoliat.addChild(new TrainUnit("Train goliat", gh, UnitTypes.Terran_Goliath, UnitTypes.Terran_Factory));
//		//Train science vessels
//		Sequence TrainVessel = new Sequence(Train science vessel");
//		TrainVessel.addChild(new CheckResources("Check resources science vessel", gh, UnitTypes.Terran_Science_Vessel));
//		TrainVessel.addChild(new ChooseBuilding("Check training science vessels", gh, UnitTypes.Terran_Science_Vessel));
//		TrainVessel.addChild(new TrainUnit("Train Science vessel nave cientifica", gh, UnitTypes.Terran_Science_Vessel, UnitTypes.Terran_Starport));
		//Selector con todos los posibles entrenamientos
		Selector<Sequence> selectorTrain = new Selector<>("Selector train", TrainVCE, TrainGoliat, TrainMedic, TrainFirebat, TrainMarine);
		// ----------- END TRAIN ---------

		
		// -------- Building sequences ---------
		//Build supplies
		Sequence buildSupply = new Sequence("Build supplies");
		buildSupply.addChild(new CheckResources("Check resources supplies", gh, UnitTypes.Terran_Supply_Depot));
		buildSupply.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Supply_Depot));
		buildSupply.addChild(new FreeBuilder("Find builder", gh));
		buildSupply.addChild(new MoveTo("Move builder", gh));
		buildSupply.addChild(new Build("Build supplies", gh, UnitTypes.Terran_Supply_Depot));
		//Build barracks
		Sequence buildBarracks = new Sequence("Build barracks");
		buildBarracks.addChild(new CheckResources("Check resources barracks", gh, UnitTypes.Terran_Barracks));
		buildBarracks.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Barracks));
		buildBarracks.addChild(new FreeBuilder("Find builder", gh));
		buildBarracks.addChild(new MoveTo("Move builder", gh));
		buildBarracks.addChild(new Build("Build barracks", gh, UnitTypes.Terran_Barracks));
		//Build refinery
		Sequence buildRefinery = new Sequence("Build refinery");
		buildRefinery.addChild(new CheckResources("Check resources refinery", gh, UnitTypes.Terran_Refinery));
		buildRefinery.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Refinery));
		buildRefinery.addChild(new FreeBuilder("Find builder", gh));
		buildRefinery.addChild(new MoveTo("Move builder", gh));
		buildRefinery.addChild(new Build("Build refinery", gh, UnitTypes.Terran_Refinery));
		//Build bay
		Sequence buildBay = new Sequence("Build bay");
		buildBay.addChild(new CheckResources("Check resources bay", gh, UnitTypes.Terran_Engineering_Bay));
		buildBay.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Engineering_Bay));
		buildBay.addChild(new FreeBuilder("Find builder", gh));
		buildBay.addChild(new MoveTo("Move builder", gh));
		buildBay.addChild(new Build("Build bay", gh, UnitTypes.Terran_Engineering_Bay));
		//Build academy
		Sequence buildAcademy = new Sequence("Build academy");
		buildAcademy.addChild(new CheckResources("Check resources academy", gh, UnitTypes.Terran_Academy));
		buildAcademy.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Academy));
		buildAcademy.addChild(new FreeBuilder("Find builder", gh));
		buildAcademy.addChild(new MoveTo("Move builder", gh));
		buildAcademy.addChild(new Build("Build academy", gh, UnitTypes.Terran_Academy));
		//Build factory
		Sequence buildFactory = new Sequence("Build factory");
		buildFactory.addChild(new CheckResources("Check resources factory", gh, UnitTypes.Terran_Factory));
		buildFactory.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Factory));
		buildFactory.addChild(new FreeBuilder("Find builder", gh));
		buildFactory.addChild(new MoveTo("Move builder", gh));
		buildFactory.addChild(new Build("Build factory", gh, UnitTypes.Terran_Factory));
		//Build armory
		Sequence buildArmory = new Sequence("Build armory");
		buildArmory.addChild(new CheckResources("Check resources armory", gh, UnitTypes.Terran_Armory));
		buildArmory.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Armory));
		buildArmory.addChild(new FreeBuilder("Find builder", gh));
		buildArmory.addChild(new MoveTo("Move builder", gh));
		buildArmory.addChild(new Build("Build armory", gh, UnitTypes.Terran_Armory));
		//Build missile turret
		Sequence buildTurret = new Sequence("Build missile turret");
		buildTurret.addChild(new CheckResources("Check resources missile turret", gh, UnitTypes.Terran_Missile_Turret));
		buildTurret.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Missile_Turret));
		buildTurret.addChild(new FreeBuilder("Find builder", gh));
		buildTurret.addChild(new MoveTo("Move builder", gh));
		buildTurret.addChild(new Build("Build missile turret", gh, UnitTypes.Terran_Missile_Turret));
		//Build CC
		Sequence buildCC = new Sequence("Build CC");
		buildCC.addChild(new CheckResources("Check resources CC", gh, UnitTypes.Terran_Command_Center));
		buildCC.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Command_Center));
		buildCC.addChild(new FreeBuilder("Find builder", gh));
		buildCC.addChild(new MoveTo("Move builder", gh));
		buildCC.addChild(new Build("Build CC", gh, UnitTypes.Terran_Command_Center));
//		//Build starport
//		Sequence buildStarport = new Sequence("Build starport");
//		buildStarport.addChild(new CheckResources("Check resources starport", gh, UnitTypes.Terran_Starport));
//		buildStarport.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Starport));
//		buildStarport.addChild(new FreeBuilder("Find builder", gh));
//		buildStarport.addChild(new MoveTo("Move builder", gh));
//		buildStarport.addChild(new Build("Build starport", gh, UnitTypes.Terran_Starport));
//		//Build science facility
//		Sequence buildLab = new Sequence("Build science facility");
//		buildLab.addChild(new CheckResources("Check resources facility", gh, UnitTypes.Terran_Science_Facility));
//		buildLab.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Science_Facility));
//		buildLab.addChild(new FreeBuilder("Find builder", gh));
//		buildLab.addChild(new MoveTo("Move builder", gh));
//		buildLab.addChild(new Build("Build facility", gh, UnitTypes.Terran_Science_Facility));
		
		Selector<Sequence> selectorBuild = new Selector<>("Selector build", buildSupply, buildBarracks, 
													buildRefinery, buildAcademy, buildFactory, buildBay, buildArmory, buildTurret, buildCC);
		// ---------- END BUILD -----------
		
		// ---------- Compact troops
		Sequence compactTroops = new Sequence("Redistribuite units in troops");
		compactTroops.addChild(new RedistribuiteTroops("Redistribuite units in troops", gh));
		// ----------- END compact troops
		
		//---------- Troops creation
		Sequence createTroop = new Sequence("Make attack troop");
		createTroop.addChild(new CheckStateTroops("Check troops status", gh));
		createTroop.addChild(new CheckStateUnits("Check units status", gh));
		createTroop.addChild(new CreateTroop("Make troop", gh));
		// -------- END createGroup ---------------
		
		// -------- Attack sequence ---------
		Sequence attack = new Sequence("Send to attack the troops");
		attack.addChild(new CheckStateTroops("Check troops status", gh));
		attack.addChild(new SelectGroup("Select one troop to attack", gh));
		attack.addChild(new ChooseDestination("Choose destination", gh));
		Selector<GameHandler> attackSelector = new Selector<>("Attack/Explore");
		attackSelector.addChild(new SendAttack("Send to attack", gh));
		attackSelector.addChild(new SendRegroup("Regroup", gh));		
		attackSelector.addChild(new SendExplorer("Send to explore", gh));
		attack.addChild(attackSelector);
		// ---------- FIN ATTACK -----------
		
		// --------- Defend sequence ---------
		Sequence defendBase = new Sequence("Defend base");
		defendBase.addChild(new SendDefend("Send to defend", gh));
		// --------- FIN DEFENSE -----------
		
		// ---------- Research sequence --------
		//Research U238 (Academy)
		Sequence u238 = new Sequence("Research U238");
		u238.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeTypes.U_238_Shells));
		u238.addChild(new Research("Research", gh, UnitTypes.Terran_Academy, UpgradeTypes.U_238_Shells));
		//Research Caduceus reactor (Academy, for medics)
		Sequence caudecus = new Sequence("Research caduceus");
		caudecus.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeTypes.Caduceus_Reactor));
		caudecus.addChild(new Research("Research", gh, UnitTypes.Terran_Academy, UpgradeTypes.Caduceus_Reactor));
		//Research infantry armor (Bay)
		Sequence infantryArmor = new Sequence("Research infantry armor");
		infantryArmor.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeTypes.Terran_Infantry_Armor));
		infantryArmor.addChild(new Research("Research", gh, UnitTypes.Terran_Engineering_Bay, UpgradeTypes.Terran_Infantry_Armor));
		//Research infantry weapons (Bay)
		Sequence infantryWeapons = new Sequence("Research infantry weapons");
		infantryWeapons.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeTypes.Terran_Infantry_Weapons));
		infantryWeapons.addChild(new Research("Research", gh, UnitTypes.Terran_Engineering_Bay, UpgradeTypes.Terran_Infantry_Weapons));
		//Research vehicle armor (Armory)
		Sequence vehicleArmor = new Sequence("Research vehicle armor");
		vehicleArmor.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeTypes.Terran_Vehicle_Plating));
		vehicleArmor.addChild(new Research("Research", gh, UnitTypes.Terran_Armory, UpgradeTypes.Terran_Vehicle_Plating));
		//Research vehicle weapons (Armory)
		Sequence vehicleWeapons = new Sequence("Research vehicle weapons");
		vehicleWeapons.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeTypes.Terran_Vehicle_Weapons));
		vehicleWeapons.addChild(new Research("Research", gh, UnitTypes.Terran_Armory, UpgradeTypes.Terran_Vehicle_Weapons));
		
		Selector<Sequence> selectorResearch = new Selector<>("Selector research", u238, caudecus, infantryArmor, infantryWeapons, vehicleArmor, vehicleWeapons); 
		// --------------- FIN RESEARCH ---------------
		
		// ----------- Repair sequence ---------		
		Sequence repair = new Sequence("Repair");
		repair.addChild(new FindDamageBuildings("Find damaged buildings", gh));
		repair.addChild(new FreeBuilder("Free worker", gh));
		repair.addChild(new RepairBuilding("Repair building", gh));
		// ------------- END REPAIR --------------------
		
		CollectTree = new BehavioralTree("Gather/Repair tree");
		CollectTree.addChild(new Selector<>("MAIN SELECTOR", collect, repair));
		BuildTree  = new BehavioralTree("Build/Research tree");
		BuildTree.addChild(new Selector<>("MAIN SELECTOR", selectorBuild, selectorResearch));
		TrainTree  = new BehavioralTree("Training tree");
		TrainTree.addChild(new Selector<>("MAIN SELECTOR", selectorTrain));
		UpdateTroopsTree = new BehavioralTree("Update/Check troops status");
		UpdateTroopsTree.addChild(new Selector<>("MAIN SELECTOR", compactTroops, createTroop));
		DefenseTree  = new BehavioralTree("Defense tree");
		DefenseTree.addChild(new Selector<>("MAIN SELECTOR", defendBase));
//		DefenseTree.addChild(new Selector<>("MAIN SELECTOR", sendToBunker, defenseBase));
		AttackTree  = new BehavioralTree("Attack tree");
		AttackTree.addChild(new Selector<>("MAIN SELECTOR", attack));
		
		bwapi.sendText("gl hf");
		
	}

	@Override
	public void matchFrame() {
		CollectTree.run();
		BuildTree.run();
		TrainTree.run();
		DefenseTree.run();
		UpdateTroopsTree.run();
		AttackTree.run();
		
		if(frames < 150){ // Cada 200 frames se recalculan las influencias.
			frames++;
		}else{
			frames = 0;
			gh.updateInfluences();
		}
		
	}


	public void unitCreate(int unitID) {
		//Cuando se comienza a construir un edificio se pone como pendiente.
		if (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) {
			if (bwapi.getUnit(unitID).getType().isBuilding()){
				gh.remainingBuildings.add(bwapi.getUnit(unitID).getType());
			}
		}
	}
	

	public void unitDestroy(int unitID) {
		gh.dah_map.removeUnitDead(unitID);
		Predicate<Unit> predicado = new Predicate<Unit>() {
			public boolean test(Unit u) {
				return u.getID() == unitID;
				
			}
		};
		int control = 0;
		if (control == 0) {
			//Casting a array de unidades (?)
			for(Object u : gh.finishedBuildings.stream().filter(predicado).toArray()) {
				//No es necesario comprobar el ID ya que la sublista que se recorre es la que cumple lo del ID
				//Aunque sólo debería haber 1 elemento
				gh.finishedBuildings.remove(u);
				if (((Unit) u).getType() == UnitTypes.Terran_Academy) gh.academy--;
				if (((Unit) u).getType() == UnitTypes.Terran_Barracks) gh.barracks--;
				if (((Unit) u).getType() == UnitTypes.Terran_Factory) gh.factory--;
				if (((Unit) u).getType() == UnitTypes.Terran_Engineering_Bay) gh.bay--;
				if (((Unit) u).getType() == UnitTypes.Terran_Armory) gh.armory--;
				if (((Unit) u).getType() == UnitTypes.Terran_Refinery) gh.refinery--;
				if (((Unit) u).getType() == UnitTypes.Terran_Science_Facility) gh.lab_cient--;
				if (((Unit) u).getType() == UnitTypes.Terran_Supply_Depot) {
					gh.totalSupplies -= UnitTypes.Terran_Supply_Depot.getSupplyProvided();
				}
				if (((Unit) u).getType() == UnitTypes.Terran_Command_Center) {
					gh.CCs.remove((Integer) unitID);
				}
				control++;
			}
		}
		if (control == 0) {
			for(Object u : gh.militaryUnits.stream().filter(predicado).toArray()) {
				gh.supplies -= ((Unit) u).getType().getSupplyRequired();
				gh.militaryUnits.remove((Unit) u);
				control++;
			}
			for(Object u : gh.boredSoldiers.stream().filter(predicado).toArray()) {
				gh.boredSoldiers.remove((Unit) u);
				control++;
			}
			for (Troop tropa: gh.assaultTroop){
				for(Object u : tropa.units.stream().filter(predicado).toArray()) {
					tropa.units.remove((Unit) u);
					control++;
				}
			}
			for (Object u : gh.attackGroup.units.stream().filter(predicado).toArray()) {
				gh.attackGroup.units.remove((Unit) u);
				control++;
			}
			for (Object u : gh.defendGroup.units.stream().filter(predicado).toArray()) {
				gh.defendGroup.units.remove((Unit) u);
				control++;
			}
		}
		if (control == 0) {
			for(ArrayList<Unit> vces_cc : gh.VCEs){
				for(Object u : vces_cc.stream().filter(predicado).toArray()) {
					gh.VCEs.get(gh.VCEs.indexOf(vces_cc)).remove((Unit) u);
					gh.supplies -= ((Unit) u).getType().getSupplyRequired();
					//Aqui no se hace control++ porque al ser VCE puede estar en las siguientes listas
				}
			}
		}
		
		if (control == 0) {
			for(Object u : gh.workers.stream().filter(predicado).toArray()) {
				gh.workers.remove((Unit) u);
			}
		}

		if (control == 0) {
			for(ArrayList<Integer> minerales : gh.workersMineral) {
				if (minerales.contains((Integer) unitID)) {
					gh.workersMineral.get(gh.workersMineral.indexOf(minerales)).remove((Integer) unitID);
					control++;
				}
			}
		}
		if (control == 0) {
			for(ArrayList<Integer> vespeno : gh.workersVespin) {
				if (vespeno.contains((Integer) unitID)) {
					gh.workersVespin.get(gh.workersVespin.indexOf(vespeno)).remove((Integer) unitID);
					//Ultimo if, no hay necesidad de control++
				}
			}
		}
	}
	
	public void unitComplete(int unitID) {
		//Se actualiza el mapa de ingluencias
		int influencia = (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) ? 1 : -1;
		gh.dah_map.newUnit(this.bwapi.getUnit(unitID), influencia, 
				this.bwapi.getUnit(unitID).getPosition().getBX(), this.bwapi.getUnit(unitID).getPosition().getBY());
		/////////////////////////////////////
		
//		Sección de código para escribir en un fichero el mapa y verificar que se crea bien.
//		String workingDirectory = System.getProperty("user.dir");
//		String path = workingDirectory + File.separator + "mapaInfluencia.txt";
//		createANDwriteInfluencia(path);
		
		//Se actualizan la cosa nostra
		if (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) {
			//Cuando se finaliza la unidad correspondiente, se agrega a su lista.
			if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_SCV){
				gh.VCEs.get(gh.CCs.indexOf(gh.cc_select.getID())).add(bwapi.getUnit(unitID));
			}
			
			//Cuando se cree una unidad de las pendientes, se elimina de la lista.
			if (gh.remainingUnits.contains(bwapi.getUnit(unitID).getType())){
				gh.remainingUnits.remove(bwapi.getUnit(unitID).getType());
				gh.supplies += bwapi.getUnit(unitID).getType().getSupplyRequired();
				//Los terran sólo poseen 1 unidad no militar, los VCEs.
				if (bwapi.getUnit(unitID).getType() != UnitTypes.Terran_SCV) {
					gh.militaryUnits.add(bwapi.getUnit(unitID));
					gh.boredSoldiers.add(bwapi.getUnit(unitID));
				}
			}
			//Cuando se cree un edificio pendiente, se elimina de la lista y se pone como construido
			if (gh.remainingBuildings.contains(bwapi.getUnit(unitID).getType())) {
				gh.remainingBuildings.remove(bwapi.getUnit(unitID).getType());
				gh.finishedBuildings.add(bwapi.getUnit(unitID));
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Supply_Depot)
					gh.totalSupplies += UnitTypes.Terran_Supply_Depot.getSupplyProvided();
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Barracks) gh.barracks++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Engineering_Bay) gh.bay++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Academy) gh.academy++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Factory) gh.factory++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Armory) gh.armory++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Science_Facility) gh.lab_cient++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Command_Center) {
					if (gh.CCs.indexOf((Integer) unitID) == -1){
						gh.CCs.add(unitID);
						gh.addCC(gh.CCs.indexOf(unitID));
					}
				}
				
				//Se actualiza el mapa.
				gh.updateMap(bwapi.getUnit(unitID).getTopLeft(),
						new Position(bwapi.getUnit(unitID).getTopLeft().getBX()+bwapi.getUnit(unitID).getType().getTileWidth(),
									bwapi.getUnit(unitID).getTopLeft().getBY()+bwapi.getUnit(unitID).getType().getTileHeight(),
									PosType.BUILD));
				
/*				//Sección de código para escribir en un fichero el mapa y verificar que se crea bien.
				String workingDirectory = System.getProperty("user.dir");
				String path = workingDirectory + File.separator + "mapa.txt";
				createANDwrite(path);*/
			}
		}
	}

	public void unitMorph(int unitID) {
		if (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) {
			if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Refinery) gh.refinery++;
		}
	}
	
	public void playerDropped(int playerID) { }
	
	public void matchEnd(boolean winner) {
		if (winner) {
			bwapi.sendText("GG EZ");
		} else {
			bwapi.sendText("n00b learn 2 play");			
		}
	}
	public void keyPressed(int keyCode) { }
	public void sendText(String text) { }
	public void receiveText(String text) { }
	public void playerLeft(int playerID) { }
	public void nukeDetect(Position p) { }
	public void nukeDetect() { }
	public void unitDiscover(int unitID) { }
	public void unitEvade(int unitID) { }
	public void unitShow(int unitID) { }
	public void unitHide(int unitID) { }
	public void unitRenegade(int unitID) { }
	public void saveGame(String gameName) {	}
	
	/**
	 * Método que crea un archivo nuevo,
	 * si ya existía lo resetea y escribe en él
	 * @param path: ruta donde se localiza el archivo
	 * @param texto: texto a escribir
	 * @return 0 -> Correcto; 1 -> Error
	 * @throws IOException
	 */
	public int createANDwriteInfluencia(String path) {
		double mydah_map[][] = gh.dah_map.getmap();
		try {
			Path p = Paths.get(path);
			Charset charset = Charset.forName("UTF-8");
			//Por defecto trae CREATE y TRUNCATE
			BufferedWriter writer = Files.newBufferedWriter(p, charset);
			for(int f = 0; f < mydah_map.length; f++){
				for (int c=0; c < mydah_map[f].length; c++){			
					writer.write(mydah_map[f][c]+";");
				}
				writer.write("\n");
			}
			//Importante cerrar el escritor, ya que si no, no escribe
			writer.close();
			return 0;
		} catch (IOException e) {
			System.out.println(e);
			return 1;
		}
	}
	
	public int createANDwrite(String path) {
		try {
			Path p = Paths.get(path);
			Charset charset = Charset.forName("UTF-8");
			//Por defecto trae CREATE y TRUNCATE
			BufferedWriter writer = Files.newBufferedWriter(p, charset);
			for(int f = 0; f < gh.map.length; f++){
				for (int c=0; c < gh.map[f].length; c++){
					if (gh.map[f][c] == -1){
						writer.write("M;");
					}
					else if (gh.map[f][c] == -2){
						writer.write("V;");
					}
					else if (gh.map[f][c] < 10){
						writer.write("0"+gh.map[f][c]+";");
					} 
					else {						
						writer.write(gh.map[f][c]+";");
					}
				}
				writer.write("\n");
			}
			//Importante cerrar el escritor, ya que si no, no escribe
			writer.close();
			return 0;
		} catch (IOException e) {
			System.out.println(e);
			return 1;
		}
	}

}
