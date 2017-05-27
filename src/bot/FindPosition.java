package bot;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;

public class FindPosition extends Conditional {

	UnitType edificio;
	
	public FindPosition(String name, GameHandler gh, UnitType building) {
		super(name, gh);
		this.edificio = building;
	}

	@Override
	public State execute() {
		try{
			//El decorador se hace aqui para ahorrar visitar el resto de nodos del árbol
			if (edificio == UnitTypes.Terran_Supply_Depot &&
					((JohnDoe)this.handler).supplies < ((JohnDoe)this.handler).totalSupplies*0.7){
				return State.FAILURE;
			}
			//Decorador para barracks. Límite a 2 por CC.
			if (edificio == UnitTypes.Terran_Barracks &&
					(((JohnDoe)this.handler).barracks >= ((JohnDoe)this.handler).CCs.size()+2 ||
					(((JohnDoe)this.handler).barracks != 0 && ((JohnDoe)this.handler).refinery == 0))) {
				return State.FAILURE;
			}
			//Decorador para la refinería. Va después de los barracks.
			if (edificio == UnitTypes.Terran_Refinery &&
					(((JohnDoe)this.handler).refinery == ((JohnDoe)this.handler).CCs.size() ||
					((JohnDoe)this.handler).barracks == 0 )) {
				return State.FAILURE;
			}
			//Decorador para la academy, va después de los barracks. Limite 1
			if (edificio == UnitTypes.Terran_Academy && (((JohnDoe)this.handler).barracks == 0 || 
					((JohnDoe)this.handler).academy != 0)) {
				return State.FAILURE;
			}
			//Decorador para la bahía de ingeniería
			if (edificio == UnitTypes.Terran_Engineering_Bay && (((JohnDoe)this.handler).bay != 0 
					|| ((JohnDoe)this.handler).barracks == 0)) {
				return State.FAILURE;
			}
			//Decorador para la fábrica, va después de los barracks. Límite a 2 por CC.
			if (edificio == UnitTypes.Terran_Factory && (((JohnDoe)this.handler).barracks == 0 ||
					((JohnDoe)this.handler).factory >= ((JohnDoe)this.handler).CCs.size())) {
				return State.FAILURE;
			}
			//Decorador para el armory. Límite a 1
			if (edificio == UnitTypes.Terran_Armory && (((JohnDoe)this.handler).factory == 0 ||
					((JohnDoe)this.handler).armory != 0)) {
				return State.FAILURE;
			}
			//Decorador para construir expansiones.
			//Sólo se construye cuando está construido el barracón
			if (edificio == UnitTypes.Terran_Command_Center && 
					(((JohnDoe)this.handler).barracks == 0 ||
					((JohnDoe)this.handler).CCs.size() == 2)) {
				return State.FAILURE;
			}
			//Decorador para construir laboratorios cientificos.
			//Sólo se construye 1
			if (edificio == UnitTypes.Terran_Science_Facility && ((JohnDoe)this.handler).lab_cient == 1) {
				return State.FAILURE;
			}
			//Decorador para construir laboratorios cientificos.
			//Sólo se construye 1
			if (edificio == UnitTypes.Terran_Starport && ((JohnDoe)this.handler).starport == 1) {
				return State.FAILURE;
			}
			
			if (edificio == UnitTypes.Terran_Bunker && ((JohnDoe)this.handler).barracks == 0 &&
					((JohnDoe)this.handler).bunkers.size() >= 2) {
				return State.FAILURE;
			}
			
			if (((JohnDoe)this.handler).findPosition(edificio)) {
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
