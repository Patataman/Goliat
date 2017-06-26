package bot;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import bwapi.Game;
import bwapi.Unit;
import bwapi.UnitType;

public class InfluenceMap {

	double mapa[][];
	final byte umbral = 3;
	final byte radio = 2;
	
	// array de las unidades que están en juego, cada posición es una tupla de tres posiciones con
	// el id de la unidad, y sus coordenadas x e y para un momento dado
	List<ArrayList<Integer>> unidades;
	
	public InfluenceMap(int alto, int ancho) {
		mapa = new double[alto][ancho];
		unidades = new ArrayList<ArrayList<Integer>>();
	}
	
	
	/*
	 * Este método modificará el valor de la influencia de una casilla.
	 * Tiene dos parámetros de entrada que se corresponden con la celda sobre la que se
	 * va a realizar la actualización mediante un objeto de tipo Point y el valor de influencia de la casilla que será
	 * de tipo int. La modificación de la influencia de una casilla se realizará mediante una operación de adición.
	 * Hay que tener en cuenta que la modificación de la influencia de una casilla supone también la propagación
	 * de la influencia a las casillas que la rodean
	 */
	public void updateCellInfluence_building(Point punto, int influencia){
		// Obtenemos el area de efecto limitada a la dimensión del mapa en esa posición.
		int n = ((int)punto.getY()-radio>0) ? (int)punto.getY()-radio : 0;
		int s = ((int)punto.getY()+radio<mapa.length) ? (int)punto.getY()+radio : mapa.length-1;
		int w = ((int)punto.getX()-radio>0) ? (int)punto.getX()-radio : 0;
		int e = ((int)punto.getX()+radio<mapa.length) ? (int)punto.getX()+radio : mapa[0].length-1;
		
		for(int i = n; i<s; i++) {
			for(int j = w; j<e; j++) {
				mapa[i][j] = redondear(mapa[i][j] + influencia/Math.pow((1+distanciaEuclidea(punto, i, j)),2));
			}
		}
	}
	
	public void updateCellInfluence_unit(Point punto, int influencia){
		// Obtenemos el area de efecto limitada a la dimensión del mapa en esa posición.
		int n = ((int)punto.getY()-radio>0) ? (int)punto.getY()-radio : 0;
		int s = ((int)punto.getY()+radio<mapa.length) ? (int)punto.getY()+radio : mapa.length-1;
		int w = ((int)punto.getX()-radio>0) ? (int)punto.getX()-radio : 0;
		int e = ((int)punto.getX()+radio<mapa.length) ? (int)punto.getX()+radio : mapa[0].length-1;
		
		for(int i = n; i<s; i++) {
			for(int j = w; j<e; j++) {
				mapa[i][j] = influencia/Math.pow((1+distanciaEuclidea(punto, i, j)),2);
			}
		}
	}
	
	/**
	 * Este método modificará el valor de influencia de un conjunto de casillas.
	 * Este método tiene dos parámetros de entrada que se corresponden con un conjunto
	 * de celdas sobre las que se va a realizar la actualización mediante una lista de 
	 * objetos de tipo Point y el valor de influencia para cada una de las casillas que
	 * será un valor de tipo int. La modificación de la influencia de cada una de las 
	 * casillas se realizará mediante una llamada al método updateCellInfluence
	 */
	public void updateCellsInfluence(List<Point> puntos, int influencia){
		for(Point p : puntos){
			updateCellInfluence_unit(p, influencia);
		}
	}
	
	/**
	 * Este método devolverá el valor de influencia de una casilla.
	 * Este método tiene un parámetro de entrada de tipo Point que se corresponde con la posición
	 * de la casilla de la que se quiere obtener el valor de influencia
	 */
	public double getInfluence(Point punto){	
		return mapa[(int)punto.getY()][(int)punto.getX()];
	}
	
	/**
	 * Auto-explicativo
	 */
	public double distanciaEuclidea(Point inicio, int i, int j){
		return Math.sqrt(Math.pow((int)inicio.getX() - j,2)+(Math.pow((int)inicio.getY() - i,2)));
	}
	
	public double redondear(double valor){
		double a = (double)(int)(valor*100);
		return a/100.0;
	}
	
	
	/**
	 * Este método deberá calcular el valor de influencia 
	 * del jugador sobre el mapa. El valor de influencia 
	 * del jugador será la suma de los valores de influencia
	 * de las casillas pertenecientes al jugador.
	 */
	public double getMyInfluenceLevel(){
		double InfluenceLevel = 0.0;
		for(int i = 0; i<mapa.length; i++){
			for(int j = 0; j<mapa[0].length; j++){
				if(mapa[i][j]>0){
					InfluenceLevel += mapa[i][j]; 
				}
			}
		}	
		return InfluenceLevel;		
	}


	/**
	 * Este método deberá obtener el valor de influencia 
	 * del jugador o jugadores enemigos sobre el mapa. 
	 * El valor de influencia del enemigo será la suma de 
	 * los valores de influencia de las casillas pertenecientes al enemigo.
	 */
	public double getEnemyInfluenceLevel(){
		double InfluenceLevel = 0.0;
		for(int i = 0; i<mapa.length; i++){
			for(int j = 0; j<mapa[0].length; j++){
				if(mapa[i][j]<0){
					InfluenceLevel += mapa[i][j]; 
				}
			}
		}	
		return InfluenceLevel;
	}


	/**
	 *  Este método deberá calcular el número de casillas sobre las 
	 *  cuales el jugador tiene el control. Se sumará 1 unidad por 
	 *  cada casilla perteneciente al jugador.
	 */
	public int getMyInfluenceArea(){
		int count = 0;
		for(int i = 0; i<mapa.length; i++){
			for(int j = 0; j<mapa[0].length; j++){
				if(mapa[i][j]>0){
					count++; 
				}
			}
		}	
		return count;		
	}

	
	/**
	 * Este método deberá calcular el número de casillas sobre 
	 * las cuales el jugador o jugadores enemigos tienen el control.
	 * Se sumará 1 unidad por cada casilla perteneciente al enemigo
	 */
	public int getEnemyInfluenceArea(){
		int count = 0;
		for(int i = 0; i<mapa.length; i++){
			for(int j = 0; j<mapa[0].length; j++){
				if(mapa[i][j]<0){
					count++; 
				}
			}
		}	
		return count;
		
	}
	
	/**
	 * 
	 * Cuando se crea una nueva unidad, se calcula su influencia y se guarda en la lista de unidades
	 * su id, posición e influencia. Esto sirve para actualizar periodicamente las unidades que se han
	 * movido o han muerto.
	 * 
	 * @param unit: Unidad que genera la influencia
	 * @param influencia: indica si es unidad aliada (vale 1) o enemiga (-1)
	 * @param x: coordenada x para el im
	 * @param y: coordenada y para el im
	 */
	public void newUnit(Unit unit, int influencia, int x, int y, boolean ally) {
		ArrayList<Integer> tupla = new ArrayList<Integer>(4);
		tupla.add(unit.getID());
		tupla.add(x);
		tupla.add(y);
		// Los edificios hacen cosas de edificios
		if (unit.getType().isBuilding() && !unit.getType().isResourceContainer()){
			if (unit.getType() == UnitType.Protoss_Pylon ||
					unit.getType() == UnitType.Terran_Bunker ||
					unit.getType() == UnitType.Zerg_Spore_Colony || 
					unit.getType() == UnitType.Zerg_Sunken_Colony) {
				updateCellInfluence_building(new Point(x,y), 5*influencia);
				tupla.add(5*influencia);
				if (ally) unidades.add(tupla);
			} else if (unit.canAttack()) {
				updateCellInfluence_building(new Point(x,y), 4*influencia);
				tupla.add(4*influencia);
				if (ally) unidades.add(tupla);
			}  else if (unit.getType() == UnitType.Terran_Command_Center ||
					unit.getType() == UnitType.Zerg_Hatchery ||
					unit.getType() == UnitType.Protoss_Nexus) {
				updateCellInfluence_building(new Point(x,y), 7*influencia);
				tupla.add(7*influencia);
				if (ally) unidades.add(tupla);
			} else {
				updateCellInfluence_building(new Point(x,y), 3*influencia);
				tupla.add(3*influencia);
				if (ally) unidades.add(tupla);
				
			}
		} else if (!unit.getType().isWorker()) {
			//Las unidades ofensivas hacen cosas de unidades, no de edificios
			if (unit.getType().isMechanical()) {
				updateCellInfluence_unit(new Point(x,y), 2*influencia);
				tupla.add(2*influencia);
				if (ally) unidades.add(tupla);
			} else if (unit.getType().isFlyer()) {
				updateCellInfluence_unit(new Point(x,y), 3*influencia);
				tupla.add(3*influencia);
				if (ally) unidades.add(tupla);
			} else {
				//Si no es mecánica ni voladora, es normal
				updateCellInfluence_unit(new Point(x,y), 1*influencia);
				tupla.add(1*influencia);
				if (ally) unidades.add(tupla);
			}
		}
		
	}
	
	
	public double [][] getmap(){
		return this.mapa;
	}
	
	public void updateMap(Game connector){
		Unit currentUnit;
		int x;
		int y;
		for(ArrayList<Integer> unitTupla : unidades){
			currentUnit = connector.getUnit(unitTupla.get(0));
			if (currentUnit != null) {
				x = currentUnit.getTilePosition().getX();
				y = currentUnit.getTilePosition().getY();
//				if(x != unitTupla.get(1) || y != unitTupla.get(2)){ // Y se ha desplazado, se actualiza la influencia
				updateCellInfluence_unit(new Point(unitTupla.get(1),unitTupla.get(2)), 0);// Retiramos la influencia anterior
				updateCellInfluence_unit(new Point(x,y), unitTupla.get(3));// Actualizamos la nueva influencia
				unitTupla.set(1, x); // actualizamos la nueva posición de la unidad.
				unitTupla.set(2, y);
//				}	
			}
		}
	}
	
	public int findUnitDead(int id){
		int i = 0;
		for(ArrayList<Integer> unitTupla : unidades){
			if(unitTupla.get(0) == id){
				return i;
			}
			i++;
		}
		return -1;
	}
	
	public void removeUnitDead(int id){
		int indice = findUnitDead(id);
		if(indice == -1){
			return;
		}
		updateCellInfluence_unit(new Point(unidades.get(indice).get(1),unidades.get(indice).get(2)), 0);// Retiramos la influencia anterior
		unidades.remove(indice);
	}
	
	//Devuelve las casillas enemigas
	public ArrayList<int[]> getEnemyPositions() {
		ArrayList<int[]> ret = new ArrayList<int[]>();
		for(int i = 0; i<mapa.length; i++){
			for(int j = 0; j<mapa[0].length; j++){
				if(mapa[i][j]<0) {
					int[] pos = new int[]{i,j};
					ret.add(pos);
				}
			}
		}
		return ret;
	}
}
