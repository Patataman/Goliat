# _"Goliat online v1"_

Bot Terran de Starcraft desarrollado con [BWAPI](https://github.com/bwapi/bwapi) mediante la API de Java ([JBWAPI](https://github.com/JNIBWAPI/JNIBWAPI)

Esta versión tiene arreglado el árbol de construcción. Añadiendo distinción entre zonas con 1 o más _ChokePoints_.
* Si la zona tiene 1 CP: Se construyen barracones y fábricas en dirección contraria al CP. El resto de edificios se intentan construir en dirección al CP, para hacer bulto.
* SI la zona tiene 2 o más CP: Se construyen barracones y fábricas lo más cercano al CC. El resto de edificios se construyen desde cierta distancia hacia el CC, es decir, desde más lejos a más cerca.
