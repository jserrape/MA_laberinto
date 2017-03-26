/**
 * MouseRun. A programming game to practice building intelligent things.
 * Copyright (C) 2013  Muhammad Mustaqim
 *
 * This file is part of MouseRun.
 *
 * MouseRun is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MouseRun is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MouseRun.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package mouserun.game;

import java.io.*;
import javax.swing.*;
import java.text.DecimalFormat;

/**
 * Class MouseRepresent is the link between the MouseController and the Mouse
 * graphical interface. It is also responsible for moving the mouse on each turn
 * towards the target grid of the mouse and display the correct asset image to
 * represent the moving direction.
 */
public class MouseRepresent {

    private GameController controller;
    private ImagedPanel represent;
    private JLabel nameLabel;
    private JLabel cheeseLabel;
    private int lastDirection;
    private String leftAsset;
    private String rightAsset;
    private String downAsset;
    private String upAsset;
    private String name;
    private DecimalFormat format;
    private int cheeseDisplayDuration;

    /**
     * Creates a new instance of the MouseRepresent
     *
     * @param controller The game controller, hosting the game.
     * @param represent The ImagedPanel that will display the Mouse on the game
     * interface
     * @param nameLabel The Label that will display the Mouse name on the game
     * interface
     * @param upAsset The asset that display the Mouse going upward
     * @param downAsset The asset that display the Mouse going downward
     * @param leftAsset The asset that display the Mouse going left
     * @param rightAsset The asset that display the Mouse going right
     */
    public MouseRepresent(GameController controller, ImagedPanel represent, JLabel nameLabel, JLabel cheeseLabel,
            String upAsset, String downAsset, String leftAsset, String rightAsset) {
        this.controller = controller;
        this.represent = represent;
        this.nameLabel = nameLabel;
        this.cheeseLabel = cheeseLabel;
        this.format = new DecimalFormat(GameConfig.CHEESE_NUMBER_FORMAT);
        this.cheeseDisplayDuration = 0;

        String name = "Nombre";
        if (name.length() > 13) {
            name = name.substring(0, 10) + "...";
        }

        this.name = name;

        this.nameLabel.setText("(" + "Quesos" + ") " + name);
        this.nameLabel.setSize(this.nameLabel.getPreferredSize());

        this.upAsset = upAsset;
        this.downAsset = downAsset;
        this.leftAsset = leftAsset;
        this.rightAsset = rightAsset;

        this.lastDirection = 0;
    }

    public String getName() {
        return this.name;
    }

}
