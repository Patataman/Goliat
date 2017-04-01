# _"Goliat online v2"_

Bot Terran de Starcraft desarrollado con [BWAPI](https://github.com/bwapi/bwapi) mediante la API de Java ([JBWAPI](https://github.com/JNIBWAPI/JNIBWAPI)

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