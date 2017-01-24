package bot;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import jnibwapi.JNIBWAPI;
import jnibwapi.Unit;
import jnibwapi.types.UnitType.UnitTypes;

public class InfluenceMap {

	double mapa[][];
	final int umbral = 3;
	final int radio = 2;
	
	// array de las unidades que est�n en juego, cada posici�n es una tupla de tres posiciones con
	// el id de la unidad, y sus coordenadas x e y para un momento dado
	List<ArrayList<Integer>> unidades;
	
	public InfluenceMap(int alto, int ancho) {
		mapa = new double[alto][ancho];
		unidades = new ArrayList<ArrayList<Integer>>();
	}
	
	
	/*
	 * Este m�todo modificar� el valor de la influencia de una casilla.
	 * Tiene dos par�metros de entrada que se corresponden con la celda sobre la que se
	 * va a realizar la actualizaci�n mediante un objeto de tipo Point y el valor de influencia de la casilla que ser�
	 * de tipo int. La modificaci�n de la influencia de una casilla se realizar� mediante una operaci�n de adici�n.
	 * Hay que tener en cuenta que la modificaci�n de la influencia de una casilla supone tambi�n la propagaci�n
	 * de la influencia a las casillas que la rodean
	 */
	public void updateCellInfluence(Point punto, int influencia){
		// Obtenemos el area de efecto limitada a la dimensi�n del mapa en esa posici�n.
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
	
	/*
	 * Este m�todo modificar� el valor de influencia de un conjunto de casillas.
	 * Este m�todo tiene dos par�metros de entrada que se corresponden con
	 * un conjunto de celdas sobre las que se va a realizar la actualizaci�n mediante una lista de objetos de tipo
	 * Point y el valor de influencia para cada una de las casillas que ser� un valor de tipo int. La modificaci�n de
	 * la influencia de cada una de las casillas se realizar� mediante una llamada al m�todo updateCellInfluence
	 */
	public void updateCellsInfluence(List<Point> puntos, int influencia){
		for(Point p : puntos){
			updateCellInfluence(p, influencia);
		}
	}
	
	/*
	 * Este m�todo devolver� el valor de influencia de una casilla.
	 * Este m�todo tiene un par�metro de entrada de tipo Point que se corresponde con la posici�n
	 * de la casilla de la que se quiere obtener el valor de influencia
	 */
	public double getInfluence(Point punto){	
		return mapa[(int)punto.getY()][(int)punto.getX()];
	}
	
	/*
	 * Auto-explicativo
	 */
	public double distanciaEuclidea(Point inicio, int i, int j){
		return Math.sqrt(Math.pow((int)inicio.getX() - j,2)+(Math.pow((int)inicio.getY() - i,2)));
	}
	
	public double redondear(double valor){
		double a = (double)(int)(valor*100);
		return a/100.0;
	}
	
	
	/*
	 * Este m�todo deber� calcular el valor
	 * de influencia del jugador sobre el mapa. El valor de influencia del jugador ser� la suma de los valores de
	 * influencia de las casillas pertenecientes al jugador.
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


	/*
	 * Este m�todo deber� obtener el
	 * valor de influencia del jugador o jugadores enemigos sobre el mapa. El valor de influencia del enemigo ser� la
	 * suma de los valores de influencia de las casillas pertenecientes al enemigo.
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


	/*
	 *  Este m�todo deber� calcular el n�mero de casillas sobre las 
	 *  cuales el jugador tiene el control. Se sumar� 1 unidad por cada casilla perteneciente al
	 *  jugador.
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

	
	/*
	 * Este m�todo deber� calcular el n�mero de casillas sobre 
	 * las cuales el jugador o jugadores enemigos tienen el control.
	 * Se sumar� 1 unidad por cada casilla perteneciente al enemigo
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
	 * su id, posici�n e influencia. Esto sirve para actualizar periodicamente las unidades que se han
	 * movido o han muerto.
	 * 
	 * @param unit: Unidad que genera la influencia
	 * @param influencia: indica si es unidad aliada (vale 1) o enemiga (-1)
	 * @param x: coordenada x para el im
	 * @param y: coordenada y para el im
	 */
	public void newUnit(Unit unit, int influencia, int x, int y) {
		//int x = unit.getPosition().getBX();
		//int y = unit.getPosition().getBY();
		ArrayList<Integer> tupla = new ArrayList<Integer>();
		tupla.add(unit.getID());
		tupla.add(x);
		tupla.add(y);
		// Los edificios hacen cosas de edificios
		if (unit.getType().isBuilding() && !unit.getType().isResourceContainer()){
			if (unit.getType().isAttackCapable()) {
				updateCellInfluence(new Point(x,y), 4*influencia);
				tupla.add(4*influencia);
				unidades.add(tupla);
			} else if (unit.getType() == UnitTypes.Terran_Bunker ||
					unit.getType() == UnitTypes.Terran_Missile_Turret ||
					unit.getType() == UnitTypes.Zerg_Spore_Colony) {
				//Estas son los �nicos edificios que se 
				//pueden considerar verdaderamente defensivas en el juego
				updateCellInfluence(new Point(x,y), 5*influencia);
				tupla.add(5*influencia);
				unidades.add(tupla);
			} else {
				updateCellInfluence(new Point(x,y), 3*influencia);
				tupla.add(3*influencia);
				unidades.add(tupla);
				
			}
		} else if (unit.getType().isAttackCapable()) {
		// Las unidades ofensivas hacen cosas de unidades, no de edificios
			if (unit.getType().isMechanical()) {
				updateCellInfluence(new Point(x,y), 3*influencia);
				tupla.add(3*influencia);
				unidades.add(tupla);
			} else if (unit.getType().isFlyer()) {
				updateCellInfluence(new Point(x,y), 6*influencia);
				tupla.add(6*influencia);
				unidades.add(tupla);
			} else {
				//Si no es mec�nica ni voladora, es normal
				updateCellInfluence(new Point(x,y), 1*influencia);
				tupla.add(1*influencia);
				unidades.add(tupla);
			}
		}
		
	}
	
	
	public double [][] getmap(){
		return this.mapa;
	}
	
	public void updateMap(JNIBWAPI connector){
		Unit currentUnit;
		int x;
		int y;
		for(ArrayList<Integer> unitTupla : unidades){
			currentUnit = connector.getUnit(unitTupla.get(0));						
			x = currentUnit.getPosition().getBX();
			y = currentUnit.getPosition().getBY();
			if(x != unitTupla.get(1) || y != unitTupla.get(2)){ // Y se ha desplazado, se actualiza la influencia
				updateCellInfluence(new Point(x,y), unitTupla.get(3));// Actualizamos la nueva influencia
				updateCellInfluence(new Point(unitTupla.get(1),unitTupla.get(2)), 0);// Retiramos la influencia anterior
				unitTupla.set(1, x); // actualizamos la nueva posici�n de la unidad.
				unitTupla.set(2, y);
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
		updateCellInfluence(new Point(unidades.get(indice).get(1),unidades.get(indice).get(2)), 0);// Retiramos la influencia anterior
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
