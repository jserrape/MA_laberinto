package mouserun.game;

import java.util.*;

/**
 * Class SequencingThread moves all MouseInstances in a round robin manner. This
 * is executed as a Thread.
 */
public class SequencingThread extends Thread {

    private volatile boolean isAlive;

    @Override
    public void run() {
        isAlive = true;

        ArrayList<Long> times = new ArrayList<>();

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
