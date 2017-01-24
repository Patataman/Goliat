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
			//El decorador se hace aqui para ahorrar visitar el resto de nodos del �rbol
			if (edificio == UnitTypes.Terran_Supply_Depot &&
					((JohnDoe)this.handler).supplies < ((JohnDoe)this.handler).totalSupplies*0.7){
				return State.FAILURE;
			}
			//Decorador para barracones. L�mite a 2 por CC.
			if (edificio == UnitTypes.Terran_Barracks &&
					(((JohnDoe)this.handler).barracones >= ((JohnDoe)this.handler).CCs.size()+2 ||
					(((JohnDoe)this.handler).barracones != 0 && ((JohnDoe)this.handler).refineria == 0))) {
				return State.FAILURE;
			}
			//Decorador para la refiner�a. Va despu�s de los barracones.
			if (edificio == UnitTypes.Terran_Refinery &&
					(((JohnDoe)this.handler).refineria == ((JohnDoe)this.handler).CCs.size() ||
					((JohnDoe)this.handler).barracones == 0 )) {
				return State.FAILURE;
			}
			//Decorador para la academia, va despu�s de los barracones. Limite 1
			if ((edificio == UnitTypes.Terran_Academy && ((JohnDoe)this.handler).barracones == 0) || 
					(edificio == UnitTypes.Terran_Academy && ((JohnDoe)this.handler).academia != 0)) {
				return State.FAILURE;
			}
			//Decorador para la bah�a de ingenier�a
			if (edificio == UnitTypes.Terran_Engineering_Bay && (((JohnDoe)this.handler).bahia != 0 
					|| ((JohnDoe)this.handler).barracones == 0)) {
				return State.FAILURE;
			}
			//Decorador para la f�brica, va despu�s de los barracones. L�mite a 2 por CC.
			if ((edificio == UnitTypes.Terran_Factory && ((JohnDoe)this.handler).barracones == 0) ||
					(edificio == UnitTypes.Terran_Factory && ((JohnDoe)this.handler).fabricas >= ((JohnDoe)this.handler).CCs.size())) {
				return State.FAILURE;
			}
			//Decorador para el arsenal. L�mite a 1
			if ((edificio == UnitTypes.Terran_Armory && ((JohnDoe)this.handler).fabricas == 0) ||
					(edificio == UnitTypes.Terran_Armory && ((JohnDoe)this.handler).arsenal != 0)) {
				return State.FAILURE;
			}
			//Decorador para construir expansiones.
			//S�lo se construye cuando est� construido el barrac�n
			if (edificio == UnitTypes.Terran_Command_Center && 
					(((JohnDoe)this.handler).barracones == 0 ||
					((JohnDoe)this.handler).CCs.size() == 2)) {
				return State.FAILURE;
			}
			//Decorador para construir laboratorios cientificos.
			//S�lo se construye 1
			if (edificio == UnitTypes.Terran_Science_Facility && ((JohnDoe)this.handler).lab_cient == 1) {
				return State.FAILURE;
			}
			//Decorador para construir laboratorios cientificos.
			//S�lo se construye 1
			if (edificio == UnitTypes.Terran_Starport && ((JohnDoe)this.handler).puerto == 1) {
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
