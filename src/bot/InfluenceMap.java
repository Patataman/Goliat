package bot;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

import bwapi.Game;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class InfluenceMap {

	double mapa[][];
	double temp_map[][][]; //High, Height, Width
	final byte radio = 2;
	
	// array de las unidades que están en juego, cada posición es una tupla de tres posiciones con
	// el id de la unidad, y sus coordenadas x e y para un momento dado
	ArrayList<Object[]> unidades;
	ArrayList<Object[]> edificios;
	
	public InfluenceMap(int alto, int ancho) {
		mapa = new double[alto][ancho];
		temp_map = new double[3][alto][ancho];
		unidades = new ArrayList<Object[]>();
		edificios = new ArrayList<Object[]>();
	}
	
	
	/*
	 * Este método modificará el valor de la influencia de una casilla.
	 * Tiene dos parámetros de entrada que se corresponden con la celda sobre la que se
	 * va a realizar la actualización mediante un objeto de tipo Point y el valor de influencia de la casilla que será
	 * de tipo int. La modificación de la influencia de una casilla se realizará mediante una operación de adición.
	 * Hay que tener en cuenta que la modificación de la influencia de una casilla supone también la propagación
	 * de la influencia a las casillas que la rodean
	 */
	public void updateCellInfluence_building(TilePosition punto, int influencia){
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
	
	public void updateCellInfluence_unit(TilePosition punto, int height, int influencia){
		// Obtenemos el area de efecto limitada a la dimensión del mapa en esa posición.
		int n = ((int)punto.getY()-radio>0) ? (int)punto.getY()-radio : 0;
		int s = ((int)punto.getY()+radio<temp_map.length) ? (int)punto.getY()+radio : temp_map.length-1;
		int w = ((int)punto.getX()-radio>0) ? (int)punto.getX()-radio : 0;
		int e = ((int)punto.getX()+radio<temp_map.length) ? (int)punto.getX()+radio : temp_map[0].length-1;
		for(int i = n; i<s; i++) {
			for(int j = w; j<e; j++) {
				temp_map[height][i][j] = influencia/Math.pow((1+distanciaEuclidea(punto, i, j)),2);
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
//	public void updateCellsInfluence(TilePosition puntos, int height, int influencia){
//		for(Point p : puntos){
//			updateCellInfluence_unit(p, height, influencia);
//		}
//	}
	
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
	public double distanciaEuclidea(TilePosition inicio, int i, int j){
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
	public void newUnit(Unit unit, int influencia, int height, boolean ally) {
		for (Object[] o : unidades){
			if (o[0].equals(unit))
				return;
		}
		Object[] tupla = new Object[3];
		tupla[0] = unit;
		tupla[2] = height;
		// Los edificios hacen cosas de edificios
		if (unit.getType().isBuilding() && !unit.getType().isResourceContainer()){
			
			if (!unit.getType().isWorker()) {
				//Las unidades ofensivas hacen cosas de unidades, no de edificios
				if (unit.getType().isMechanical()) {
					updateCellInfluence_unit(unit.getTilePosition(), height, 2*influencia);
					tupla[1] = 2*influencia;
					unidades.add(tupla);
				} else if (unit.getType().isFlyer()) {
					updateCellInfluence_unit(unit.getTilePosition(), height, 3*influencia);
					tupla[1] = 3*influencia;
					unidades.add(tupla);
				} else {
					//Si no es mecánica ni voladora, es normal
					updateCellInfluence_unit(unit.getTilePosition(), height, 1*influencia);
					tupla[1] = 1*influencia;
					unidades.add(tupla);
				}
			}
		}
		
	}
	
	public void newBuilding(Unit unit, int influencia, boolean ally) {
		for (Object[] o : edificios){
			if (o[0].equals(unit))
				return;
		}
		Object[] tupla = new Object[2];
		tupla[0] = unit;
		// Los edificios hacen cosas de edificios
		if (unit.getType().isBuilding() && !unit.getType().isResourceContainer()){
			if (unit.getType() == UnitType.Protoss_Pylon ||
					unit.getType() == UnitType.Terran_Bunker ||
					unit.getType() == UnitType.Zerg_Spore_Colony || 
					unit.getType() == UnitType.Zerg_Sunken_Colony) {
				updateCellInfluence_building(unit.getTilePosition(), 5*influencia);
				tupla[1] = 5*influencia;
				edificios.add(tupla);
			} else if (unit.canAttack()) {
				updateCellInfluence_building(unit.getTilePosition(), 4*influencia);
				tupla[1] = 4*influencia;
				unidades.add(tupla);
			}  else if (unit.getType() == UnitType.Terran_Command_Center ||
					unit.getType() == UnitType.Zerg_Hatchery ||
					unit.getType() == UnitType.Protoss_Nexus) {
				updateCellInfluence_building(unit.getTilePosition(), 10*influencia);
				tupla[1] = 7*influencia;
				unidades.add(tupla);
			} else {
				updateCellInfluence_building(unit.getTilePosition(), 3*influencia);
				tupla[1] = 3*influencia;
				unidades.add(tupla);
				
			}
		}
	}
	
	
	public double [][] getmap(){
		return this.mapa;
	}
	
	public void updateMap(Game connector){
		//Clear temp_map
		for (int i = 0; i<temp_map.length; i++){
			for (int j=0; j<temp_map[0].length; j++){
				Arrays.fill(temp_map[i][j], 0);
			}
		}
		for(Object[] unitTupla : unidades){
			Unit currentUnit = (Unit) unitTupla[0];
			if (currentUnit != null) {
				int z = connector.getGroundHeight(currentUnit.getTilePosition());
//				if(x != unitTupla.get(1) || y != unitTupla.get(2)){ // Y se ha desplazado, se actualiza la influencia
				updateCellInfluence_unit(currentUnit.getTilePosition(), z, (Integer) unitTupla[1]);// Actualizamos la nueva influencia
//				}	
			}
		}
	}

	
	public void removeUnitDead(Unit u){
		ArrayList<Object[]> aux = null;
		
		if (u.getType().isBuilding()){
			aux = (ArrayList<Object[]>)edificios.clone();
			for (Object[] b_aux : aux){
				if (b_aux[0].equals(u)) {
					edificios.remove(aux.indexOf(b_aux));
					updateCellInfluence_building(u.getTilePosition(), 0);
					return;
				}
			}	
		} else {
            aux = (ArrayList<Object[]>)unidades.clone();
			for (Object[] u_aux : aux){
				if (u_aux[0].equals(u)) {
					unidades.remove(aux.indexOf(u_aux));
					updateCellInfluence_unit(u.getTilePosition(), (Integer)u_aux[2], 0);
					return;
				}
			}
		}
		
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
	
	public ArrayList<int[]> getTempEnemyPositions(int height) {
		ArrayList<int[]> ret = new ArrayList<int[]>();
		for(int i = 0; i<temp_map[height].length; i++){
			for(int j = 0; j<temp_map[height][0].length; j++){
				if(temp_map[height][i][j]<0) {
					int[] pos = new int[]{i,j};
					ret.add(pos);
				}
			}
		}
		return ret;
	}
}
