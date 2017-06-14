package bot;

//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.Charset;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Predicate;

import org.iaie.btree.BehavioralTree;
import org.iaie.btree.task.composite.Selector;
import org.iaie.btree.task.composite.Sequence;
import org.iaie.btree.util.GameHandler;

import jnibwapi.BWAPIEventListener;
import jnibwapi.ChokePoint;
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.types.RaceType.RaceTypes;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType.UpgradeTypes;

public class Goliat implements BWAPIEventListener {

	/** reference to JNI-BWAPI */
	private final JNIBWAPI bwapi;
	
	BehavioralTree CollectTree, BuildTree, TrainTree, AttackTree, DefenseTree, UpdateTroopsTree, AddonTree;
	Unit buildingTree;
	JohnDoe gh;
	int frames;
	ArrayList<UnitType> TvsT, TvsP, TvsZ;
	
	/**
	 * Create a Java AI.
	 */
	public static void main(String[] args) {
		new Goliat();
	}
	
	
	public Goliat() {            

        // Creación de una instancia del connector JNIBWAPI. Esta instancia sólo puede ser creada
        // una vez ya que ha sido desarrollada mediante la utilización del patrón de diseño singlenton.
        bwapi = new JNIBWAPI(this, true);
        // Inicia la conexión en modo cliente con el servidor BWAPI que está conectado directamente al videojuego.
        // Este proceso crea una conexión mediante el uso de socket TCP con el servidor. 
        bwapi.start();
    }
	
	public void connected() {}

	public void matchStart() {
        
        bwapi.drawTargets(true);
        bwapi.drawHealth(true);
        
//      bwapi.enableUserInput();
		bwapi.enablePerfectInformation();
//		bwapi.setGameSpeed(0);
		
		gh = new JohnDoe(bwapi);
			
		TvsT = new ArrayList<UnitType>(5) {{
			add(UnitTypes.Terran_Marine);
			add(UnitTypes.Terran_Medic);
			add(UnitTypes.Terran_Siege_Tank_Tank_Mode);
			add(UnitTypes.Terran_Goliath);
			add(UnitTypes.Terran_Science_Vessel);
		}};
        TvsP = new ArrayList<UnitType>(6) {{
        	add(UnitTypes.Terran_Marine);
			add(UnitTypes.Terran_Medic);
			add(UnitTypes.Terran_Firebat);
			add(UnitTypes.Terran_Siege_Tank_Tank_Mode);
			add(UnitTypes.Terran_Goliath);
			add(UnitTypes.Terran_Science_Vessel);
        }};
        TvsZ = new ArrayList<UnitType>(7) {{
        	add(UnitTypes.Terran_Marine);
			add(UnitTypes.Terran_Medic);
			add(UnitTypes.Terran_Firebat);
			add(UnitTypes.Terran_Vulture);
			add(UnitTypes.Terran_Siege_Tank_Tank_Mode);
			add(UnitTypes.Terran_Goliath);
			add(UnitTypes.Terran_Science_Vessel);
        }};
        
        //Default
        gh.unitsToTrain = TvsT;
		
        //Initialize CC variables.
		for (Unit cc : bwapi.getMyUnits()){
			if (cc.getType() == UnitTypes.Terran_Command_Center){
				gh.cc = cc;
				gh.cc_select = cc;
				gh.CCs.add(cc.getID());
				gh.addCC();
				gh.finishedBuildings.add(cc);
			}
		}

		//Get the number of choke points to determinate strategy
		if (gh.number_chokePoints == 1) {
			for (ChokePoint cp : bwapi.getMap().getRegion(gh.cc_select.getPosition()).getChokePoints()){
				gh.defendGroup.destination = cp.getCenter();				
			}
		} else {
			gh.defendGroup.destination = gh.cc.getPosition().makeValid();
		}
		
		//Number of supplies
		gh.supplies = bwapi.getSelf().getSupplyUsed();
		//Influence map
		gh.createMap();
		
		frames = 0;

		bwapi.sendText("Secuencia recoleccion");
		// --------- Resources sequence ---------
		Selector<GameHandler> CollectResources = new Selector<>("Minerals or Vespin gas");
		CollectResources.addChild(new CollectGas("Vespin gas", gh));
		CollectResources.addChild(new CollectMineral("Minerals", gh));
		
		Sequence collect = new Sequence("Gather");
		collect.addChild(new FreeWorker("Free Worker", gh));
		collect.addChild(CollectResources);
		// --------- END resources -------------
		
		bwapi.sendText("Secuencia entrenamiento");
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
		Sequence TrainSiegeTank = new Sequence("Train Siege Tank");
		TrainSiegeTank.addChild(new CheckResources("Check resources siege tank", gh, UnitTypes.Terran_Siege_Tank_Tank_Mode));
		TrainSiegeTank.addChild(new ChooseBuilding("Check training siege tank", gh, UnitTypes.Terran_Siege_Tank_Tank_Mode));
		TrainSiegeTank.addChild(new TrainUnit("Train siege tank", gh, UnitTypes.Terran_Siege_Tank_Tank_Mode, UnitTypes.Terran_Factory));
		//Train vultures
		Sequence TrainVulture = new Sequence("Train Vulture");
		TrainVulture.addChild(new CheckResources("Check resources vulture", gh, UnitTypes.Terran_Vulture));
		TrainVulture.addChild(new ChooseBuilding("Check training vulture", gh, UnitTypes.Terran_Vulture));
		TrainVulture.addChild(new TrainUnit("Train vulture", gh, UnitTypes.Terran_Vulture, UnitTypes.Terran_Factory));
		//Train goliats
		Sequence TrainGoliat = new Sequence("Train Goliat");
		TrainGoliat.addChild(new CheckResources("Check resources goliat", gh, UnitTypes.Terran_Goliath));
		TrainGoliat.addChild(new ChooseBuilding("Check training goliat", gh, UnitTypes.Terran_Goliath));
		TrainGoliat.addChild(new TrainUnit("Train goliat", gh, UnitTypes.Terran_Goliath, UnitTypes.Terran_Factory));
		//Train science vessels
		Sequence TrainVessel = new Sequence("Train science vessel");
		TrainVessel.addChild(new CheckResources("Check resources science vessel", gh, UnitTypes.Terran_Science_Vessel));
		TrainVessel.addChild(new ChooseBuilding("Check training science vessels", gh, UnitTypes.Terran_Science_Vessel));
		TrainVessel.addChild(new TrainUnit("Train Science vessel", gh, UnitTypes.Terran_Science_Vessel, UnitTypes.Terran_Starport));

		Selector<Sequence> selectorTrain = new Selector<>("Selector train", TrainVessel, TrainGoliat, TrainVCE, TrainSiegeTank, 
															TrainVulture, TrainMedic, TrainFirebat, TrainMarine);
//		Selector<Sequence> selectorTrain = new Selector<>("Selector train", TrainVCE);
		// ----------- END TRAIN ---------

		bwapi.sendText("Secuencia edificios");
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
		//Build bunker
		Sequence buildBunker = new Sequence("Build bunker");
		buildBunker.addChild(new CheckResources("Check resources bunker", gh, UnitTypes.Terran_Bunker));
		buildBunker.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Bunker));
		buildBunker.addChild(new FreeBuilder("Find builder", gh));
		buildBunker.addChild(new MoveTo("Move builder", gh));
		buildBunker.addChild(new Build("Build bunker", gh, UnitTypes.Terran_Bunker));
		//Build CC
		Sequence buildCC = new Sequence("Build CC");
		buildCC.addChild(new CheckResources("Check resources CC", gh, UnitTypes.Terran_Command_Center));
		buildCC.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Command_Center));
		buildCC.addChild(new FreeBuilder("Find builder", gh));
		buildCC.addChild(new MoveTo("Move builder", gh));
		buildCC.addChild(new Build("Build CC", gh, UnitTypes.Terran_Command_Center));
		//Build starport
		Sequence buildStarport = new Sequence("Build starport");
		buildStarport.addChild(new CheckResources("Check resources starport", gh, UnitTypes.Terran_Starport));
		buildStarport.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Starport));
		buildStarport.addChild(new FreeBuilder("Find builder", gh));
		buildStarport.addChild(new MoveTo("Move builder", gh));
		buildStarport.addChild(new Build("Build starport", gh, UnitTypes.Terran_Starport));
		//Build science facility
		Sequence buildLab = new Sequence("Build science facility");
		buildLab.addChild(new CheckResources("Check resources facility", gh, UnitTypes.Terran_Science_Facility));
		buildLab.addChild(new FindPosition("Find position", gh, UnitTypes.Terran_Science_Facility));
		buildLab.addChild(new FreeBuilder("Find builder", gh));
		buildLab.addChild(new MoveTo("Move builder", gh));
		buildLab.addChild(new Build("Build facility", gh, UnitTypes.Terran_Science_Facility));
		
		Selector<Sequence> selectorBuild = new Selector<>("Selector build", buildSupply, buildBarracks, buildBay,
															buildRefinery, buildTurret, buildBunker, buildAcademy, 
															buildFactory, buildCC, buildStarport, buildLab, buildArmory);
		// ---------- END BUILD -----------
		
		bwapi.sendText("Secuencia addons");
		// ---------- Build addons ----------
		//Factory's addon
		Sequence addonFactory = new Sequence("Factory's addon");
		addonFactory.addChild(new CheckResources("Check resources addon", gh, UnitTypes.Terran_Machine_Shop));
		addonFactory.addChild(new FindBuilding("Find building", gh, UnitTypes.Terran_Factory));
		addonFactory.addChild(new BuildAddon("Build factory's addon", gh, UnitTypes.Terran_Machine_Shop));
		//Science facility's addon
		Sequence addonFacility = new Sequence("Facility's addon");
		addonFacility.addChild(new CheckResources("Check resources addon", gh, UnitTypes.Terran_Physics_Lab));
		addonFacility.addChild(new FindBuilding("Find building", gh, UnitTypes.Terran_Science_Facility));
		addonFacility.addChild(new BuildAddon("Build facility's addon", gh, UnitTypes.Terran_Physics_Lab));
		//Starport's addon
		Sequence addonStarport = new Sequence("Starport addon");
		addonStarport.addChild(new CheckResources("Check resources addon", gh, UnitTypes.Terran_Control_Tower));
		addonStarport.addChild(new FindBuilding("Find building", gh, UnitTypes.Terran_Starport));
		addonStarport.addChild(new BuildAddon("Build starport's addon", gh, UnitTypes.Terran_Control_Tower));
		
		Selector<Sequence> selectorAddons = new Selector<>("Selector addon", addonFactory, addonFacility, addonStarport);
		// ---------- END addons ----------
		
		bwapi.sendText("Secuencia tropas");
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
		
		bwapi.sendText("Secuencia ataque");
		// -------- Attack sequence ---------
		Sequence attack = new Sequence("Send to attack the troops");
		attack.addChild(new CheckStateTroops("Check troops status", gh));
		attack.addChild(new ChooseDestination("Choose destination", gh));
		attack.addChild(new SelectGroup("Select one troop to attack", gh));
		Selector<GameHandler> attackSelector = new Selector<>("Attack/Regroup");
		attackSelector.addChild(new SendAttack("Send to attack", gh));
		attackSelector.addChild(new SendRegroup("Regroup", gh));		
		attack.addChild(attackSelector);
		// ---------- END ATTACK -----------
		
		bwapi.sendText("Secuencia defensa");
		// --------- Defend sequence ---------
		Sequence defendBase = new Sequence("Defend base");
		defendBase.addChild(new SendDefend("Send to defend", gh));
		// --------- END DEFENSE -----------
		
		// --------- Fill bunker ---------
		Sequence fillBunker = new Sequence("Fill bunker");
		fillBunker.addChild(new FillBunker("Send to bunker", gh));
		// --------- END BUNKER ---------
		
		bwapi.sendText("Secuencia investigar");
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
		
		Selector<Sequence> selectorResearch = new Selector<>("Selector research", u238, caudecus, infantryArmor, 
																infantryWeapons, vehicleArmor, vehicleWeapons); 
		// --------------- END RESEARCH ---------------
		
		bwapi.sendText("Secuencia reparar");
		// ----------- Repair sequence ---------		
		Sequence repair = new Sequence("Repair");
		repair.addChild(new FindDamageBuildings("Find damaged buildings", gh));
		repair.addChild(new FreeBuilder("Free worker", gh));
		repair.addChild(new RepairBuilding("Repair building", gh));
		// ------------- END REPAIR --------------------
		
		bwapi.sendText("Secuencia CC");
		// ----------- CC managment -----------
		Sequence ccManage = new Sequence("CC managment");
		ccManage.addChild(new SelectCC("Select a CC", gh));
		// ----------- END CC managment
		
		bwapi.sendText("Creando arboles");
		
		CollectTree = new BehavioralTree("Gather/Repair tree");
		CollectTree.addChild(new Selector<>("MAIN SELECTOR", repair, ccManage, collect));
		BuildTree  = new BehavioralTree("Build/Research tree");
		BuildTree.addChild(new Selector<>("MAIN SELECTOR", selectorBuild, selectorResearch));
		AddonTree = new BehavioralTree("Addons tree");
		AddonTree.addChild(new Selector<>("MAIN SELECTOR", selectorAddons));
		TrainTree  = new BehavioralTree("Training tree");
		TrainTree.addChild(new Selector<>("MAIN SELECTOR", selectorTrain));
		UpdateTroopsTree = new BehavioralTree("Update/Check troops status");
		UpdateTroopsTree.addChild(new Selector<>("MAIN SELECTOR", compactTroops, createTroop));
		DefenseTree  = new BehavioralTree("Defense tree");
		DefenseTree.addChild(new Selector<>("MAIN SELECTOR", fillBunker, defendBase));
		AttackTree  = new BehavioralTree("Attack tree");
		AttackTree.addChild(new Selector<>("MAIN SELECTOR", attack));
		
		bwapi.sendText("Arboles creados");
		
		bwapi.sendText("gl hf bro :>");
		
	}

	public void matchFrame() {
		CollectTree.run();
		BuildTree.run();
		AddonTree.run();
		TrainTree.run();
		DefenseTree.run();
		UpdateTroopsTree.run();
		AttackTree.run();
		
		if(frames < 300){ //Each 300 frames, recalculate influences
			frames++;
		}else{
			frames = 0;
			gh.updateInfluences();
		}
		
	}


	public void unitCreate(int unitID) {
		//When start to build a building, add to remainingBuildings.
		if (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) {
			if (bwapi.getUnit(unitID).getType().isBuilding()){
				gh.remainingBuildings.add(bwapi.getUnit(unitID).getType());
				gh.posBuild = null;
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
		//List control (Buildings)
		if (control == 0) {
			for(Object u : gh.finishedBuildings.stream().filter(predicado).toArray()) {
				//Remove unit from lists and update variables
				gh.finishedBuildings.remove((Unit) u);
				if (gh.bunkers.contains((Unit ) u)) { gh.bunkers.remove((Unit) u);}
				
				if (((Unit) u).getType() == UnitTypes.Terran_Academy) gh.academy--;
				if (((Unit) u).getType() == UnitTypes.Terran_Barracks) gh.barracks--;
				if (((Unit) u).getType() == UnitTypes.Terran_Factory) gh.factory--;
				if (((Unit) u).getType() == UnitTypes.Terran_Engineering_Bay) gh.bay--;
				if (((Unit) u).getType() == UnitTypes.Terran_Armory) gh.armory--;
				if (((Unit) u).getType() == UnitTypes.Terran_Refinery) gh.refinery--;
				if (((Unit) u).getType() == UnitTypes.Terran_Science_Facility) gh.lab_cient--;
				if (((Unit) u).getType() == UnitTypes.Terran_Starport) gh.starport--;
				if (((Unit) u).getType() == UnitTypes.Terran_Supply_Depot) {
					gh.totalSupplies -= UnitTypes.Terran_Supply_Depot.getSupplyProvided();
				}
				if (((Unit) u).getType() == UnitTypes.Terran_Command_Center) {
					gh.totalSupplies -= UnitTypes.Terran_Command_Center.getSupplyProvided();
					gh.CCs.remove((Integer) unitID);
				}
				control++;
			}
		}
		//More list update (Units)
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
		//IF it's a SCV
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
		//Updates influence map
		int influencia = (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) ? 1 : -1;
		gh.dah_map.newUnit(this.bwapi.getUnit(unitID), influencia, 
				this.bwapi.getUnit(unitID).getPosition().getBX(), this.bwapi.getUnit(unitID).getPosition().getBY());
		/////////////////////////////////////
		
		//This 3 lines write in a file the influence map -- FOR DEBUGGING
//		String workingDirectory = System.getProperty("user.dir");
//		String path = workingDirectory + File.separator + "mapaInfluencia.txt";
//		createANDwriteInfluencia(path);
		
		//Update list, variables, etc...
		if (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) {
			//Add the unit to its list.
			if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_SCV){
				gh.VCEs.get(gh.CCs.indexOf(gh.cc_select.getID())).add(bwapi.getUnit(unitID));
			}
			
			//Remove the unit from the remaining list.
			if (gh.remainingUnits.contains(bwapi.getUnit(unitID).getType())){
				
				gh.remainingUnits.remove(bwapi.getUnit(unitID).getType());
				gh.supplies += bwapi.getUnit(unitID).getType().getSupplyRequired();
				
				if (bwapi.getUnit(unitID).getType() != UnitTypes.Terran_SCV) {
					gh.militaryUnits.add(bwapi.getUnit(unitID));
					gh.boredSoldiers.add(bwapi.getUnit(unitID));
					if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Science_Vessel) {
						gh.vessels++;
						gh.detector_first = false;
					}
				}
			}
			
			//Case: Building
			if (gh.remainingBuildings.contains(bwapi.getUnit(unitID).getType())) {
				
				gh.remainingBuildings.remove(bwapi.getUnit(unitID).getType());
				gh.finishedBuildings.add(bwapi.getUnit(unitID));
				//Updates variables.
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Supply_Depot)
					gh.totalSupplies += UnitTypes.Terran_Supply_Depot.getSupplyProvided();
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Barracks) gh.barracks++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Engineering_Bay) gh.bay++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Academy) gh.academy++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Factory) gh.factory++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Armory) gh.armory++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Bunker) gh.bunkers.add(bwapi.getUnit(unitID));
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Science_Facility) gh.lab_cient++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Starport) gh.starport++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Command_Center) {
					//If it's a CC, need to add control lists.
					gh.totalSupplies += UnitTypes.Terran_Command_Center.getSupplyProvided();
					if (gh.CCs.indexOf((Integer) unitID) == -1){
						gh.expanded = true;
						gh.CCs.add(unitID);
						gh.addCC();
					}
				}
				
				//Updates influence map.
				gh.updateMap(bwapi.getUnit(unitID).getTopLeft(), bwapi.getUnit(unitID).getBottomRight(), bwapi.getUnit(unitID).getType());

				//This 3 lines write in a file the construction map -- FOR DEBUGGING
//				workingDirectory = System.getProperty("user.dir");
//				path = workingDirectory + File.separator + "mapa.txt";
//				createANDwrite(path);
			}
		}
	}

	public void unitMorph(int unitID) {
		//Refinery it's an especial case.
		if (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) {
			if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Refinery) gh.refinery++;
		}
	}
	
	public void playerDropped(int playerID) { }
	
	public void matchEnd(boolean winner) {
		// :P
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
	public void unitDiscover(int unitID) {
	}
	public void unitEvade(int unitID) {	}
	public void unitShow(int unitID) {
		//Enemy player
		if (bwapi.getUnit(unitID).getPlayer().getID() != bwapi.getSelf().getID() && 
				( bwapi.getUnit(unitID).getPlayer().getRace() == RaceTypes.Protoss || 
						bwapi.getUnit(unitID).getPlayer().getRace() == RaceTypes.Zerg ) && frames > 0) {
			//Updates unitsToTrain
			if (bwapi.getUnit(unitID).getPlayer().getRace() == RaceTypes.Protoss &&
					gh.enemyRace != RaceTypes.Protoss) {
				gh.unitsToTrain = TvsP;
			} else if (bwapi.getUnit(unitID).getPlayer().getRace() == RaceTypes.Zerg &&
					gh.enemyRace != RaceTypes.Zerg) {
				gh.unitsToTrain = TvsZ;
			} else {
				gh.unitsToTrain = TvsT;
			}
			gh.enemyRace = bwapi.getUnit(unitID).getPlayer().getRace();
			if (gh.vessels == 0) {
				gh.detector_first = true;	
			}						
		}
	}
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
//	public int createANDwriteInfluencia(String path) {
//		double mydah_map[][] = gh.dah_map.getmap();
//		try {
//			Path p = Paths.get(path);
//			Charset charset = Charset.forName("UTF-8");
//			//Por defecto trae CREATE y TRUNCATE
//			BufferedWriter writer = Files.newBufferedWriter(p, charset);
//			for(int f = 0; f < mydah_map.length; f++){
//				for (int c=0; c < mydah_map[f].length; c++){			
//					writer.write(mydah_map[f][c]+";");
//				}
//				writer.write("\n");
//			}
//			//Importante cerrar el escritor, ya que si no, no escribe
//			writer.close();
//			return 0;
//		} catch (IOException e) {
//			System.out.println(e);
//			return 1;
//		}
//	}
	
//	public int createANDwrite(String path) {
//		try {
//			Path p = Paths.get(path);
//			Charset charset = Charset.forName("UTF-8");
//			//Por defecto trae CREATE y TRUNCATE
//			BufferedWriter writer = Files.newBufferedWriter(p, charset);
//			for(int f = 0; f < gh.map.length; f++){
//				for (int c=0; c < gh.map[f].length; c++){
//					if (gh.map[f][c] == -1){
//						writer.write("M;");
//					}
//					else if (gh.map[f][c] == -2){
//						writer.write("V;");
//					}
//					else if (gh.map[f][c] < 10){
//						writer.write("0"+gh.map[f][c]+";");
//					} 
//					else {						
//						writer.write(gh.map[f][c]+";");
//					}
//				}
//				writer.write("\n");
//			}
//			//Importante cerrar el escritor, ya que si no, no escribe
//			writer.close();
//			return 0;
//		} catch (IOException e) {
//			System.out.println(e);
//			return 1;
//		}
//	}

}
