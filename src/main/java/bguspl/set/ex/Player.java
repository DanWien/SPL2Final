package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;
    /**
     * This array indicates what slots the player chose to put his tokens.
     */
    private int[] tokens;
    /**
     * Number of tokens the player has placed so far.
     */
    public int numOfTokens = 0;
    /**
     * This queue holds the player's key presses by order of FIFO.
     */
    private Queue<Integer> keyQueue;
    private volatile int penalty = 0;
    private final int SETSIZE = 3;

    private Object playerLock = new Object();

    private Dealer dealer;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        tokens = new int[SETSIZE];
        for (int i = 0; i < tokens.length; i++)
            tokens[i] = -1;
        keyQueue = new ArrayBlockingQueue<>(SETSIZE);
        this.dealer = dealer;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            if (penalty == 1) {
                point();
                penalty = 0;
            } else if (penalty == 2) {
                penalty();
                penalty = 0;
            }
            if (!keyQueue.isEmpty() & numOfTokens <= SETSIZE) {
                int currSlot = keyQueue.poll();
                boolean exists = false;
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i] == currSlot)
                        exists = true;
                }
                if (exists)
                    removeToken(currSlot);
                else if (numOfTokens < SETSIZE) {
                    placeToken(currSlot);
                    if (numOfTokens == SETSIZE) {
                        synchronized (table.setQueue) {
                            table.setQueue.add(id);
                        }
                        try {
                            synchronized (playerLock) {
                                dealer.notifyDealer();
                                playerLock.wait();
                            }
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
        if (!human) try {
            aiThread.join();
        } catch (InterruptedException ignored) {
        }
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
            Random simulator = new Random();
            while (!terminate) {
                if (!table.shouldWait)
                    keyPressed(simulator.nextInt(env.config.tableSize));
            }
            env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        removeTokens();
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        if (!table.shouldWait) {
            if (table.slotToCard[slot] != null && keyQueue.size() < SETSIZE)
                keyQueue.add(slot);
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        score++;
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, score);
        int seconds = (int) (env.config.pointFreezeMillis / 1000);
        try {
            for (int i = seconds; i > 0; i--) {
                env.ui.setFreeze(id, i * 1000);
                playerThread.sleep(1000);
            }
        } catch (InterruptedException e) {
        }
        env.ui.setFreeze(id, 0);
        keyQueue.clear();
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        int seconds = (int) (env.config.penaltyFreezeMillis / 1000);
        for (int i = seconds; i > 0; i--) {
            env.ui.setFreeze(id, i * 1000);
            try {
                playerThread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        env.ui.setFreeze(id, 0);
        keyQueue.clear();
    }

    public int score() {
        return score;
    }

    public void removeTokens() {
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] != -1) {
                table.removeToken(id, i);
                tokens[i] = -1;
            }
        }
        numOfTokens = 0;
    }

    public void removeToken(int slot) {
        synchronized (table) {
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i] == slot)
                    tokens[i] = -1;
            }
            table.removeToken(id, slot);
            numOfTokens--;
        }
    }

    public void placeToken(int slot) {
        synchronized (table) {
            boolean placed = false;
            for (int i = 0; !placed && i < tokens.length; i++) {
                if (tokens[i] == -1) {
                    placed = true;
                    tokens[i] = slot;
                    table.placeToken(id, slot);
                    numOfTokens++;
                }
            }
        }
    }

    public Thread createThread() {
        playerThread = new Thread(this, "player" + id);
        return playerThread;
    }

    public int[] getTokens() {
        int[] copy = new int[tokens.length];
        for (int i = 0; i < copy.length; i++)
            copy[i] = tokens[i];
        return copy;
    }

    public void setPenalty(int i) {
        penalty = i;
    }

    public void release() {
        synchronized (playerLock) {
            playerLock.notifyAll();
        }
    }

    public boolean realSet() {
        for (int i : tokens) {
            if (i == -1 || table.slotToCard[i] == null)
                return false;
        }
        return true;
    }
}
