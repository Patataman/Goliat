package bot.others;

import java.util.ArrayList;
import java.util.Arrays;

import bwapi.Game;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class NewInfluence {
	
	public double buildMap[][];  //Height, Width
	public double unitMap[][];   //Height, Width
	private float threshold;
	private byte distance;   //Max distance to spread the influence
	
	/**
	 * Constructor
	 * @param height
	 * @param width
	 */
	public NewInfluence(int height, int width) {
		buildMap = new double[height][width];
		unitMap = new double[height][width];
		threshold = 0.5f;
		distance = 2;
	}

	/**
	 * Clean the temporal influence of the map
	 * @param connector
	 */
	public void updateMap(Game connector) {
		for (int h=0; h<unitMap.length; h++){
				//Clear all the rows
				Arrays.fill(unitMap[h], 0);
		}		
		//Now sets the visible units influence
		for (Unit u : connector.enemy().getUnits()) {
			if (!u.getType().isBuilding() && 
					u.isVisible() && 
					u.getType() != UnitType.Unknown &&
					connector.isBuildable(u.getTilePosition())) {
				newUnit(u);
			}
		}
		
	}
	
	/**
	 * Return the influence of a unit tile
	 * @param tile: tile to get the influence
	 * @param gh: At what ground height the tile is
	 * @return double with the tile value
	 */
	public double getUnitTileInfluence(TilePosition tile) {
		return unitMap[tile.getY()][tile.getX()];
	}
	
	/**
	 * Returns the influence of a tile
	 * @param tile: Tile to get the influence
	 * @return double with the tile value
	 */
	public double getBuildingTileInfluence(TilePosition tile) {
		return buildMap[tile.getY()][tile.getX()];
	}

	/**
	 * Returns a list of enemy units positions at the specific ground height
	 * @return ArrayList<TilePosition> with the enemy positions in that height
	 */
	public ArrayList<TilePosition> getTempEnemyPositions() {
		ArrayList<TilePosition> retList = new ArrayList<TilePosition>();
		//Iterate over the unitMap (at the ground distance passed) and return all
		//the tiles with have an influence bigger than the threshold.
		for (int h=0; h<unitMap.length; h++) {
			for (int w=0; w<unitMap[0].length; w++) {
				if (unitMap[h][w] < -threshold) {
					retList.add(new TilePosition(w, h));
				}
			}
		}
		return retList;
	}
	
	/**
	 * Returns a list of enemy buildings positions
	 * @return ArrayList<TilePosition> List with the enemy buildings positions
	 */
	public ArrayList<TilePosition> getEnemyPositions() {
		ArrayList<TilePosition> retList = new ArrayList<TilePosition>();
		//Iterate over the buildMap and return all the tiles
		//with have an influence bigger than the threshold.
		for (int h=0; h<buildMap.length; h++) {
			for (int w=0; w<buildMap[0].length; w++) {
//				if (buildMap[h][w] != 0)
//					System.out.println(""+buildMap[h][w]+", "+ -threshold +", "+ (buildMap[h][w] < -threshold));
				if (buildMap[h][w] < -threshold) {
					retList.add(new TilePosition(w, h));
				}
			}
		}
		return retList;
	}


//	/**
//	 * Removes an enemy unit from the list (NOT BUILDINGS)
//	 * Buildings are updated with removeBuilding
//	 * @param unit: remove the unit from the enemyUnits list
//	 */
//	public void removeUnit(Unit unit, byte gh) {
//		int indexToRemove = -1;
//		if (unitsOnSight.contains(unit)) {
//			unitsOnSight.remove(unit);
//			for (ArrayList<Object> t : controlList) {
//				if (t.get(0).equals(unit)) {
//					indexToRemove = controlList.indexOf(t);
//					applyUnitInfluence((byte)0, unit.getTilePosition(), gh);
//					break;
//				}
//			}
//			controlList.remove(indexToRemove);
//		}
//	}
//	
	/**
	 * Removes a building influence from the map.
	 * Applies 0 influence in the center of the building
	 * @param unit
	 * @param b
	 */
	public void removeBuilding(Unit building) {
		applyBuildingInfluence((byte)0, building.getTilePosition(), null);
	}

	/**
	 * Adds building influence in the map
	 * @param unit: Building finished
	 * @param b: true: ally - false: enemy
	 */
	public void newBuilding(Unit unit, boolean b, Game connector) {
		byte influence = (byte) ((b) ? 1 : -1);
		//Check important buildings
		if (unit.getType() == UnitType.Protoss_Pylon ||
				unit.getType() == UnitType.Terran_Bunker ||
				unit.getType() == UnitType.Zerg_Spore_Colony || 
				unit.getType() == UnitType.Zerg_Sunken_Colony) {
			applyBuildingInfluence((byte)(5*influence), unit.getTilePosition(), connector);
		}
		//Check bases
		else if (unit.getType() == UnitType.Terran_Command_Center ||
				unit.getType() == UnitType.Zerg_Hatchery ||
				unit.getType() == UnitType.Protoss_Nexus) {
			applyBuildingInfluence((byte)(7*influence), unit.getTilePosition(), connector);
		}
		//Check other types of buildings which can attack
		else if (unit.canAttack()) {
			applyBuildingInfluence((byte)(4*influence), unit.getTilePosition(), connector);
		}
		//Anything else
		else {
			applyBuildingInfluence((byte)(3*influence), unit.getTilePosition(), connector);
		}
	}

	/**
	 * Adds unit influence in the map (ONLY ENEMIES)
	 * It's distributed in the three possible heights
	 * @param unit: Enemy unit
	 */
	public void newUnit(Unit unit) {
		if (!unit.getType().isWorker()) {

			//Las unidades ofensivas hacen cosas de unidades, no de edificios
			if (unit.getType().isMechanical()) {
				applyUnitInfluence((byte)-2,unit.getTilePosition());
			} else if (unit.getType().isFlyer()) {
				applyUnitInfluence((byte)-3,unit.getTilePosition());
			} else {
				applyUnitInfluence((byte)-1,unit.getTilePosition());
			}
		}
	}
	
	/**
	 * Spread the influence of a tile
	 * @param influence: influence to apply
	 * @param pos: position where apply the influence
	 */
	public void applyBuildingInfluence(byte influence, TilePosition pos, Game connector) {
		// Obtenemos el area de efecto limitada a la dimensión del mapa en esa posición.
		byte n = (byte) ((pos.getY()-distance>0) ? pos.getY()-distance : 0);
		byte s = (byte) ((pos.getY()+distance<buildMap.length) ? pos.getY()+distance : buildMap.length-1);
		byte w = (byte) ((pos.getX()-distance>0) ? pos.getX()-distance : 0);
		byte e = (byte) ((pos.getX()+distance<buildMap.length) ? pos.getX()+distance : buildMap[0].length-1);
		for(byte i = n; i<s; i++) {
			for(byte j = w; j<e; j++) {
				if (influence == 0 || connector.isBuildable(pos))
					buildMap[i][j] += influence/Math.pow((1+euclideanDistance(pos, i, j)),2);
			}
		}
	}
	
	public void applyUnitInfluence(byte influence, TilePosition pos) {
		//Maybe there's more than 1 unit in a tile, so +=
		unitMap[(byte) pos.getY()][(byte) pos.getX()] += influence;
	}
	
	/**
	 * Euclidean distance to a point
	 * @param pos beginning position
	 * @param i Y axis distance
	 * @param j X axis distance
	 * @return euclidean distance
	 */
	public double euclideanDistance(TilePosition pos, byte i, byte j){
		return Math.sqrt(Math.pow(pos.getX() - j,2)+(Math.pow(pos.getY() - i,2)));
	}

}
