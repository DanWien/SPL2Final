package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DealerTest {

    Dealer dealer;
    Util util;
    private UserInterface ui;
    private Table table;

    private Player[] players;

    private Logger logger;

    private Env env;

    @BeforeEach
    void setUp() {
        Properties properties = new Properties();
        properties.put("Rows", "3");
        properties.put("Columns", "4");
        properties.put("FeatureSize", "3");
        properties.put("FeatureCount", "4");
        properties.put("TableDelaySeconds", "0");
        properties.put("PlayerKeys1", "81,87,69,82,65,83,68,70,90,88,67,86");
        properties.put("PlayerKeys2", "85,73,79,80,74,75,76,59,77,44,46,47");
        MockLogger logger = new MockLogger();
        Config config = new Config(logger, properties);
        Player pl = new Player(env, dealer, table, 0, false);
        players = new Player[1];
        players[0] = pl;
        env = new Env(logger, config, new TableTest.MockUserInterface(), new TableTest.MockUtil());
        table = new Table(env, new Integer[config.tableSize], new Integer[config.deckSize]);
        dealer = new Dealer(env,  table, players);
       // assertDeck();
    }

 //   @AfterEach
   // void tearDown() {
  //      assertInvariants();
  //  }

    @Test
    void shuffleTable() {

        // calculate the expected number of cards when shuffling from a full deck
        int expectedNumOfCards = env.config.tableSize;

        // call the method we are testing
        dealer.shuffleTable();

        // check that every slot on the table was filled with a card
        assertEquals(expectedNumOfCards, table.countCards());

    }

    @Test
    void removeAllCardsFromTable() {
        dealer.shuffleTable();
        // calculate the expected number of cards when removing all the cards
        int expectedNumOfCards = 0;

        // call the method we are testing
        dealer.removeAllCardsFromTable();

        // check that every slot on the table was filled with a card
        assertEquals(expectedNumOfCards, table.countCards());

    }
    static class MockUserInterface implements UserInterface {
        @Override
        public void dispose() {}
        @Override
        public void placeCard(int card, int slot) {}
        @Override
        public void removeCard(int slot) {}
        @Override
        public void setCountdown(long millies, boolean warn) {}
        @Override
        public void setElapsed(long millies) {}
        @Override
        public void setScore(int player, int score) {}
        @Override
        public void setFreeze(int player, long millies) {}
        @Override
        public void placeToken(int player, int slot) {}
        @Override
        public void removeTokens() {}
        @Override
        public void removeTokens(int slot) {}
        @Override
        public void removeToken(int player, int slot) {}
        @Override
        public void announceWinner(int[] players) {}
    };

    static class MockUtil implements Util {
        @Override
        public int[] cardToFeatures(int card) {
            return new int[0];
        }

        @Override
        public int[][] cardsToFeatures(int[] cards) {
            return new int[0][];
        }

        @Override
        public boolean testSet(int[] cards) {
            return false;
        }

        @Override
        public List<int[]> findSets(List<Integer> deck, int count) {
            return null;
        }

        @Override
        public void spin() {}
    }

    static class MockLogger extends Logger {
        protected MockLogger() {
            super("", null);
        }
    }
}
