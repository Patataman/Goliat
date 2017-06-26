# _"Goliat online"_

StarCraft Terran Bot developed with [BWAPI](https://github.com/bwapi/bwapi) using [JNIBWAPI](https://github.com/JNIBWAPI/JNIBWAPI) (before V4.1)
and [BWMirror](https://github.com/vjurenka/BWMirror) (after V4.1 (included)).
If you want see the bot playing against the game's AI go to this [Youtube list](https://www.youtube.com/playlist?list=PL9JgBzni37CJBxh18jNsubarZSZKtdO2Y).
If you want see it playing against other real bots, go to the [SSCAIT](http://sscaitournament.com/) page! 


# V1 

This version has the building tree fixed. Including distinction of zones with 1 or more ChokePoints.
* If the zone has 1 CP: Barracks and factory are built in the opposite direction to the CP. Other building are built between the CP and the CC.
* If the zone has 2 or more CP: Barracks and factory are built closer to the CC. Other building are built farther to closer.

# V2

This version has fixed, upgraded and more fabulous attack/defense tree. Now Goliat can manage different groups of unit to attack/defense. Specifically:

* Creation and management of groups of at least 10 units.
  * This groups can have 6 different status:
    * 0: Doing nothing.
    * 1: Attacking.
    * 2: Defending.
    * 3: Regroup.
    * 4: Retreat.
    * 5: Waiting.
    * 6: Exploring. (Never used for now)
* Small changes in the use of the influence for selecting targets.

# V3

This version contains:

* Building tree fixed, upgraded and more fabulous allowing to build add-ons.
* A brand new tree which have a basic management of expansions (Gather resources and train more SCVs).
* Small fixes in the influence.

# V4

This version contains:

* More small fixes in the influence.
* Can train different units according the enemy race.
* Small fixes in building expansions.

# V4.1

This version contains:

* **Change from JNI-BWAPI to BWMirror.**
* **Change to BWAPI 4.1.2**
* Scout behavior.
* Real mineral nodes management. As consequence Stop training endless SCV.

