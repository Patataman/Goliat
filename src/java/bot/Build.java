package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

import jnibwapi.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;

public class Build extends Action {
	
	UnitType edificio;
	InfluenceMap influencias;
	
	public Build(String name, GameHandler gh, UnitType building) {
		super(name, gh);
		this.edificio = building;
		this.influencias = ((JohnDoe)this.handler).dah_map;
	}

	@Override
	public State execute() {
		try{
			//Control para construir torretas
			int x = ((JohnDoe)this.handler).posBuild.getBX();
			int y = ((JohnDoe)this.handler).posBuild.getBY();
			if (edificio == UnitTypes.Terran_Missile_Turret){
				//La influencia debe ser menor de 3
				if (influencias.mapa[y][x] > 3) {
					return State.FAILURE;
				}
				//Deben estar espaciadas las torretas
				for (Unit u : ((JohnDoe)this.handler).finishedBuildings) {
					if (u.getType() == UnitTypes.Terran_Missile_Turret && 
							u.getDistance(((JohnDoe)this.handler).posBuild) < 300) {
						return State.FAILURE;
					}
				}
			}
			if (((JohnDoe)this.handler).buildUnit(edificio)) {
				return State.SUCCESS;
			} else {
				return State.FAILURE;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return State.ERROR;
		}
	}

}
