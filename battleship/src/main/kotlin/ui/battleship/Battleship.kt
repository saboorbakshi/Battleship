package ui.battleship

import ui.battleship.model.Game
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.ImagePattern
import javafx.scene.shape.Rectangle
import javafx.stage.Stage

class Battleship : Application() {
    override fun start(stage: Stage) {

        val game = Game(10, false)
        val player = UI(game)
        val computer = AI(game)
        game.startGame()

        stage.apply {
            scene = Scene(player, 875.0, 375.0)
            title = "BATTLESHIP"
        }.show()
    }
}