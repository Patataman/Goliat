package bot;

import java.io.BufferedWriter;
//import java.io.File;
//import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.Charset;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
import java.util.ArrayList;
//import java.util.function.Predicate;
import java.util.function.Predicate;

import org.iaie.btree.BehavioralTree;
import org.iaie.btree.task.composite.Selector;
import org.iaie.btree.task.composite.Sequence;
import org.iaie.btree.util.GameHandler;

import bwapi.BWEventListener;
import bwapi.Color;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.Race;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwta.BWTA;

public class Goliat implements BWEventListener {
	
	private Mirror mirror = new Mirror();

    private Game game;
    private Player self;
    

	BehavioralTree CollectTree, BuildTree, TrainTree, AttackTree, DefenseTree, UpdateTroopsTree, AddonTree;
	Unit buildingTree;
	JohnDoe gh;
	//int frames;
	ArrayList<UnitType> TvsT, TvsP, TvsZ;



    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }
	
	/**
	 * Create a Java AI.
	 */
	public static void main(String[] args) {
		new Goliat().run();
	}

	public void onStart() {
		
        game = mirror.getGame();
        game.setLocalSpeed(6);
        self = game.self();
        
//        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
//        BWTA.buildChokeNodes();
//        System.out.println("Map data ready");
        
        game.enableFlag(1);
//      self.enableUserInput();
//		self.enablePerfectInformation();
//		self.setGameSpeed(0);
		
		gh = new JohnDoe(game, self);
			
		TvsT = new ArrayList<UnitType>(5) {{
			add(UnitType.Terran_Marine);
			add(UnitType.Terran_Medic);
			add(UnitType.Terran_Siege_Tank_Tank_Mode);
			add(UnitType.Terran_Goliath);
			add(UnitType.Terran_Science_Vessel);
		}};
        TvsP = new ArrayList<UnitType>(6) {{
        	add(UnitType.Terran_Marine);
			add(UnitType.Terran_Medic);
			add(UnitType.Terran_Firebat);
			add(UnitType.Terran_Siege_Tank_Tank_Mode);
			add(UnitType.Terran_Goliath);
			add(UnitType.Terran_Science_Vessel);
        }};
        TvsZ = new ArrayList<UnitType>(7) {{
        	add(UnitType.Terran_Marine);
			add(UnitType.Terran_Medic);
			add(UnitType.Terran_Firebat);
			add(UnitType.Terran_Vulture);
			add(UnitType.Terran_Siege_Tank_Tank_Mode);
			add(UnitType.Terran_Goliath);
			add(UnitType.Terran_Science_Vessel);
        }};

        if(gh.enemyRace == Race.Terran) {
        	 gh.unitsToTrain.addAll(TvsT);
        }
        else if(gh.enemyRace == Race.Zerg) {
        	 gh.unitsToTrain.addAll(TvsZ);
        }
        else if(gh.enemyRace == Race.Protoss) {
        	 gh.unitsToTrain.addAll(TvsP);
        }
        else {
        	 //Default
        	 gh.unitsToTrain.addAll(TvsT);
        }

        //Initialize CC variables.
		for (Unit cc : self.getUnits()){
			if (cc.getType() == UnitType.Terran_Command_Center){
				gh.cc = cc;
				gh.cc_select = cc;
				gh.CCs.add(cc);
				gh.addCC(cc);
				gh.finishedBuildings.add(cc);
				break;
			}
		}

		//Get the number of choke points to determinate strategy
		gh.defendGroup.destination = gh.cc.getPosition().makeValid().toTilePosition();
		
		//Number of supplies
		gh.supplies = self.supplyUsed();
		//Influence map
		gh.createMap();

		game.sendText("Secuencia recoleccion");
		
		// ---------- Scout sequence -----------
		Sequence scoutBases = new Sequence("ScoutInitialBases");
		scoutBases.addChild(new CheckTime("Check if it's moment to scout", gh));
		scoutBases.addChild(new ChooseSCV("Select 1 SCV", gh));
		scoutBases.addChild(new SendSCV("Send to scout", gh));
		// --------- END scout -------------
		
		// --------- Resources sequence ---------
		Selector<GameHandler> CollectResources = new Selector<>("Minerals or Vespin gas");
		CollectResources.addChild(new CollectGas("Vespin gas", gh));
		CollectResources.addChild(new CollectMineral("Minerals", gh));
		
		Sequence collect = new Sequence("Gather");
		collect.addChild(new FreeWorker("Free Worker", gh));
		collect.addChild(CollectResources);
		// --------- END resources -------------
		
		game.sendText("Secuencia entrenamiento");
		// -------- Training sequences ---------
		//Train SCVs
		Sequence TrainVCE = new Sequence("Train SCV");
		TrainVCE.addChild(new CheckResources("Check resources SCV", gh, UnitType.Terran_SCV));
		TrainVCE.addChild(new ChooseBuilding("Check training SCV", gh, UnitType.Terran_SCV));
		TrainVCE.addChild(new TrainUnit("Train SCV", gh, UnitType.Terran_SCV, UnitType.Terran_Command_Center));
		//Train marines
		Sequence TrainMarine = new Sequence("Train Marine");
		TrainMarine.addChild(new CheckResources("Check resources marine", gh, UnitType.Terran_Marine));
		TrainMarine.addChild(new ChooseBuilding("Check training marine", gh, UnitType.Terran_Marine));
		TrainMarine.addChild(new TrainUnit("Train marine", gh, UnitType.Terran_Marine, UnitType.Terran_Barracks));
		//Train medics
		Sequence TrainMedic = new Sequence("Train medic");
		TrainMedic.addChild(new CheckResources("Check resources medic", gh, UnitType.Terran_Medic));
		TrainMedic.addChild(new ChooseBuilding("Check training medic", gh, UnitType.Terran_Medic));
		TrainMedic.addChild(new TrainUnit("Train medic", gh, UnitType.Terran_Medic, UnitType.Terran_Barracks));
		//Train firebats
		Sequence TrainFirebat = new Sequence("Train Firebat");
		TrainFirebat.addChild(new CheckResources("Check resources firebat", gh, UnitType.Terran_Firebat));
		TrainFirebat.addChild(new ChooseBuilding("Check training firebat", gh, UnitType.Terran_Firebat));
		TrainFirebat.addChild(new TrainUnit("Train Firebat", gh, UnitType.Terran_Firebat, UnitType.Terran_Barracks));
		//Train siege tanks
		Sequence TrainSiegeTank = new Sequence("Train Siege Tank");
		TrainSiegeTank.addChild(new CheckResources("Check resources siege tank", gh, UnitType.Terran_Siege_Tank_Tank_Mode));
		TrainSiegeTank.addChild(new ChooseBuilding("Check training siege tank", gh, UnitType.Terran_Siege_Tank_Tank_Mode));
		TrainSiegeTank.addChild(new TrainUnit("Train siege tank", gh, UnitType.Terran_Siege_Tank_Tank_Mode, UnitType.Terran_Factory));
		//Train vultures
		Sequence TrainVulture = new Sequence("Train Vulture");
		TrainVulture.addChild(new CheckResources("Check resources vulture", gh, UnitType.Terran_Vulture));
		TrainVulture.addChild(new ChooseBuilding("Check training vulture", gh, UnitType.Terran_Vulture));
		TrainVulture.addChild(new TrainUnit("Train vulture", gh, UnitType.Terran_Vulture, UnitType.Terran_Factory));
		//Train goliats
		Sequence TrainGoliat = new Sequence("Train Goliat");
		TrainGoliat.addChild(new CheckResources("Check resources goliat", gh, UnitType.Terran_Goliath));
		TrainGoliat.addChild(new ChooseBuilding("Check training goliat", gh, UnitType.Terran_Goliath));
		TrainGoliat.addChild(new TrainUnit("Train goliat", gh, UnitType.Terran_Goliath, UnitType.Terran_Factory));
		//Train science vessels
		Sequence TrainVessel = new Sequence("Train science vessel");
		TrainVessel.addChild(new CheckResources("Check resources science vessel", gh, UnitType.Terran_Science_Vessel));
		TrainVessel.addChild(new ChooseBuilding("Check training science vessels", gh, UnitType.Terran_Science_Vessel));
		TrainVessel.addChild(new TrainUnit("Train Science vessel", gh, UnitType.Terran_Science_Vessel, UnitType.Terran_Starport));

		Selector<Sequence> selectorTrain = new Selector<>("Selector train", TrainVessel, TrainGoliat, TrainSiegeTank, 
															TrainVulture, TrainMedic, TrainFirebat, TrainMarine, TrainVCE);
		// ----------- END TRAIN ---------

		game.sendText("Secuencia edificios");
		// -------- Building sequences ---------
		//Build supplies
		Sequence buildSupply = new Sequence("Build supplies");
		buildSupply.addChild(new CheckResources("Check resources supplies", gh, UnitType.Terran_Supply_Depot));
		buildSupply.addChild(new FindPosition("Find position", gh, UnitType.Terran_Supply_Depot));
		buildSupply.addChild(new FreeBuilder("Find builder", gh));
		buildSupply.addChild(new MoveTo("Move builder", gh));
		buildSupply.addChild(new Build("Build supplies", gh, UnitType.Terran_Supply_Depot));
		//Build barracks
		Sequence buildBarracks = new Sequence("Build barracks");
		buildBarracks.addChild(new CheckResources("Check resources barracks", gh, UnitType.Terran_Barracks));
		buildBarracks.addChild(new FindPosition("Find position", gh, UnitType.Terran_Barracks));
		buildBarracks.addChild(new FreeBuilder("Find builder", gh));
		buildBarracks.addChild(new MoveTo("Move builder", gh));
		buildBarracks.addChild(new Build("Build barracks", gh, UnitType.Terran_Barracks));
		//Build refinery
		Sequence buildRefinery = new Sequence("Build refinery");
		buildRefinery.addChild(new CheckResources("Check resources refinery", gh, UnitType.Terran_Refinery));
		buildRefinery.addChild(new FindPosition("Find position", gh, UnitType.Terran_Refinery));
		buildRefinery.addChild(new FreeBuilder("Find builder", gh));
		buildRefinery.addChild(new MoveTo("Move builder", gh));
		buildRefinery.addChild(new Build("Build refinery", gh, UnitType.Terran_Refinery));
		//Build bay
		Sequence buildBay = new Sequence("Build bay");
		buildBay.addChild(new CheckResources("Check resources bay", gh, UnitType.Terran_Engineering_Bay));
		buildBay.addChild(new FindPosition("Find position", gh, UnitType.Terran_Engineering_Bay));
		buildBay.addChild(new FreeBuilder("Find builder", gh));
		buildBay.addChild(new MoveTo("Move builder", gh));
		buildBay.addChild(new Build("Build bay", gh, UnitType.Terran_Engineering_Bay));
		//Build academy
		Sequence buildAcademy = new Sequence("Build academy");
		buildAcademy.addChild(new CheckResources("Check resources academy", gh, UnitType.Terran_Academy));
		buildAcademy.addChild(new FindPosition("Find position", gh, UnitType.Terran_Academy));
		buildAcademy.addChild(new FreeBuilder("Find builder", gh));
		buildAcademy.addChild(new MoveTo("Move builder", gh));
		buildAcademy.addChild(new Build("Build academy", gh, UnitType.Terran_Academy));
		//Build factory
		Sequence buildFactory = new Sequence("Build factory");
		buildFactory.addChild(new CheckResources("Check resources factory", gh, UnitType.Terran_Factory));
		buildFactory.addChild(new FindPosition("Find position", gh, UnitType.Terran_Factory));
		buildFactory.addChild(new FreeBuilder("Find builder", gh));
		buildFactory.addChild(new MoveTo("Move builder", gh));
		buildFactory.addChild(new Build("Build factory", gh, UnitType.Terran_Factory));
		//Build armory
		Sequence buildArmory = new Sequence("Build armory");
		buildArmory.addChild(new CheckResources("Check resources armory", gh, UnitType.Terran_Armory));
		buildArmory.addChild(new FindPosition("Find position", gh, UnitType.Terran_Armory));
		buildArmory.addChild(new FreeBuilder("Find builder", gh));
		buildArmory.addChild(new MoveTo("Move builder", gh));
		buildArmory.addChild(new Build("Build armory", gh, UnitType.Terran_Armory));
		//Build missile turret
		Sequence buildTurret = new Sequence("Build missile turret");
		buildTurret.addChild(new CheckResources("Check resources missile turret", gh, UnitType.Terran_Missile_Turret));
		buildTurret.addChild(new FindPosition("Find position", gh, UnitType.Terran_Missile_Turret));
		buildTurret.addChild(new FreeBuilder("Find builder", gh));
		buildTurret.addChild(new MoveTo("Move builder", gh));
		buildTurret.addChild(new Build("Build missile turret", gh, UnitType.Terran_Missile_Turret));
		//Build bunker
		Sequence buildBunker = new Sequence("Build bunker");
		buildBunker.addChild(new CheckResources("Check resources bunker", gh, UnitType.Terran_Bunker));
		buildBunker.addChild(new FindPosition("Find position", gh, UnitType.Terran_Bunker));
		buildBunker.addChild(new FreeBuilder("Find builder", gh));
		buildBunker.addChild(new MoveTo("Move builder", gh));
		buildBunker.addChild(new Build("Build bunker", gh, UnitType.Terran_Bunker));
		//Build CC
		Sequence buildCC = new Sequence("Build CC");
		buildCC.addChild(new CheckResources("Check resources CC", gh, UnitType.Terran_Command_Center));
		buildCC.addChild(new FindPosition("Find position", gh, UnitType.Terran_Command_Center));
		buildCC.addChild(new FreeBuilder("Find builder", gh));
		buildCC.addChild(new MoveTo("Move builder", gh));
		buildCC.addChild(new Build("Build CC", gh, UnitType.Terran_Command_Center));
		//Build starport
		Sequence buildStarport = new Sequence("Build starport");
		buildStarport.addChild(new CheckResources("Check resources starport", gh, UnitType.Terran_Starport));
		buildStarport.addChild(new FindPosition("Find position", gh, UnitType.Terran_Starport));
		buildStarport.addChild(new FreeBuilder("Find builder", gh));
		buildStarport.addChild(new MoveTo("Move builder", gh));
		buildStarport.addChild(new Build("Build starport", gh, UnitType.Terran_Starport));
		//Build science facility
		Sequence buildLab = new Sequence("Build science facility");
		buildLab.addChild(new CheckResources("Check resources facility", gh, UnitType.Terran_Science_Facility));
		buildLab.addChild(new FindPosition("Find position", gh, UnitType.Terran_Science_Facility));
		buildLab.addChild(new FreeBuilder("Find builder", gh));
		buildLab.addChild(new MoveTo("Move builder", gh));
		buildLab.addChild(new Build("Build facility", gh, UnitType.Terran_Science_Facility));
		
		Selector<Sequence> selectorBuild = new Selector<>("Selector build", buildSupply, buildBarracks, buildRefinery,
															buildBay, buildTurret, buildBunker, buildAcademy,
															buildFactory, buildStarport, buildLab, buildCC, buildArmory);
		// ---------- END BUILD -----------
		
		game.sendText("Secuencia addons");
		// ---------- Build addons ----------
		//Factory's addon
		Sequence addonFactory = new Sequence("Factory's addon");
		addonFactory.addChild(new CheckResources("Check resources addon", gh, UnitType.Terran_Machine_Shop));
		addonFactory.addChild(new FindBuilding("Find building", gh, UnitType.Terran_Factory));
		addonFactory.addChild(new BuildAddon("Build factory's addon", gh, UnitType.Terran_Machine_Shop));
		//Science facility's addon
		Sequence addonFacility = new Sequence("Facility's addon");
		addonFacility.addChild(new CheckResources("Check resources addon", gh, UnitType.Terran_Physics_Lab));
		addonFacility.addChild(new FindBuilding("Find building", gh, UnitType.Terran_Science_Facility));
		addonFacility.addChild(new BuildAddon("Build facility's addon", gh, UnitType.Terran_Physics_Lab));
		//Starport's addon
		Sequence addonStarport = new Sequence("Starport addon");
		addonStarport.addChild(new CheckResources("Check resources addon", gh, UnitType.Terran_Control_Tower));
		addonStarport.addChild(new FindBuilding("Find building", gh, UnitType.Terran_Starport));
		addonStarport.addChild(new BuildAddon("Build starport's addon", gh, UnitType.Terran_Control_Tower));
		//Control tower addon
		Sequence addonComsat = new Sequence("Comsat addon");
		addonComsat.addChild(new CheckResources("Check resources addon", gh, UnitType.Terran_Comsat_Station));
		addonComsat.addChild(new FindBuilding("Find building", gh, UnitType.Terran_Command_Center));
		addonComsat.addChild(new BuildAddon("Build CC's addon", gh, UnitType.Terran_Comsat_Station));
		
		Selector<Sequence> selectorAddons = new Selector<>("Selector addon", addonFactory, addonFacility, addonStarport, addonComsat);
		// ---------- END addons ----------
		
		game.sendText("Secuencia tropas");
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
		
		game.sendText("Secuencia ataque");
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
		
		game.sendText("Secuencia defensa");
		// --------- Defend sequence ---------
		Sequence defendBase = new Sequence("Defend base");
		defendBase.addChild(new SendDefend("Send to defend", gh));
		// --------- END DEFENSE -----------
		
		// --------- Fill bunker ---------
		Sequence fillBunker = new Sequence("Fill bunker");
		fillBunker.addChild(new FillBunker("Send to bunker", gh));
		// --------- END BUNKER ---------
		
		//---------- Attack closer enemies -------
		Sequence attackCloserEnemies = new Sequence("Attack closer enemies");
		attackCloserEnemies.addChild(new CheckPerimeter("Checks if there is enemies in the perimeter", gh));
		attackCloserEnemies.addChild(new AttackIntruders("Attack enemies inside the perimeter", gh));;
		// ------------ END Closer enemies -------
		
		game.sendText("Secuencia investigar");
		// ---------- Research sequence --------
		//Research U238 (Academy)
		Sequence u238 = new Sequence("Research U238");
		u238.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeType.U_238_Shells));
		u238.addChild(new Research("Research", gh, UnitType.Terran_Academy, UpgradeType.U_238_Shells));
		//Research Caduceus reactor (Academy, for medics)
		Sequence caudecus = new Sequence("Research caduceus");
		caudecus.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeType.Caduceus_Reactor));
		caudecus.addChild(new Research("Research", gh, UnitType.Terran_Academy, UpgradeType.Caduceus_Reactor));
		//Research infantry armor (Bay)
		Sequence infantryArmor = new Sequence("Research infantry armor");
		infantryArmor.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeType.Terran_Infantry_Armor));
		infantryArmor.addChild(new Research("Research", gh, UnitType.Terran_Engineering_Bay, UpgradeType.Terran_Infantry_Armor));
		//Research infantry weapons (Bay)
		Sequence infantryWeapons = new Sequence("Research infantry weapons");
		infantryWeapons.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeType.Terran_Infantry_Weapons));
		infantryWeapons.addChild(new Research("Research", gh, UnitType.Terran_Engineering_Bay, UpgradeType.Terran_Infantry_Weapons));
		//Research vehicle armor (Armory)
		Sequence vehicleArmor = new Sequence("Research vehicle armor");
		vehicleArmor.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeType.Terran_Vehicle_Plating));
		vehicleArmor.addChild(new Research("Research", gh, UnitType.Terran_Armory, UpgradeType.Terran_Vehicle_Plating));
		//Research vehicle weapons (Armory)
		Sequence vehicleWeapons = new Sequence("Research vehicle weapons");
		vehicleWeapons.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeType.Terran_Vehicle_Weapons));
		vehicleWeapons.addChild(new Research("Research", gh, UnitType.Terran_Armory, UpgradeType.Terran_Vehicle_Weapons));
		//Research Caronte boosters (Machine shop)
		Sequence caronteBoosters = new Sequence("Research vehicle weapons");
		caronteBoosters.addChild(new CheckResearch("Checks if it can be researched", gh, UpgradeType.Charon_Boosters));
		caronteBoosters.addChild(new Research("Research", gh, UnitType.Terran_Machine_Shop, UpgradeType.Charon_Boosters));
		
		Selector<Sequence> selectorResearch = new Selector<>("Selector research", u238, caudecus, infantryArmor, 
																infantryWeapons, vehicleArmor, vehicleWeapons, caronteBoosters); 
		// --------------- END RESEARCH ---------------
		
		game.sendText("Secuencia reparar");
		// ----------- Repair sequence ---------		
		Sequence repair = new Sequence("Repair");
		repair.addChild(new FindDamageBuildings("Find damaged buildings", gh));
		repair.addChild(new FreeBuilder("Free worker", gh));
		repair.addChild(new RepairBuilding("Repair building", gh));
		// ------------- END REPAIR --------------------
		
		game.sendText("Secuencia CC");
		// ----------- CC managment -----------
		Sequence ccManage = new Sequence("CC managment");
		ccManage.addChild(new SelectCC("Select a CC", gh));
		// ----------- END CC managment
		
		game.sendText("Creando arboles");
		
		CollectTree = new BehavioralTree("Gather/Repair tree");
		CollectTree.addChild(new Selector<>("MAIN SELECTOR", scoutBases, repair, ccManage, collect));
		BuildTree  = new BehavioralTree("Build/Research tree");
		BuildTree.addChild(new Selector<>("MAIN SELECTOR", selectorBuild, selectorResearch));
		AddonTree = new BehavioralTree("Addons tree");
		AddonTree.addChild(selectorAddons);
		TrainTree  = new BehavioralTree("Training tree");
		TrainTree.addChild(selectorTrain);
		UpdateTroopsTree = new BehavioralTree("Update/Check troops status");
		UpdateTroopsTree.addChild(new Selector<>("MAIN SELECTOR", compactTroops, createTroop));
		DefenseTree  = new BehavioralTree("Defense tree");
		DefenseTree.addChild(new Selector<>("MAIN SELECTOR", attackCloserEnemies, fillBunker, defendBase));
		AttackTree  = new BehavioralTree("Attack tree");
		AttackTree.addChild(attack);
		
		game.sendText("Arboles creados");
		
		game.sendText("gl hf bro :>");
		
	}

    public void onFrame() {
    	if (this.game.elapsedTime() > 0) {
    		CollectTree.run();
    		TrainTree.run();
    		BuildTree.run();
    		AddonTree.run();
    		DefenseTree.run();
    		if (gh.militaryUnits.size() > 0) {
    			UpdateTroopsTree.run();
    			AttackTree.run();
    		}
    		if (game.getFrameCount() % 200 == 0) { //Each 200 frames, recalculate influences
    			gh.updateInfluences();
    		}
    		if (game.getFrameCount() % 5 == 0) {
    			gh.mineral = self.minerals();
    			gh.vespin_gas = self.gas();
    		}
    		gh.debug();
    	}
    }

	public void onUnitCreate(Unit unit) {
		//When start to build a building, add to remainingBuildings.
		if (unit.getPlayer().getID() == self.getID() && game.elapsedTime() > 0) {
			if (unit.getType().isBuilding()){
				gh.remainingBuildings.add(unit.getType());
				gh.posBuild = null;
				if (unit.getType() == UnitType.Terran_Command_Center) {
					gh.CCs.add(unit);
				}
			} else {
				gh.remainingUnits.add(unit);
			}
		}
	}
	
	public void onUnitDestroy(Unit unit) {
		gh.dah_map.removeUnitDead(unit.getID());
		
		if (unit.getType().isMineralField()) {
			for(ArrayList<Unit> mineralNode : gh.mineralNodes) {
				Predicate<Unit> removeNode = mn-> mn.getID() == unit.getID();
				mineralNode.removeIf(removeNode);
				if (mineralNode.isEmpty()) {
					gh.workersMineral.get(gh.mineralNodes.indexOf(mineralNode)).clear();
				}
			}
//			Predicate<ArrayList<Unit>> clearNodes = m-> m.isEmpty();
//			gh.mineralNodes.removeIf(clearNodes);			
		}
		
		if (gh.intruders.contains(unit)) {
			gh.intruders.remove(unit);
		} else if (unit.getPlayer().getID() == self.getID() && game.elapsedTime() > 0) {
			//List control (Buildings)
			if (unit.getType().isBuilding()) {
				for(Unit u : gh.finishedBuildings) {
					if (gh.bunkers.contains(u)) { gh.bunkers.remove(u);}
					else if (u.getType() == UnitType.Terran_Academy) gh.academy--;
					else if (u.getType() == UnitType.Terran_Barracks) gh.barracks--;
					else if (u.getType() == UnitType.Terran_Factory) gh.factory--;
					else if (u.getType() == UnitType.Terran_Engineering_Bay) gh.bay--;
					else if (u.getType() == UnitType.Terran_Armory) gh.armory--;
					else if (u.getType() == UnitType.Terran_Refinery) gh.refinery--;
					else if (u.getType() == UnitType.Terran_Science_Facility) gh.lab_cient--;
					else if (u.getType() == UnitType.Terran_Starport) gh.starport--;
					else if (u.getType() == UnitType.Terran_Supply_Depot) {
						gh.totalSupplies -= UnitType.Terran_Supply_Depot.supplyProvided();
					}
					if (u.getType() == UnitType.Terran_Command_Center) {
						gh.totalSupplies -= UnitType.Terran_Command_Center.supplyProvided();
						gh.CCs.remove(unit);
					}
				}
				//Remove unit from lists and update variables
				gh.finishedBuildings.remove(unit);
			} else {
				if (gh.scouter != null &&
						gh.scouter.getID() == unit.getID()){
					gh.scouter = null;
				}
				if (gh.militia.contains(unit)) {
					gh.militia.remove(unit);
				}
				if (unit.getType().isWorker()) {
					if (gh.workers.contains(unit)){
						gh.workers.remove(unit);
					}
					if (gh.repairer.contains(unit)) {
						gh.repairer.remove(unit);
					}
					for(ArrayList<Unit> vces_cc : gh.VCEs){
						if (vces_cc.contains(unit)) {
							vces_cc.remove(unit);
							gh.supplies -= unit.getType().supplyRequired();
							break;
						}
					}
					for(ArrayList<Unit> minerals : gh.workersMineral) {
						if (minerals.contains(unit)) {
							minerals.remove(unit);
							break;
						}
					}
					for(ArrayList<Unit> vespin : gh.workersVespin) {
						if (vespin.contains(unit)) {
							vespin.remove(unit);
							break;
						}
					}
					
				} else {					
					if (gh.militaryUnits.contains(unit)) {
						gh.supplies -= unit.getType().supplyRequired();
						gh.militaryUnits.remove(unit);
					}
					if (gh.boredSoldiers.contains(unit)) {
						gh.boredSoldiers.remove(unit);
					}
					if (gh.attackGroup.units.contains(unit)){
						gh.attackGroup.units.remove(unit);
					}
					if (gh.defendGroup.units.contains(unit)) {
						gh.defendGroup.units.remove(unit);
					}
					for (Troop tropa: gh.assaultTroop){
						if (tropa.units.contains(unit)) {
							tropa.units.remove(unit);
							break;
						}
					}
				}
			}
		}
	}
	
	public void onUnitComplete(Unit unit) {
		
		//Update list, variables, etc...
		if (unit.getPlayer().getID() == self.getID()) {
			
			//Updates influence map
			gh.dah_map.newUnit(unit, 1, 
								unit.getTilePosition().getX(), 
								unit.getTilePosition().getY(), true);
			/////////////////////////////////////
			
			//This 3 lines write in a file the influence map -- FOR DEBUGGING
//			String workingDirectory = System.getProperty("user.dir");
//			String path = workingDirectory + File.separator + "mapaInfluencia.txt";
//			createANDwriteInfluencia(path);
			//Add the unit to its list.
			if (unit.getType() == UnitType.Terran_SCV){
				gh.VCEs.get(gh.CCs.indexOf(gh.cc_select)).add(unit);
			}
			
			//Remove the unit from the remaining list.
			if (gh.remainingUnits.contains(unit)){
				gh.remainingUnits.remove(unit);
				gh.supplies += unit.getType().supplyRequired();
				
				if (unit.getType() != UnitType.Terran_SCV) {
					gh.militaryUnits.add(unit);
					gh.boredSoldiers.add(unit);
					if (unit.getType() == UnitType.Terran_Science_Vessel) {
						gh.vessels++;
						gh.detector_first = false;
					}
				}
			}
			
			//Case: Building
			if (gh.remainingBuildings.contains(unit.getType()) && game.elapsedTime() > 0) {
				
				gh.remainingBuildings.remove(unit.getType());
				gh.finishedBuildings.add(unit);
				//Updates variables.
				if (unit.getType() == UnitType.Terran_Supply_Depot)
					gh.totalSupplies += UnitType.Terran_Supply_Depot.supplyProvided();
				if (unit.getType() == UnitType.Terran_Barracks) gh.barracks++;
				if (unit.getType() == UnitType.Terran_Engineering_Bay) gh.bay++;
				if (unit.getType() == UnitType.Terran_Academy) gh.academy++;
				if (unit.getType() == UnitType.Terran_Factory) gh.factory++;
				if (unit.getType() == UnitType.Terran_Armory) gh.armory++;
				if (unit.getType() == UnitType.Terran_Bunker) gh.bunkers.add(unit);
				if (unit.getType() == UnitType.Terran_Science_Facility) gh.lab_cient++;
				if (unit.getType() == UnitType.Terran_Starport) gh.starport++;
				if (unit.getType() == UnitType.Terran_Command_Center) {
					//If it's a CC, need to add control lists.
					gh.totalSupplies += UnitType.Terran_Command_Center.supplyProvided();
					if (gh.CCs.contains(unit)){
						gh.expanded = true;
						gh.addCC(unit);
					}
				}
				
				//Updates influence map.
				gh.updateMap(unit.getTilePosition(), unit.getType());
				
				//This 3 lines write in a file the construction map -- FOR DEBUGGING
//				String workingDirectory = System.getProperty("user.dir");
//				String path = workingDirectory + File.separator + "mapa.txt";
//				createANDwrite(path);
			}
		}
	}
	
	public void onUnitShow(Unit unit) {
		//Enemy player
		gh.updateMap(unit.getTilePosition(), unit.getType());
		
		if (unit.getPlayer().getID() != self.getID() &&
				!unit.getType().isNeutral() && 
				!unit.getType().isSpecialBuilding()){
			
			if (game.getFrameCount() > 0) {
				if(gh.enemyRace == Race.Unknown) {
					//Updates unitsToTrain
					if (unit.getType().getRace() == Race.Protoss) {
						gh.unitsToTrain = TvsP;
						if (gh.vessels == 0) {
							gh.detector_first = true;	
						}
					} else if (unit.getType().getRace() == Race.Zerg) {
						gh.unitsToTrain = TvsZ;
						if (gh.vessels == 0) {
							gh.detector_first = true;	
						}
					} else {
						gh.unitsToTrain = TvsT;
					}
					gh.enemyRace = unit.getType().getRace();
				}
				
			}
			
			if (gh.scouter != null && gh.scouter.isIdle()) {
				gh.scouter = null;
//				String workingDirectory = System.getProperty("user.dir");
//				String path = workingDirectory + File.separator + "mapaInfluencia.txt";
//				createANDwriteInfluencia(path);
			}
			

		}
	}
	
	public void onEnd(boolean winner) {
		game.sendText("GG "+game.enemy().getName());
	}

	public void onNukeDetect(Position arg0) {}

	public void onPlayerDropped(Player arg0) {}

	public void onPlayerLeft(Player arg0) {}

	public void onReceiveText(Player arg0, String arg1) {}

	public void onSaveGame(String arg0) {}

	public void onSendText(String arg0) {}

	public void onUnitDiscover(Unit unit) {
		if (unit.getPlayer().getID() != self.getID() 
				&& !unit.getType().isNeutral()
				&& !unit.getType().isSpecialBuilding()){
			//Updates influence map
			gh.dah_map.newUnit(unit, -1, 
								unit.getTilePosition().getX(), 
								unit.getTilePosition().getY(), false);
			/////////////////////////////////////
		}
	}

	public void onUnitEvade(Unit arg0) {}

	public void onUnitHide(Unit arg0) {}

	public void onUnitMorph(Unit unit) {
		//Refinery it's an especial case.
		if (unit.getPlayer().getID() == self.getID()) {
			if (unit.getType() == UnitType.Terran_Refinery) {
				gh.remainingBuildings.add(unit.getType());
				gh.refinery++;
				gh.posBuild = null;
			}
		}
	}

	public void onUnitRenegade(Unit arg0) {}

	
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
