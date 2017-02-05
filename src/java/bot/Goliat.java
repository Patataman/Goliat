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
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.Position.PosType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType.UpgradeTypes;

public class Goliat extends Agent implements BWAPIEventListener {

	BehavioralTree CollectTree;
	Unit buildingTree;
	//InfluenceMap dah_mapa;
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
	
	@Override
	public void connected() {
	}

	@Override
	public void matchStart() {
		 
        // Mediante este metodo se puede obtener información del usuario. 
        if (Options.getInstance().getUserInput()) this.bwapi.enableUserInput();

        if (Options.getInstance().getInformation()) this.bwapi.enablePerfectInformation();
        // Mediante este método se define la velocidad de ejecución del videojuego. 
        // Los valores posibles van desde 0 (velocidad estándar) a 10 (velocidad máxima).
        this.bwapi.setGameSpeed(Options.getInstance().getSpeed());
		
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
				gh.edificiosConstruidos.add(cc);
			}
		}

		gh.supplies = bwapi.getSelf().getSupplyUsed();
		gh.totalSupplies = bwapi.getSelf().getSupplyTotal();
		//gh.addCC(gh.CCs.indexOf(gh.cc_select.getID()));
		
		gh.createMap();
		//dah_mapa = new InfluenceMap(bwapi.getMap().getSize().getBY(), bwapi.getMap().getSize().getBX());
		
		frames = 0;
		
		Selector<GameHandler> CollectResources = new Selector<>("Minerales o Vespeno");
		CollectResources.addChild(new CollectGas("Vespeno", gh));
		CollectResources.addChild(new CollectMineral("Minerales", gh));
		
		Sequence collect = new Sequence("Recolectar");
		collect.addChild(new FreeWorker("Trabajador libre", gh));
		collect.addChild(CollectResources);
		
		// -------- Secuencias de entrenamiento ---------
		
		//Entrenar VCEs
		Sequence TrainVCE = new Sequence("Entrenar VCE");
		TrainVCE.addChild(new CheckResources("Comprobar recursos vce", gh, UnitTypes.Terran_SCV));
		TrainVCE.addChild(new ChooseBuilding("Comprobar crear VCE", gh, UnitTypes.Terran_SCV));
		TrainVCE.addChild(new TrainUnit("Entrenar VCE", gh, UnitTypes.Terran_SCV, UnitTypes.Terran_Command_Center));
		//Entrenar soldados
		Sequence TrainMarine = new Sequence("Entrenar Soldado");
		TrainMarine.addChild(new CheckResources("Comprobar recursos soldado", gh, UnitTypes.Terran_Marine));
		TrainMarine.addChild(new ChooseBuilding("Comprobar entrenamiento soldado", gh, UnitTypes.Terran_Marine));
		TrainMarine.addChild(new TrainUnit("Entrenar soldado", gh, UnitTypes.Terran_Marine, UnitTypes.Terran_Barracks));
		//Entrenar medicos
		Sequence TrainMedic = new Sequence("Entrenar Medico");
		TrainMedic.addChild(new CheckResources("Comprobar recursos medico", gh, UnitTypes.Terran_Medic));
		TrainMedic.addChild(new ChooseBuilding("Comprobar entrenamiento medico", gh, UnitTypes.Terran_Medic));
		TrainMedic.addChild(new TrainUnit("Entrenar medico", gh, UnitTypes.Terran_Medic, UnitTypes.Terran_Barracks));
		//Entrenar murcielagos de fuego
		Sequence TrainFirebat = new Sequence("Entrenar Firebat");
		TrainFirebat.addChild(new CheckResources("Comprobar recursos murcielago", gh, UnitTypes.Terran_Firebat));
		TrainFirebat.addChild(new ChooseBuilding("Comprobar entrenamiento murcielago", gh, UnitTypes.Terran_Firebat));
		TrainFirebat.addChild(new TrainUnit("Entrenar murcielago", gh, UnitTypes.Terran_Firebat, UnitTypes.Terran_Barracks));
		//Entrenar goliats
		Sequence TrainGoliat = new Sequence("Entrenar Goliat");
		TrainGoliat.addChild(new CheckResources("Comprobar recursos goliat", gh, UnitTypes.Terran_Goliath));
		TrainGoliat.addChild(new ChooseBuilding("Comprobar entrenamiento goliat", gh, UnitTypes.Terran_Goliath));
		TrainGoliat.addChild(new TrainUnit("Entrenar goliat", gh, UnitTypes.Terran_Goliath, UnitTypes.Terran_Factory));
//		//Entrenar naves científicas
//		Sequence TrainVessel = new Sequence("Entrenar nave cientifica");
//		TrainVessel.addChild(new CheckResources("Comprobar recursos nave cientifica", gh, UnitTypes.Terran_Science_Vessel));
//		TrainVessel.addChild(new ChooseBuilding("Comprobar entrenamiento nave cientifica", gh, UnitTypes.Terran_Science_Vessel));
//		TrainVessel.addChild(new TrainUnit("Entrenar nave cientifica", gh, UnitTypes.Terran_Science_Vessel, UnitTypes.Terran_Starport));
		//Selector con todos los posibles entrenamientos
		Selector<Sequence> selectorTrain = new Selector<>("Selector train", TrainGoliat, TrainMedic, TrainFirebat, TrainMarine, TrainVCE);
		// ----------- FIN TRAIN ---------

		
		// -------- Secuencias de construcción ---------
		//Construir dep�sito de suministros
		Sequence buildSupply = new Sequence("Construir suministros");
		buildSupply.addChild(new CheckResources("Comprobar recursos suministros", gh, UnitTypes.Terran_Supply_Depot));
		buildSupply.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Supply_Depot));
		buildSupply.addChild(new FreeBuilder("Encontrar un constructor", gh));
		buildSupply.addChild(new Build("Construir suministros", gh, UnitTypes.Terran_Supply_Depot));
		//Construir barracones
		Sequence buildBarracks = new Sequence("Construir barracones");
		buildBarracks.addChild(new CheckResources("Comprobar recursos barracones", gh, UnitTypes.Terran_Barracks));
		buildBarracks.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Barracks));
		buildBarracks.addChild(new FreeBuilder("Encontrar un constructor", gh));
		buildBarracks.addChild(new Build("Construir barracones", gh, UnitTypes.Terran_Barracks));
		//Construir refineria
		Sequence buildRefinery = new Sequence("Construir refineria");
		buildRefinery.addChild(new CheckResources("Comprobar recursos refineria", gh, UnitTypes.Terran_Refinery));
		buildRefinery.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Refinery));
		buildRefinery.addChild(new FreeBuilder("Encontrar un constructor", gh));
		buildRefinery.addChild(new Build("Construir refineria", gh, UnitTypes.Terran_Refinery));
		//Construir bah�a de ingenieria
		Sequence buildBay = new Sequence("Construir bahia");
		buildBay.addChild(new CheckResources("Comprobar recursos bahia", gh, UnitTypes.Terran_Engineering_Bay));
		buildBay.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Engineering_Bay));
		buildBay.addChild(new FreeBuilder("Encontrar un constructor", gh));
		buildBay.addChild(new Build("Construir bahia", gh, UnitTypes.Terran_Engineering_Bay));
		//Construir academia
		Sequence buildAcademy = new Sequence("Construir academia");
		buildAcademy.addChild(new CheckResources("Comprobar recursos academia", gh, UnitTypes.Terran_Academy));
		buildAcademy.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Academy));
		buildAcademy.addChild(new FreeBuilder("Encontrar un constructor", gh));
		buildAcademy.addChild(new Build("Construir academia", gh, UnitTypes.Terran_Academy));
		//Construir fabrica
		Sequence buildFactory = new Sequence("Construir fabrica");
		buildFactory.addChild(new CheckResources("Comprobar recursos fabrica", gh, UnitTypes.Terran_Factory));
		buildFactory.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Factory));
		buildFactory.addChild(new FreeBuilder("Encontrar un constructor", gh));
		buildFactory.addChild(new Build("Construir fabrica", gh, UnitTypes.Terran_Factory));
		//Construir arsenal
		Sequence buildArmory = new Sequence("Construir arsenal");
		buildArmory.addChild(new CheckResources("Comprobar recursos arsenal", gh, UnitTypes.Terran_Armory));
		buildArmory.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Armory));
		buildArmory.addChild(new FreeBuilder("Encontrar un constructor", gh));
		buildArmory.addChild(new Build("Construir arsenal", gh, UnitTypes.Terran_Armory));
		//Construir misiles
		Sequence buildTurret = new Sequence("Construir torreta de misiles");
		buildTurret.addChild(new CheckResources("Comprobar recursos torreta", gh, UnitTypes.Terran_Missile_Turret));
		buildTurret.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Missile_Turret));
		buildTurret.addChild(new FreeBuilder("Encontrar un constructor", gh));
		buildTurret.addChild(new Build("Construir torreta", gh, UnitTypes.Terran_Missile_Turret));
		//Construir CC
		Sequence buildCC = new Sequence("Construir centro de mando");
		buildCC.addChild(new CheckResources("Comprobar recursos CC", gh, UnitTypes.Terran_Command_Center));
		buildCC.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Command_Center));
		buildCC.addChild(new FreeBuilder("Encontrar un constructor", gh));
		buildCC.addChild(new Build("Construir CC", gh, UnitTypes.Terran_Command_Center));
//		//Construir puerto estelar
//		Sequence buildStarport = new Sequence("Construir puerto estelar");
//		buildStarport.addChild(new CheckResources("Comprobar recursos puerto", gh, UnitTypes.Terran_Starport));
//		buildStarport.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Starport));
//		buildStarport.addChild(new FreeBuilder("Encontrar un constructor", gh));
//		buildStarport.addChild(new Build("Construir puerto", gh, UnitTypes.Terran_Starport));
//		//Construir laboratorio científico
//		Sequence buildLab = new Sequence("Construir laboratorio cientifico");
//		buildLab.addChild(new CheckResources("Comprobar recursos laboratorio", gh, UnitTypes.Terran_Science_Facility));
//		buildLab.addChild(new FindPosition("Encontrar posicion", gh, UnitTypes.Terran_Science_Facility));
//		buildLab.addChild(new FreeBuilder("Encontrar un constructor", gh));
//		buildLab.addChild(new Build("Construir laboratorio", gh, UnitTypes.Terran_Science_Facility));
		
		Selector<Sequence> selectorBuild = new Selector<>("Selector build", buildSupply, buildBarracks, 
																buildAcademy, buildRefinery, buildBay, buildTurret, buildCC, buildFactory, buildArmory);
		// ---------- FIN BUILD -----------
		
		// -------- Secuencias de movimiento ---------
		Sequence adventure = new Sequence("Mover unidades");
		adventure.addChild(new CheckPositionUnits("Comprobar posici�n de las unidades", gh));
		adventure.addChild(new ChosseUnits("Formar patrulla", gh));
		adventure.addChild(new ChooseDestination("Escoger destino", gh));
		adventure.addChild(new SendUnits("Mandar patrulla", gh));
		// ---------- FIN MOVE -----------
		
		// -------- Secuencias de ataque ---------
		Sequence attack = new Sequence("Mandar de ataque a las tropas");
		attack.addChild(new CheckStateUnits("Comprobar estado de las unidades", gh));
		attack.addChild(new ChosseTropa("Formar tropa", gh));
		attack.addChild(new ChooseDestination("Escoger destino", gh));
		attack.addChild(new ChooseVictim("Escoger v�ctima", gh));
		attack.addChild(new SendAttack("Mandar ataque", gh));
		// ---------- FIN ATTACK -----------
		
		// ---------- Secuencias investigación --------
		//Investigar U238 (Academia)
		Sequence u238 = new Sequence("Investigar U238");
		u238.addChild(new CheckResearch("Comprobar si se puede investigar", gh, UpgradeTypes.U_238_Shells));
		u238.addChild(new Research("Investigar", gh, UnitTypes.Terran_Academy, UpgradeTypes.U_238_Shells));
		//Investigar mejora curar medicos (Academia)
		Sequence caudecus = new Sequence("Investigar caudecus");
		caudecus.addChild(new CheckResearch("Comprobar si se puede investigar", gh, UpgradeTypes.Caduceus_Reactor));
		caudecus.addChild(new Research("Investigar", gh, UnitTypes.Terran_Academy, UpgradeTypes.Caduceus_Reactor));
		//Investigar mejora de armadura (infanteria)(Bahia)
		Sequence armor = new Sequence("Investigar medicos");
		armor.addChild(new CheckResearch("Comprobar si se puede investigar", gh, UpgradeTypes.Terran_Infantry_Armor));
		armor.addChild(new Research("Investigar", gh, UnitTypes.Terran_Engineering_Bay, UpgradeTypes.Terran_Infantry_Armor));
		//Investigar mejora de armamento (infanteria)(Bahia)
		Sequence weapons = new Sequence("Investigar medicos");
		weapons.addChild(new CheckResearch("Comprobar si se puede investigar", gh, UpgradeTypes.Terran_Infantry_Weapons));
		weapons.addChild(new Research("Investigar", gh, UnitTypes.Terran_Engineering_Bay, UpgradeTypes.Terran_Infantry_Weapons));
		
		Selector<Sequence> selectorResearch = new Selector<>("Selector research", u238, caudecus, armor, weapons); 
		// --------------- FIN RESEARCH ---------------
		
		// ----------- Secuencia de reparación ---------		
		Sequence repair = new Sequence("Reparación");
		repair.addChild(new FindDamageBuildings("Encontrar edificios da�ados", gh));
		repair.addChild(new FreeBuilder("Trabajador libre", gh));
		repair.addChild(new RepairBuilding("Reparar el edificio", gh));
		// ------------- FIN REPAIR --------------------
		
		CollectTree = new BehavioralTree("Arbol maravilloso");
		CollectTree.addChild(new Selector<>("MAIN SELECTOR", collect, selectorBuild, repair, selectorTrain, selectorResearch, adventure, attack));
		
		
	}

	@Override
	public void matchFrame() {
		CollectTree.run();
		if(frames < 300){ // Cada 300 frames se recalculan las influencias.
			frames++;
		}else{
			frames = 0;
			gh.updateInfluences();
		}
		
	}


	@Override
	public void unitCreate(int unitID) {
		//Cuando se comienza a construir un edificio se pone como pendiente.
		if (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) {
			if (bwapi.getUnit(unitID).getType().isBuilding()){
				gh.edificiosPendientes.add(bwapi.getUnit(unitID).getType());
			}
		}
	}
	

	@Override
	public void unitDestroy(int unitID) {
		gh.dah_mapa.removeUnitDead(unitID);
		Predicate<Unit> predicado = new Predicate<Unit>() {
			public boolean test(Unit u) {
				return u.getID() == unitID;
				
			}
		};
		int control = 0;
		if (control == 0) {
			//Casting a array de unidades (?)
			for(Object u : gh.edificiosConstruidos.stream().filter(predicado).toArray()) {
				//No es necesario comprobar el ID ya que la sublista que se recorre es la que cumple lo del ID
				//Aunque sólo debería haber 1 elemento
				gh.edificiosConstruidos.remove(u);
				if (((Unit) u).getType() == UnitTypes.Terran_Academy) gh.academia--;
				if (((Unit) u).getType() == UnitTypes.Terran_Barracks) gh.barracones--;
				if (((Unit) u).getType() == UnitTypes.Terran_Factory) gh.fabricas--;
				if (((Unit) u).getType() == UnitTypes.Terran_Engineering_Bay) gh.bahia--;
				if (((Unit) u).getType() == UnitTypes.Terran_Armory) gh.arsenal--;
				if (((Unit) u).getType() == UnitTypes.Terran_Refinery) gh.refineria--;
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
			for(Object u : gh.unidadesMilitares.stream().filter(predicado).toArray()) {
				gh.supplies -= ((Unit) u).getType().getSupplyRequired();
				gh.unidadesMilitares.remove((Unit) u);
				control++;
			}
			for(Object u : gh.soldadosAburridos.stream().filter(predicado).toArray()) {
				gh.supplies -= ((Unit) u).getType().getSupplyRequired();
				gh.soldadosAburridos.remove((Unit) u);
				control++;
			}
			for(Object u : gh.tropaAsalto.stream().filter(predicado).toArray()) {
				gh.supplies -= ((Unit) u).getType().getSupplyRequired();
				gh.tropaAsalto.remove((Unit) u);
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
			for(ArrayList<Integer> minerales : gh.trabajadoresMineral) {
				if (minerales.contains((Integer) unitID)) {
					gh.trabajadoresMineral.get(gh.trabajadoresMineral.indexOf(minerales)).remove((Integer) unitID);
					control++;
				}
			}
		}
		if (control == 0) {
			for(ArrayList<Integer> vespeno : gh.trabajadoresVespeno) {
				if (vespeno.contains((Integer) unitID)) {
					gh.trabajadoresVespeno.get(gh.trabajadoresVespeno.indexOf(vespeno)).remove((Integer) unitID);
					//Ultimo if, no hay necesidad de control++
				}
			}
		}
	}
	
	@Override
	public void unitComplete(int unitID) {
		//Se actualiza el mapa de ingluencias
		int influencia = (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) ? 1 : -1;
		gh.dah_mapa.newUnit(this.bwapi.getUnit(unitID), influencia, 
				this.bwapi.getUnit(unitID).getPosition().getBX(), this.bwapi.getUnit(unitID).getPosition().getBY());
		/////////////////////////////////////
		
		//Sección de código para escribir en un fichero el mapa y verificar que se crea bien.
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
			if (gh.unidadesPendientes.contains(bwapi.getUnit(unitID).getType())){
				gh.unidadesPendientes.remove(bwapi.getUnit(unitID).getType());
				gh.supplies += bwapi.getUnit(unitID).getType().getSupplyRequired();
				//Los terran sólo poseen 1 unidad no militar, los VCEs.
				if (bwapi.getUnit(unitID).getType() != UnitTypes.Terran_SCV) {
					gh.soldadosAburridos.add(bwapi.getUnit(unitID));
				}
			}
			//Cuando se cree un edificio pendiente, se elimina de la lista y se pone como construido
			if (gh.edificiosPendientes.contains(bwapi.getUnit(unitID).getType())) {
				gh.edificiosPendientes.remove(bwapi.getUnit(unitID).getType());
				gh.edificiosConstruidos.add(bwapi.getUnit(unitID));
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Supply_Depot)
					gh.totalSupplies += UnitTypes.Terran_Supply_Depot.getSupplyProvided();
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Barracks) gh.barracones++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Engineering_Bay) gh.bahia++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Academy) gh.academia++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Factory) gh.fabricas++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Armory) gh.arsenal++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Science_Facility) gh.lab_cient++;
				if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Command_Center) {
					if (gh.CCs.indexOf((Integer) unitID) == -1){
						gh.CCs.add(unitID);
						gh.addCC(gh.CCs.indexOf(unitID));
					}
				}
				gh.worker = null;
				
				//Se actualiza el mapa.
				gh.updateMap(bwapi.getUnit(unitID).getTopLeft(),
						new Position(bwapi.getUnit(unitID).getTopLeft().getBX()+bwapi.getUnit(unitID).getType().getTileWidth(),
									bwapi.getUnit(unitID).getTopLeft().getBY()+bwapi.getUnit(unitID).getType().getTileHeight(),
									PosType.BUILD));
				
				//Sección de código para escribir en un fichero el mapa y verificar que se crea bien.
//				String workingDirectory = System.getProperty("user.dir");
//				String path = workingDirectory + File.separator + "mapa.txt";
//				createANDwrite(path);
			}
		}
	}

	@Override
	public void unitMorph(int unitID) {
		if (bwapi.getUnit(unitID).getPlayer().getID() == bwapi.getSelf().getID()) {
			if (bwapi.getUnit(unitID).getType() == UnitTypes.Terran_Refinery) gh.refineria++;
		}
	}
	
	@Override
	public void playerDropped(int playerID) { }
	
	@Override
	public void matchEnd(boolean winner) { }
	
	@Override
	public void keyPressed(int keyCode) { }
	
	@Override
	public void sendText(String text) { }
	
	@Override
	public void receiveText(String text) { }
	
	@Override
	public void playerLeft(int playerID) { }
	
	@Override
	public void nukeDetect(Position p) { }
	
	@Override
	public void nukeDetect() { }
	
	@Override
	public void unitDiscover(int unitID) { }
	
	@Override
	public void unitEvade(int unitID) { }
	
	@Override
	public void unitShow(int unitID) { }
	
	@Override
	public void unitHide(int unitID) { }
	
	@Override
	public void unitRenegade(int unitID) { }

	@Override
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
		double mydah_mapa[][] = gh.dah_mapa.getmap();
		try {
			Path p = Paths.get(path);
			Charset charset = Charset.forName("UTF-8");
			//Por defecto trae CREATE y TRUNCATE
			BufferedWriter writer = Files.newBufferedWriter(p, charset);
			for(int f = 0; f < mydah_mapa.length; f++){
				for (int c=0; c < mydah_mapa[f].length; c++){			
					writer.write(mydah_mapa[f][c]+";");
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
			for(int f = 0; f < gh.mapa.length; f++){
				for (int c=0; c < gh.mapa[f].length; c++){
					if (gh.mapa[f][c] == -1){
						writer.write("M;");
					}
					else if (gh.mapa[f][c] == -2){
						writer.write("V;");
					}
					else if (gh.mapa[f][c] < 10){
						writer.write("0"+gh.mapa[f][c]+";");
					} 
					else {						
						writer.write(gh.mapa[f][c]+";");
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
