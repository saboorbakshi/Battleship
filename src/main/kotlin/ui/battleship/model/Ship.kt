package ui.battleship.model

import javafx.scene.shape.Rectangle

class Ship (var mainX: Double,
            var initX : Double,
            var initY : Double,
            var shape: Rectangle,
            var shipType : ShipType,
            var orientation : Orientation = Orientation.Vertical,
            var shipId : Int = -1)