# Battleship

Implementation of a widely popular game, [battleship](https://en.wikipedia.org/wiki/Battleship_(game)) in Kotlin. This game is designed to played with a mouse.

How to play:
* Place all 5 ships onto your formation.
  * Left clicking a ship selects it.
  * Dragging the mouse whilst having the ship selected drags the ship.
  * Right clicking on a ship rotates it to the right by 90 degrees. 
  * Releasing the left-hand button releases the ship.
  * Releasing a ship anywhere but inside your formation animates the ship back to its original position
  * Overlapping two ships is restricted.
* Once all ships are placed, the Start Game button is activated.
* Once clicked upon, the computer (opponent) places its ships.
* Click on any tile in the Opponent's Waters.
  * If the tile turns grey, you missed.
  * Else if the tile gets bombed, you hit.
  * The tiles turn red when a ship sinks.
* Whoever sinks all 5 ships first wins.

\

https://user-images.githubusercontent.com/90419652/238118853-c5f92f5a-7d5d-4a94-8080-9c146e60d826.mov
