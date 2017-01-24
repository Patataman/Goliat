package bot;

import java.util.ArrayList;
import java.util.List;

//import org.iaie.search.Successor;

import jnibwapi.ChokePoint;
import jnibwapi.Map;
import jnibwapi.Position;

public class HierarchicalMap {
	
	/*Lista que contiene en cada posición (Id de la region)
	Las regiones (Point) con las que se encuentra conectada esa región*/
//	private List<List<Successor>> conexiones;
	/* Matriz que contiene por cada par X-Y de regiones, la lista de
	 * chokePoints que las conectan. Si no hay conexión, esa lista 
	 * estará vacia.
	 * Además, sólo se rellenará la parte superior a la diagonal
	 * principal, ya que la parte inferior sería identica y en la diagonal
	 * no habrá valores. */
	private List<ChokePoint>[][] chokePoints;
	
	public HierarchicalMap(Map bwapi) {
//		this.conexiones = new ArrayList<List<Successor>>(bwapi.getRegions().size());
//		this.getRegionConections(bwapi.getRegions());
		chokePoints = new ArrayList[bwapi.getRegions().size()][bwapi.getRegions().size()];
		this.getCPConections(bwapi);
	}
	
	/**
	 * Genera la lista de regiones conectadas para cada región.
	 * Guardando los puntos centrales de las mismas.
	 * @param regiones
	 */
//	private void getRegionConections (List<Region> regiones){
//		List <Successor> ids = new ArrayList<Successor>();
//		//Se recorren todas las regiones
//		for (Region r : regiones){
//			//Por cada región se obtienen los ids de las regiones que conecta
//			for (Region r2 : r.getConnectedRegions()){
//				ids.add(new Successor(new Point(r2.getCenter().getWX(), r2.getCenter().getWY()))) ;
//			}
//			//Se añade a la región r las regiones conectadas a ella
//			this.conexiones.add(ids);
//			ids = new ArrayList<Successor>();
//		}
//	}

	/**
	 * Obtiene los choke points que hay entre 2 regiones
	 * @param mapa
	 */
	private void getCPConections(Map mapa){
		//inicializamos la matriz con las listas de chokePoint
		for(int i = 0; i<chokePoints[0].length;i++){
			for(int i2 = i+1; i2<chokePoints[0].length; i2++){
				chokePoints[i][i2] = new ArrayList<ChokePoint>();
			}
		}
		// Se obtienen los chokePoints del mapa
		int c1 = 0;
		int c2 = 0;
		for (ChokePoint cp : mapa.getChokePoints()){
			/* Cada chokePoint se agrega a su lista correspondiente
			 * en la matriz de tal manera que sólo se indicen las 
			 * posiciones superiores a la diagonal*/
			c1 = cp.getFirstRegion().getID()-1;
			c2 = cp.getSecondRegion().getID()-1;
			if(c1<c2){
				this.chokePoints[c1][c2].add(cp);
			}
			else{
				this.chokePoints[c2][c1].add(cp);
			}
		}
	}
	
//	public List<List<Successor>> getConexiones() {
//		return conexiones;
//	}

	public List<ChokePoint>[][] getChokePoints() {
		return chokePoints;
	}
	
	/*
	 * Dada la forma en la que rellenamos la matriz, se utiliza
	 * este método par aacceder comodamente a la lista de chokePoints
	 * que unen dos regiones dadas*/
	public Position getChokeCenter(int c1, int c2){
		if(c1<c2){
			return this.chokePoints[c1][c2].get(0).getCenter();
		}
		else{
			return this.chokePoints[c2][c1].get(0).getCenter();
		}
	}
	
}
