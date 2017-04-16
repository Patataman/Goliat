# _"Goliat online"_

# V1 

Bot Terran de Starcraft desarrollado con [BWAPI](https://github.com/bwapi/bwapi) mediante la API de Java ([JBWAPI](https://github.com/JNIBWAPI/JNIBWAPI))

Esta versión tiene arreglado el árbol de construcción. Añadiendo distinción entre zonas con 1 o más _ChokePoints_.
* Si la zona tiene 1 CP: Se construyen barracones y fábricas en dirección contraria al CP. El resto de edificios se intentan construir en dirección al CP, para hacer bulto.
* SI la zona tiene 2 o más CP: Se construyen barracones y fábricas lo más cercano al CC. El resto de edificios se construyen desde cierta distancia hacia el CC, es decir, desde más lejos a más cerca.

# V2

Esta versión tiene arreglado, mejorado y más elaborado el árbol de ataque/defensa. Pudiendo gestionar distintos grupos de unidades para realizar ataques/defensas. Concretamente:

* Creación y gestión de grupos de al menos 10 unidades.
  * Estos grupos pueden pasar por 6 estados distintos:
    * 0: Doing nothing.
    * 1: Attacking.
    * 2: Defending.
    * 3: Regroup.
    * 4: Retreat.
    * 5: Waiting.
    * 6: Exploring 
* Cambios menores en el uso de la influencia para la selección de objetivos

# V3

Esta versión tiene:

* Árbol de construcción arreglado, mejorado, permitiendo construir los añadidos a edificios, como pueden ser el de la fábrica o laboratorio científico.
* Árbol para gestionar la recolección de recursos de cada base (incluyendo expansiones), aunque muy básico.
* Pequeños cambios en la influencia para mejorar el ataque
