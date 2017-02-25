package bot;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.iaie.btree.util.GameHandler;

import jnibwapi.BaseLocation;
import jnibwapi.ChokePoint;
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.Position.PosType;
import jnibwapi.Region;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType;

public class JohnDoe extends GameHandler {
	
	//Listas de control internas
	List<Integer> CCs;				//Lista para llevar el conteo de los CCs
	List<ArrayList<Unit>> VCEs;		//Lista para llevar el conteo de los VCEs de cada CC
	List<ArrayList<Integer>> trabajadoresMineral; //Lista para llevar el conteo de los VCEs que recolectan mineral de cada CC
	List<ArrayList<Integer>> trabajadoresVespeno; //Lista para llevar el conteo de los VCEs que recolectan vespeno de cada CC
	List<UnitType> unidadesPendientes; 	//Lista para llevar el conteo de las unidades entrenandose en este momento
	List<Unit> unidadesMilitares;		//Lista para llevar de las unidades militares que tienen algo asignado.
	List<Unit> soldadosAburridos;		//Lista para llevar el conteo de todas las unidades militares entrenadas
	List<Unit> tropaAsalto;				//Lista para saber cuales son los valientes que han ido a la guerra
	List<UnitType> edificiosPendientes;	//Lista para llevar el conteo de los edificios construyendose.
	List<Unit> edificiosConstruidos; 	//Lista para saber los edificios construidos actualmente
	List<UpgradeType> researching;		//Lista para saber las investigaciones que se estén realizando.
	List<Unit> damageBuildings;			//Lista para saber los edificios que han sido atacados 
										//y poder repararlos. De esta forma no hay que recorrer toda edificiosConstruidos
	List<Unit> workers;					//Lista para tener hasta 3 trabajadores construyendo o haciendo cosas
	Unit current_worker;				//Variable para saber que VCE se usa de la lista
		
	int supplies, totalSupplies;
	byte barracones, refineria, fabricas, 
		academia, arsenal, bahia, max_vce, lab_cient, puerto, number_chokePoints;
	
	List<ChokePoint>[][] chokePoints;
	
	//Variables para tener controlado el cc inicial
	//y el cc del vce seleccionado.
	Unit cc, cc_select;
	
	//Posición donde se va a construir el último edificio.
	Position posBuild;
	//Posición a la que mandar una patrulla.
	Position destination;
	Position objetivo;
	
	Position tramo1;
//	Position tramo2;
	
	int[][] mapa;
	
	InfluenceMap dah_mapa;

	public JohnDoe(JNIBWAPI bwapi) {
		super(bwapi);

		workers 				= new ArrayList<Unit>(3);
		cc 						= null;
		cc_select 				= null;
		CCs 					= new ArrayList<Integer>();
		VCEs 					= new ArrayList<ArrayList<Unit>>();
		trabajadoresMineral 	= new ArrayList<ArrayList<Integer>>();
		trabajadoresVespeno 	= new ArrayList<ArrayList<Integer>>();
		unidadesPendientes 		= new ArrayList<UnitType>();
		unidadesMilitares		= new ArrayList<Unit>();
		soldadosAburridos		= new ArrayList<Unit>();
		tropaAsalto				= new ArrayList<Unit>();
		edificiosPendientes 	= new ArrayList<UnitType>();
		edificiosConstruidos 	= new ArrayList<Unit>();
		researching				= new ArrayList<UpgradeType>();
		damageBuildings			= new ArrayList<Unit>();
		barracones = refineria = fabricas = 
		academia = arsenal = bahia = lab_cient = puerto = 0;
		number_chokePoints 		= (byte) this.connector.getMap().getRegion(this.connector.getSelf().getStartLocation()).getChokePoints().size();
		max_vce = 20;
		dah_mapa 				= new InfluenceMap(bwapi.getMap().getSize().getBY(), bwapi.getMap().getSize().getBX());
	}
	
	//Añaden las listas correspondientes al nuevo CC
	public void addCC(int cc_pos) {
		VCEs.add(new ArrayList<Unit>());
		trabajadoresMineral.add(new ArrayList<Integer>());
		trabajadoresVespeno.add(new ArrayList<Integer>());
	}
	
	//Obtiene un trabajador que se encuentra libre
	//Un trabajador está libre cuando no recolecta mineral/vespeno o no hace nada con su vida
	public boolean getWorker() {
		for (ArrayList<Unit> vces_cc : VCEs) {
			for (Unit vce : vces_cc) {
				// Se comprueba si la unidades es de tipo VCE y no esté ocupada
				if ((!trabajadoresMineral.get(VCEs.indexOf(vces_cc)).contains(vce.getID()) &&
					 !trabajadoresVespeno.get(VCEs.indexOf(vces_cc)).contains(vce.getID())) &&
					 vce.isIdle() && vce.isCompleted() && CCs.size() > 0) {
					current_worker = vce;
					cc_select = this.connector.getUnit(CCs.get(VCEs.indexOf(vces_cc)));
					return true;
				}
			}
			
		}
		return false;
	}
	
	//Obtiene un trabajador para construir. Es diferente a obtener un trabajador, porque aqui cogemos un VCE
	//que esté recolectando minerales. Se construye siempre con un VCE del cc inicial.
	public boolean getMasterBuilder() {
		if (workers.size() < 3){
			//Si todos los actuales están ocupados coge otro VCE nuevo
			for (Unit vce : workers) {
				if (!vce.isConstructing()){
					return true;
				}
			}
			//Se coge 1 VCE de la lista de VCEs del CC inicial (0)
			for (int vce : this.trabajadoresMineral.get(0)) {
				//Si no está en la lista...
				if (!workers.contains(this.connector.getUnit(vce))) {
					//Se pone como trabajador
					workers.add(this.connector.getUnit(vce));
					//Se elimina de la lista
//					trabajadoresMineral.get(0).remove((Integer) vce);
					return true;					
				}
			}
			//No se ha podido seleccionar ninguno
			return false;
		}
		else if (workers.size() <= 3){
			for (Unit vce : workers) {
				if (!vce.isConstructing()){
					return true;
				}
			}
			return false;
		}
		//Si se llega a else es que ya hay 3 trabajadores
		else {return false;}
	}
	
	//Se manda a recolectar minerales al trabajador seleccionado, 
	// ya que antes de llamar a esta función se llama a getWorker
	public boolean aCurrarMina(){
		//Se verifica que no se pase del número de trabajadores y que el VCE está
		//completado, ya que a veces se selecciona sin haber completado el entrenamiento.
		if ((trabajadoresMineral.get(CCs.indexOf(cc_select.getID())).size() < max_vce-2) && current_worker.isCompleted()){
			//Se buscan los minerales cercanos a la base.
			for (Unit recurso : this.connector.getNeutralUnits()) {
				if (recurso.getType().isMineralField()) {
					if (this.connector.getMap().getRegion(recurso.getPosition()) == 
							this.connector.getMap().getRegion(cc_select.getPosition())) {
						//Se manda al VCE a recolectar
						this.connector.getUnit(current_worker.getID()).rightClick(recurso, false);
						trabajadoresMineral.get(CCs.indexOf(cc_select.getID())).add(current_worker.getID());
						current_worker = null;
						return true;
					}
				}
			}	
		}
		return false;
	}
	
	//Igual que los minerales
	public boolean aCurrarGas(){
		if (trabajadoresVespeno.get(CCs.indexOf(cc_select.getID())).size() < 2 && current_worker.isCompleted()) {
			for (Unit refineria : this.connector.getMyUnits()) {
				if (refineria.getType() == UnitTypes.Terran_Refinery && refineria.isCompleted()){
					this.connector.getUnit(current_worker.getID()).rightClick(refineria, false);
					trabajadoresVespeno.get(CCs.indexOf(cc_select.getID())).add(current_worker.getID());
					current_worker = null;
					return true;	
				}
			}			
		}
		return false;
	}
	
	//Se comprueba si se posee más mineral y gas que el pasado por par�metro
	public boolean checkResources(int mineral, int gas){
		if (this.connector.getSelf().getMinerals() >= mineral &&
				this.connector.getSelf().getGas() >= gas){
			return true;
		}
		return false;
	}
	
	//Se comprueba si se puede construir una unidad
	public boolean canTrain(UnitType unidad) {
		if (this.connector.canMake(unidad)) {
			return true;
		}
		return false;

	}
	
	//Entrena una unidad 
	public boolean trainUnit(UnitType edificio, UnitType unidad){
		for (Unit u : edificiosConstruidos){
			if (u.getType() == edificio && !u.isTraining()){
				u.train(unidad);
				unidadesPendientes.add(unidad);
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
	
	//Construye un edificio
	public boolean buildUnit(UnitType edificio) {
		if (edificiosPendientes.contains(edificio)) {
			return false;
		}
		for (Unit vce : workers){
			if (vce.getPosition().getApproxBDistance(posBuild) < 10){
				return vce.build(posBuild, edificio);
			}
		}
		return false;
	}
	
	//Comprueba si se puede investigar la investigación (valga la redundancia)
	public boolean checkResearch(UpgradeType res) {
		if (this.researching.contains(res)) {
			return false;
		}
		if (this.connector.canUpgrade(res)){
			return true;
		}
		return false;
	}
	
	//Realiza la investigación
	public boolean doResearch(UnitType building, UpgradeType res) {
		for (Unit u : edificiosConstruidos) {
			if (u.getType() == building) {
				return u.upgrade(res);
			}
		}
		return false;
	}
	
	// Comprueba la posición de las unidades
	public boolean checkPositionUnits(){
		double myInfluence = this.dah_mapa.getMyInfluenceLevel();
		double pointInfluence;
		if(!this.connector.getEnemyUnits().isEmpty()){ // Nos atacan, no es momento para formar patrullas
			for(Unit victima : this.connector.getEnemyUnits()){
				if(victima.isExists() && victima.getDistance(cc)<500){ // 500 es un buen radio de defensa
					return false;
				}
			}
		}
		for (Unit u : soldadosAburridos){ // Si faltan unidades en los choke, las mandamos
			if(u.isIdle() && u.isCompleted()){				
				Region p = this.connector.getMap().getRegion(cc.getPosition());
				for(ChokePoint a : p.getChokePoints()){
					pointInfluence = this.dah_mapa.getInfluence(new Point(a.getCenter().getBX(), a.getCenter().getBY()));
					if(pointInfluence < (myInfluence*0.3)/p.getChokePoints().size()){ //Un tercio de la influencia debe defender los chokes
						return true;
					}
				}
			}
		}	
		return false;
	}
		
	// Selecciona las unidades militares que no hacen nada para ponerlas a hacer algo
	public boolean chosseUnits(){
		ArrayList<Unit> aux = new ArrayList<Unit>();
		for (Unit u : soldadosAburridos){
			if(u.isIdle() && !unidadesMilitares.contains(u)){
				unidadesMilitares.add(u);
				aux.add(u);
			}
		}
		for (Unit u : aux){
			soldadosAburridos.remove(u);
		}
		if(!unidadesMilitares.isEmpty()){
			return true;
		}
		return false;
	}
	
	// Elige el destino de las unidades
	public boolean chooseDestination(){
		double myInfluence = this.dah_mapa.getMyInfluenceLevel();
		double pointInfluence;
		Region p = this.connector.getMap().getRegion(cc.getPosition());
		for(ChokePoint a : p.getChokePoints()){
			pointInfluence = this.dah_mapa.getInfluence(new Point(a.getCenter().getBX(), a.getCenter().getBY()));
			if(pointInfluence < (myInfluence*0.3)/p.getChokePoints().size()){ //Un tercio de la influencia debe defender los chokes
				destination = a.getCenter();
				if(a.getFirstSide().getWDistance(cc.getPosition()) > a.getSecondSide().getWDistance(cc.getPosition())){
//					tramo2 = a.getFirstSide();
					tramo1 = a.getSecondSide();
				}else{
					tramo1 = a.getFirstSide();
//					tramo2 = a.getSecondSide();
				}
				return true;
			}
		}
		return false; // No hay choke al que mandar
	}
	
	// Mandar las unidades a la posicion destino
	public boolean sendUnits(){
		for(Unit soldadito : unidadesMilitares){
			if(soldadito.isIdle()){ // Solo mandamos a la unidad que este parada 
				if (soldadito.getDistance(destination) > 50) {
					soldadito.move(destination.makeValid(), false);					
				} else {
					soldadito.move(destination, false);
				}
			}
		}
		return true;
	}
	
	// Comprueba el estado de las unidades
	public boolean checkStateUnits(){
		for (Unit u : unidadesMilitares){
			if(u.isCompleted() && u.isIdle()){
				return true;
			}
		}
		return false;
	}
	
	// Selecciona las unidades militares para formar una tropa de asalto.
	public boolean chosseTropa(){
		for (Unit u : unidadesMilitares){
			if(u.isIdle() && !tropaAsalto.contains(u)){
				//Esta lista tiene sentido para en el futuro poder crear subgrupos
				tropaAsalto.add(u);
			}
		}
		if(tropaAsalto.size() > 15){
			return true;
		}
		return false;
	}
	
	// Elige el el lugar o unidad a la que atacar
	public boolean chooseVictim(){
		for(Unit victima : this.connector.getEnemyUnits()){ // Nos atacan, es el momento de defender
			if(victima.isExists() && victima.getDistance(cc)<500){ // 500 es un buen radio de defensa
				objetivo = victima.getPosition();
				return true;
			}
		}
		objetivo = getPosToAttack(); //Se obtiene la posición objetivo
		return true;
	}
	
	// Mandar patrulla a la posicion destino
	public boolean sendAttack(){
		for(Unit soldadito : tropaAsalto){
			if(!soldadito.isAttacking()){
				soldadito.attack(objetivo, true);
			}
		}
		
		return false;
	}
	
	
	/** Para construir vamos a coger como origen el CC. Y se realizarán hasta 10 intentos
	 * para encontrar una posición válida. En cada intento se va cambiando de posicion maxima.
	 * (es decir, se va moviendo alrededor para buscar la posición)
	 */
	public boolean findPosition(UnitType edificio) {
		//Si ya se tiene una posición, no la vuelve a buscar
		if (posBuild != null && this.connector.canBuildHere(posBuild, edificio, false)){
			return true;
		}
		//Caso especial de que sea una refinería
		if (edificio == UnitTypes.Terran_Refinery) {
			return findPositionRefinery();
		}
		//Caso especial de que sea una expansión
		if (edificio == UnitTypes.Terran_Command_Center) {
			return findPositionCC();
		}
		//Caso de edificios más o menos importantes (para alejarlos de la entrada)
		if (edificio == UnitTypes.Terran_Barracks ||
				edificio == UnitTypes.Terran_Factory ||
				edificio == UnitTypes.Terran_Starport) {
			return findPositionAwayCP(edificio);
		}
		//Edificios no especiales
		//Para hacer bulto se construye entre el CP y el CC
		if (number_chokePoints == 1) {
			//Solo se ejecuta 1 vez
			for (ChokePoint cp : this.connector.getMap().getRegion(cc_select.getPosition()).getChokePoints()){
				Position cp_position = cp.getCenter();
				byte x, y;
				//Si el chokePoint está a la izq del CC
				x = (cc_select.getPosition().getBX() < cp_position.getBX()) ? (byte)1 : (byte)-1;
				//Si el chokePoint está arriba del CC
				y = (cc_select.getPosition().getBY() < cp_position.getBY()) ? (byte)1 : (byte)-1;
				byte [][] pruebas = {{x,0},{x,y},{0,y},{(byte)(-1*x), (byte)(-1*y)},{(byte)(-1*x), 0},{0, (byte)(-1*y)}};
				for (int j=0; j<pruebas.length; j++) {
					for (int i=1; i<4; i++){
						//Point origen, Point maximo, UnitType building
						Position pos = findPlace(new Point(cc_select.getPosition().getBX(), cc_select.getPosition().getBY()),
								new Point((cc_select.getPosition().getBX()+pruebas[j][0]*edificio.getTileWidth()*i),
										(cc_select.getPosition().getBY()+pruebas[j][1]*edificio.getTileHeight()*i)),
											edificio);
						//Si la posición es válida...
						if (this.connector.canBuildHere(pos, edificio, true)){
							posBuild = pos;
							return true;
						}				
					}
				}
			}
		} else {
			
		}
		//No se encuentra nada
		return false;
	}
	
	public boolean findPositionRefinery() {
		//Caso especial de que sea una refinería
		for (Unit vespeno : this.connector.getNeutralUnits()){
			//Se construirá la refinería si está en la misma región que el CC
			if (vespeno.getType() == UnitTypes.Resource_Vespene_Geyser &&
					this.connector.getMap().getRegion(vespeno.getPosition()) ==
					this.connector.getMap().getRegion(cc_select.getPosition())) {                              
					//Se obtiene la pos. del vespeno.
					posBuild = vespeno.getTopLeft();
					return true;
			}
		}
		//No se encuentra posición para una refinería, asique fuera
		return false;
	}
	
	public boolean findPositionCC() {
		//Nos quedamos con la expansión más cercana a la base
		int dist = 9999;
		BaseLocation pos = null;
		for (BaseLocation aux : this.connector.getMap().getBaseLocations()) {
			//Se comprueba que no sean la misma posición, que la distancia 
			//sea menor que la anterior y que se pueda construir
			if (!aux.isStartLocation() &&
					cc.getPosition().getApproxWDistance(aux.getCenter()) < dist &&
					this.connector.canBuildHere(aux.getPosition(), UnitTypes.Terran_Command_Center, false) &&
					!(aux.isIsland() || aux.isMineralOnly())) {
				//Si es mas cercano se actualiza la distancia
				dist = cc.getPosition().getApproxWDistance(aux.getCenter());
				//Se guarda
				pos = aux;
			}
			//Ha encontrado una posición
			if (pos != null) {
				posBuild = pos.getPosition();
				return true;
			}
			//No ha encontrado posición
			else { return false; }
		}
		//No se encuentra para un CC, asique fuera
		return false;
	}
	
	public boolean findPositionAwayCP(UnitType edificio) {
		//Se construyen alejados, Barracas, Fabricas y Puertos estelares
		//Conocer numeros de CP.
		//Si sólo hay 1 construir lo más alejados del CP
		//Si hay varios, construir lo más pegado al CC y alejado del CP
		if (number_chokePoints == 1) {
			//Solo se ejecuta 1 vez
			for (ChokePoint cp : this.connector.getMap().getRegion(cc_select.getPosition()).getChokePoints()){
				Position cp_position = cp.getCenter();
				byte x, y;
				//Si el chokePoint está a la izq del CC
				x = (cc_select.getPosition().getBX() > cp_position.getBX()) ? (byte)1 : (byte)-1;
				//Si el chokePoint está arriba del CC
				y = (cc_select.getPosition().getBY() > cp_position.getBY()) ? (byte)1 : (byte)-1;
				byte [][] pruebas = {{0,y},{x,y},{x,0},{(byte)(-1*x), 0},{0, (byte)(-1*y)},{(byte)(-1*x), (byte)(-1*y)}};
				for (int j=0; j<pruebas.length; j++) {
					for (int i=0; i<4; i++){
						//Point origen, Point maximo, UnitType building
						Position pos = findPlace(new Point(cc_select.getPosition().getBX(), cc_select.getPosition().getBY()),
								new Point((cc_select.getPosition().getBX()+pruebas[j][0]*edificio.getTileWidth()*i),
										(cc_select.getPosition().getBY()+pruebas[j][1]*edificio.getTileHeight()*i)),
										edificio);
						//Si la posición es válida...
						if (this.connector.canBuildHere(pos, edificio, false)){
							posBuild = pos;
							return true;
						}				
					}
				}
			}
		} else {
			
		}
		return false;
		
	}
	
	public void updateInfluences(){
		this.dah_mapa.updateMap(this.connector);
	}
	
	/**
	 * Calcula la posición que habría que atacar.
	 * Tiene en cuenta la influencia y la distancia a la base.
	 * Se le da menor importancia a la influencia, ya que mientras
	 * mas cerca, mayor importancia hay que darle.
	 * @return Posición a la que atacar
	 */
	public Position getPosToAttack() {
		ArrayList<int[]> posiciones = dah_mapa.getEnemyPositions(); //Posiciones enemigas
		Position ret = new Position(posiciones.get(0)[1], posiciones.get(0)[0], PosType.BUILD); //Posición por defecto
		double infl = dah_mapa.mapa[posiciones.get(0)[0]][posiciones.get(0)[1]]; //Influencia por defecto
		int dist = cc.getPosition().getApproxWDistance(ret); //Distancia inicial
		
		for (int[] i : posiciones) {
			Position aux = new Position(i[1], i[0], PosType.BUILD);
			if (dah_mapa.mapa[i[0]][i[1]] < infl*1.5 && cc.getPosition().getApproxWDistance(aux) < dist) {
				//se actualizan los valores
				dist = cc.getPosition().getApproxBDistance(aux);
				ret = aux;
				infl = dah_mapa.mapa[i[0]][i[1]];
			}
		}
		return ret;
	}
	
	/**
	 * Se comprueba si hay edificios dañados
	 * @return True si hay edificio, false si no
	 */
	public boolean checkBuildings() {
		//Para ahorrar ciclos, si la lista de edificios dañados contiene algo
		//se evita el mirar todos los edificios.
		if (!damageBuildings.isEmpty()) {
			return true;
		}
		for (Unit u : edificiosConstruidos){
			//Si el edificio ha sido dañado y no se está reparando ni guardado
			if (u.getHitPoints() - u.getType().getMaxHitPoints() != 0 &&
					!u.isRepairing() && !damageBuildings.contains(u)) {
				damageBuildings.add(u);
			}
		}
		if (damageBuildings.isEmpty()) {
			return false;
		}
		return true;
	}
	
	public boolean repair() {
		for (Unit vce : workers){
			if (vce.isIdle()) {
				boolean ret = vce.repair(damageBuildings.get(0), false);
				//Si se va a reparar, se elimina de la lista.
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
    	//Position pos_aux = new Position(0,0, PosType.BUILD);
    	//Altura máxima del mapa en pixeles (Build)
		int maxHeight = this.connector.getMap().getSize().getBY();
		//Anchura máxima del mapa en pixeles (Build)
		int maxWidth = this.connector.getMap().getSize().getBX();
		//Altura de la casilla actual
		//int altura = this.connector.getMap().getGroundHeight(pos_aux);
		//Mapa a devolver
		mapa = new int[maxHeight][maxWidth];
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
				if (f+1>=maxHeight ||
					c+1>=maxWidth ||
					!this.connector.getMap().isBuildable(pos_aux)){ zonaLibre = false; }
				
				//Se obtiene la altura de la posición
				int altura = this.connector.getMap().getGroundHeight(pos_aux);
				while(zonaLibre && dimension <= 4){
					dimension++;
					//Se verifica vertical, horizontal y diagonalmente si son válidas las posiciones.
					//Si alguna no lo es, se sale del while y se guarda el valor en el mapa
					for(int i = 0; i < dimension; i++){
						//matriz[i+f][c+dimension]	Comprueba columnas
						if (this.connector.isBuildable(new Position(c+dimension, f+i, PosType.BUILD), true)){ // �Es construible?
							if(this.connector.getMap().getGroundHeight(new Position(c+dimension, f+i, PosType.BUILD)) != altura){ // �Est�n a diferente altura?
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
				mapa[f][c] = (dimension);	
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
				mapa[u.getTilePosition().getBY()][u.getTilePosition().getBX()] = -1;
			}
			if (u.getType() == UnitTypes.Resource_Vespene_Geyser) {
				//Para construir la refinería nos vale la casilla arriba a la izquierda.
				mapa[u.getTilePosition().getBY()][u.getTilePosition().getBX()] = -2;
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
    	if (origen.x == maximo.x && maximo.x + building.getTileWidth() < mapa[0].length) {
    		maximo.x += building.getTileWidth();
    	}
    	if (origen.y == maximo.y && maximo.y + building.getTileHeight() < mapa.length) {
    		maximo.y += building.getTileHeight();
    	}
    	
    	//Limites de la submatriz X e Y
    	//Eje X
    	if (origen.x < maximo.x) {
    		//Origen está antes que el maximo
    		xMaximo = (maximo.x > mapa[0].length ? mapa[0].length : maximo.x);
    		xOrigen = origen.x;
    	} else {
    		//Maximo está antes que el origen
    		xMaximo = (origen.x > mapa[0].length ? mapa[0].length : origen.x);
    		xOrigen = maximo.x;
    	}
    	//Lo mismo con el eje Y
    	if (origen.y < maximo.y) {
    		yMaximo = (maximo.y > mapa.length ? mapa.length : maximo.y);
    		yOrigen = origen.y;
    	} else {
    		yMaximo = (origen.y > mapa.length ? mapa.length : origen.y);
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
    			if (mapa[y][x] >= max) {return new Position(x, y, PosType.BUILD);}
    			//Este bucle busca en vertical hacia abajo
    			for (int y2=0; y2<=(x-xOrigen) && y+y2 < mapa.length;y2++) {
    				if (mapa[y+y2][x] >= max) {
    					return new Position(x, y+y2, PosType.BUILD);
    				}
    			}
    			//Este bucle mira desde la y que se quedó el bucle anterior y va horizontalmente hacia atrás
    			for (int x2=0; x2<=(x-xOrigen) && y+(x-xOrigen) < mapa.length;x2++) {
    				if (mapa[y+(x-xOrigen)][x-x2] >= max) {
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
     * La posici�n origen ha sido obtenida mediante el m�todo findPlace y la posici�n
     * destino ha sido calculada con el tama�o del edificio + la posici�n origen
     */
    public void updateMap(Position origen, Position destino) {
    	if (destino.getBX() > mapa[0].length) {
    		if (destino.getBY() > mapa.length) {
    			destino = new Position(mapa[0].length-1,mapa.length-1);
    		} else {
    			destino = new Position(mapa[0].length-1,destino.getBY());
    		}
    	}
    	//se recorre la matriz entre las posiciones dadas
    	for (int i = origen.getBY(); i < destino.getBY(); i++){
    		for(int j = origen.getBX(); j < destino.getBX(); j++){
    			//se ponen como ocupadas las casillas
    			mapa[i][j] = 0;
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
    		if (((origen.getBY()-iv >= 0 && destino.getBX()-ih >= 0) && mapa[origen.getBY()-iv][destino.getBX()-ih] > iv) && (iv+extra < 4)){ // Si llegamos a 4 no es necesario seguir
    			mapa[origen.getBY()-iv][destino.getBX()-ih] = (iv == 1 ? iv+extra : iv);
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
    		if (((origen.getBY()+iv >= 0 && origen.getBX()-ih >= 0) && mapa[origen.getBY()+iv][origen.getBX()-ih] > ih) && (ih < 4)){ // Si llegamos a 4 no es necesario seguir
    			mapa[origen.getBY()+iv][origen.getBX()-ih] = ih;
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
