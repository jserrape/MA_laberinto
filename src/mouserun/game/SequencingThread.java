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
import java.util.*;
import java.util.concurrent.*;

/**
 * Class SequencingThread moves all MouseInstances in a round robin manner. This
 * is executed as a Thread.
 */
public class SequencingThread extends Thread {

    private volatile boolean isAlive;

    public void run() {
        isAlive = true;

        ArrayList<Long> times = new ArrayList<Long>();

        while (isAlive) {
            times.clear();
            while (times.size() > 0) {
                long time = Collections.min(times);
                times.remove(time);
            }
            try {
                Thread.sleep(GameConfig.ROUND_SLEEP_TIME);
            } catch (InterruptedException itex) {
            }
        }
    }

    /**
     * Stops the Thread
     */
    public void kill() {
        isAlive = false;
    }

}
