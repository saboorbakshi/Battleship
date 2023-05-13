package ui.battleship

import javafx.animation.RotateTransition
import javafx.animation.TranslateTransition
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.input.MouseButton
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.paint.ImagePattern
import javafx.scene.paint.Paint
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.util.Duration
import ui.battleship.model.*
import kotlin.math.abs
import kotlin.math.floor

class UI(private var game : Game) : Pane() {

    private val boom = Image("bomb.jpg")
    private val boomPattern = ImagePattern(boom)
    private val water = Image("water.jpg")
    private val waterPattern = ImagePattern(water)

    inner class Pos(var x: Double = 0.0, var y: Double = 0.0)

    private val startButton = Button("Start Game").apply {
        textAlignment = TextAlignment.CENTER
        minWidth = 150.0
        isDisable = true
        translateX = 362.5
        translateY = 300.0
    }

    private val exitButton = Button("Exit Game").apply {
        textAlignment = TextAlignment.CENTER
        minWidth = 150.0
        translateX = 362.5
        translateY = 325.0
    }

    // determines cell fill based on cell state & game stage
    private val cellColor: (CellState, GameStage) -> Paint = { state: CellState, stage : GameStage ->
        when (stage) {
            GameStage.Setup -> { waterPattern }
            GameStage.Loop -> {
                when (state) {
                    CellState.Ocean -> { waterPattern }
                    CellState.Attacked -> { Color.LIGHTGRAY }
                    CellState.ShipSunk -> { Color.RED }
                    else -> { boomPattern }}}
            GameStage.Resolution -> {
                when (state) {
                    CellState.ShipSunk -> { Color.RED }
                    else -> { waterPattern }}}
        }
    }

    private var stage = GameStage.Setup

    private val ships = mutableListOf<Ship>()
    private val humanBoard = mutableListOf<Rectangle>()
    private val computerBoard = mutableListOf<Rectangle>()

    private val customFont: () -> Font = { Font.font("Verdana", FontWeight.BOLD, 15.0) }
    private var middleText = Text(402.5, 25.0, "My Fleet").apply { font = customFont() }

    // stage determines color scheme to be used to draw the board
    private fun drawBoard(playerType : Player, stage : GameStage) {
        var posY = 50.0
        var posX = if (playerType == Player.Human) { 30.0 } else { 545.0 }
        val board = game.getBoard(playerType)
        for (row in board) {
            for (cellState in row) {
                val rect = Rectangle(posX, posY, 30.0, 30.0).apply {
                    fill = cellColor(cellState, stage)
                    stroke = Color.BLACK
                }
                if (playerType == Player.Human) { humanBoard.add(rect) } else { computerBoard.add(rect) }
                children.add(rect)
                posX += 30.0
            }
            posX = if (playerType == Player.Human) { 30.0 } else { 545.0 }
            posY += 30.0
        }
    }

    // finds the offset between the vertical and "actual" layoutX/layoutY
    private fun getOffset(ship : Ship) : Double {
        var offset = 0.0
        if (ship.orientation == Orientation.Horizontal) {
            val length = Game.shipLength[ship.shipType]
            offset = floor(length!!.toDouble() / 2.0) * 30.0
            if (length.rem(2) == 0) offset -= 15.0
        }
        return offset
    }

    // determines whether a ship in within the bounds of the human board
    private fun isOnBoard(ship : Ship) : Boolean {

        var topLeft = Pos(ship.shape.layoutX + ship.initX, ship.shape.layoutY + ship.initY)
        var topRight = Pos(topLeft.x + 20.0, topLeft.y)
        var bottomLeft = Pos(topLeft.x, topLeft.y + (30.0 * Game.shipLength[ship.shipType]!! - 10.0))
        var bottomRight = Pos(topRight.x, bottomLeft.y)

        if (ship.orientation == Orientation.Horizontal) {
            val offset = getOffset(ship)
            topLeft = Pos(topLeft.x - offset, topLeft.y + offset)
            topRight = Pos(topLeft.x + (30.0 * Game.shipLength[ship.shipType]!! - 10.0), topLeft.y)
            bottomLeft = Pos(topLeft.x, topLeft.y + 20.0)
            bottomRight = Pos(topRight.x, bottomLeft.y)
        }

        val corners = listOf(topLeft, topRight, bottomLeft, bottomRight)
        for (corner in corners) {
            if (corner.x !in (30.0..330.0) || corner.y !in (50.0..350.0)) return false
        }
        return true
    }

    // snaps a ship to its closest grid position on board
    private fun snap(ship : Ship) {
        var topLeftX = ship.shape.layoutX + ship.initX
        var topLeftY = ship.shape.layoutY + ship.initY
        var oldDiffX = 999.0
        var oldDiffY = 999.0
        var closestX = 0.0
        var closestY = 0.0

        val offset = getOffset(ship)
        topLeftX -= offset
        topLeftY += offset

        for (i in 0..9) {
            val newDiffX = abs(topLeftX - (30.0 * (i + 1)))
            if (newDiffX < oldDiffX) {
                oldDiffX = newDiffX
                closestX = 30.0 * (i + 1)
            }
            val newDiffY = abs(topLeftY - (50.0 + (30.0 * i)))
            if (newDiffY < oldDiffY) {
                oldDiffY = newDiffY
                closestY = 50.0 + (30.0 * i)
            }
        }

        val bowX = (closestX / 30) - 1
        val bowY = ((closestY - 20) / 30) - 1
        val id = game.placeShip(ship.shipType, ship.orientation, bowX.toInt(), bowY.toInt())

        // Overlapping ships
        if (id == Cell.NoShip) {
            drift(ship)
        // Success
        } else {
            ship.shipId = id
            closestX += offset
            closestY -= offset
            ship.shape.layoutX = closestX - ship.initX + 5.0
            ship.shape.layoutY = closestY - ship.initY + 5.0
        }
    }

    // implements a rotation and translation transition to the ship's original position in the fleet
    private fun drift(ship : Ship) {
        if (ship.orientation == Orientation.Horizontal) {
            ship.orientation = Orientation.Vertical
            RotateTransition(Duration.seconds(0.15), ship.shape).apply {
                byAngle = 90.0
                play()
            }
        }
        TranslateTransition(Duration.seconds(0.75), ship.shape).apply {
            toX = ship.shape.layoutX * -1
            toY = ship.shape.layoutY * -1
            play()
        }
        ship.initX = ship.mainX - ship.shape.layoutX
        ship.initY = 50.0 - ship.shape.layoutY
    }

    // implements multiple mouse events including pressed, released and dragged
    private fun setListeners(ship: Ship) {
        val dragDelta = Pos()
        var primaryPressed = false
        ship.shape.onMousePressed = EventHandler {
            if (it.button == MouseButton.PRIMARY) {
                primaryPressed = true
                dragDelta.x = ship.shape.layoutX - it.sceneX
                dragDelta.y = ship.shape.layoutY - it.sceneY
            } else if (it.button == MouseButton.SECONDARY && primaryPressed) {
                ship.orientation = if (ship.orientation == Orientation.Horizontal) {
                    Orientation.Vertical
                } else {
                    Orientation.Horizontal
                }
                RotateTransition(Duration.seconds(0.15), ship.shape).apply {
                    byAngle = 90.0
                    play()
                }
            }
        }

        ship.shape.onMouseReleased = EventHandler {
            if (it.button == MouseButton.PRIMARY) {
                primaryPressed = false
                // ship was already on board, must remove it before proceeding
                if (ship.shipId != Cell.NoShip) {
                    game.removeShip(ship.shipId)
                    ship.shipId = Cell.NoShip
                }
                if (isOnBoard(ship)) {
                    snap(ship)
                } else {
                    drift(ship)
                }
                startButton.isDisable = game.getShipsPlacedCount(Player.Human) != 5
            }
        }

        ship.shape.onMouseDragged = EventHandler {
            if (it.button == MouseButton.PRIMARY) {
                ship.shape.layoutX = it.sceneX + dragDelta.x
                ship.shape.layoutY = it.sceneY + dragDelta.y
            }
        }
    }

    // check whether an attack on the opponent board is valid
    // If so, redraws the two boards and the human player's ships
    private fun attackCell() {
        this.onMousePressed = EventHandler {
            if ((it.sceneX in (545.0..845.0)) && (it.sceneY in (50.0..350.0))) {
                val x = floor((it.sceneX - 515) / 30) - 1
                val y = floor((it.sceneY - 20) / 30) - 1
                if (game.getBoard(Player.Ai)[y.toInt()][x.toInt()] == CellState.Ocean) {
                    game.attackCell(x.toInt(), y.toInt())
                    children.removeAll(computerBoard)
                    children.removeAll(humanBoard)
                    for (ship in ships) children.remove(ship.shape)
                    // redraw
                    drawBoard(Player.Ai, stage)
                    drawBoard(Player.Human, stage)
                    for (ship in ships) children.add(ship.shape)
                }
                // implemented after final click on computer board
                if (stage == GameStage.Resolution) {
                    children.removeAll(computerBoard)
                    children.removeAll(humanBoard)
                    for (ship in ships) children.remove(ship.shape)
                    // redraw
                    drawBoard(Player.Ai, stage)
                    drawBoard(Player.Human, stage)
                    for (ship in ships) {
                        children.add(ship.shape)
                        if (!game.isSunk(Player.Human, ship.shipId)) drift(ship)
                    }
                }
            }
        }
    }

    init {
        children.addAll(startButton, exitButton)

        // adds headings
        children.add(middleText)
        children.add(Text(122.5, 25.0, "My Formation").apply { font = customFont() })
        children.add(Text(615.0, 25.0, "Opponent’s Waters").apply { font = customFont() })

        // adds boards
        drawBoard(Player.Human, stage)
        drawBoard(Player.Ai, stage)

        var c = 'A'
        var posY = 50.0
        var oneX = 30.0
        var twoX = 545.0

        // x-axis labels
        for (i in 1..game.dimension) {
            posY += 30.0
            children.add(Text(oneX - 17.5, posY - 10.0, c.toString()))
            children.add(Text(oneX + 307.5, posY - 10.0, c.toString()))
            children.add(Text(twoX - 17.5, posY - 10.0, c.toString()))
            children.add(Text(twoX + 307.5, posY - 10.0, c.toString()))
            ++c
        }

        // y-axis labels
        posY = 50.0
        for (i in 1..game.dimension) {
            oneX += 30.0
            twoX += 30.0
            children.add(Text(oneX - 20.0, posY - 7.5, i.toString()))
            children.add(Text(oneX - 20.0, posY + 317.5, i.toString()))
            children.add(Text(twoX - 20.0, posY - 7.5, i.toString()))
            children.add(Text(twoX - 20.0, posY + 317.5, i.toString()))
        }

        // creates ships
        var posX = 362.5
        for (ship in Game.shipLength) {
            val rect = Rectangle(posX, 50.0, 20.0, ship.value * 30.0 - 10.0).apply {
                fill = Color.LIGHTGREEN
                stroke = Color.BLACK
            }
            ships.add(Ship(posX, posX, 50.0, rect, ship.key).apply {
                setListeners(this)
            })
            children.add(rect)
            posX += 32.5
        }

        // makes sure ships cannot be moved & calls startGame()
        startButton.setOnAction {
            for (ship in ships) {
                ship.shape.onMousePressed = null
                ship.shape.onMouseDragged = null
                ship.shape.onMouseReleased = null
            }
            game.startGame()
            startButton.isDisable = true
        }

        // exits program
        exitButton.setOnAction {
            Platform.exit()
        }

        // adds listeners to three GameStates i.e. HumanAttack, HumanWon, AiWon
        game.gameStateProperty.addListener { _, _, newGameState ->
            when (newGameState) {
                GameState.HumanAttack -> {
                    stage = GameStage.Loop
                    attackCell()
                }
                GameState.HumanWon -> {
                    stage = GameStage.Resolution
                    middleText.text = "You won!"
                    middleText.x = 397.5
                }
                GameState.AiWon -> {
                    stage = GameStage.Resolution
                    middleText.text = "You were defeated!"
                    middleText.x = 356.0
                }
                else -> {}
            }
        }
    }
}