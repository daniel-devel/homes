import me.minecraft_server.homes.database.SQLDataSource;
import me.minecraft_server.homes.database.SQLDatabase;
import me.minecraft_server.homes.database.impl.H2Database;
import me.minecraft_server.homes.dto.HomeLocation;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SQLDatabaseTests {

    private SQLDataSource source;
    private SQLDatabase database;

    @BeforeAll
    public void prepareDatabase() {
        source = new SQLDataSource(H2Database.memory("Test"));
        database = new SQLDatabase(source);
    }

    @BeforeEach
    public void clearTables() {
        try (final var connection = source.getConnection();
             final var statement = connection.prepareStatement("DELETE FROM `homes`; DELETE FROM `players`;")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void tableExistenceTest() throws SQLException {
        try (final var connection = source.getConnection()) {
            final var metaData = connection.getMetaData();
            try (final var result = metaData.getTables(null, null, "players", null)) {
                Assertions.assertTrue(result.next(), "Table 'Players' does not exist!");
            }
            try (final var result = metaData.getTables(null, null, "homes", null)) {
                Assertions.assertTrue(result.next(), "Table 'Homes' does not exist!");
            }
        }
    }

    @Test
    public void playerTest() throws ExecutionException, InterruptedException {

        // Check registering players
        final var inUniqueId = new UUID(0, 1);
        final var inUsername = "Player";
        database.registerPlayer(inUniqueId, inUsername);
        final var outUsername = database.getPlayerName(inUniqueId);
        Assertions.assertEquals(inUsername, outUsername, "Resulting player name differs!");
        final var outUniqueIds = database.getPlayerUniqueId(inUsername);
        Assertions.assertEquals(1, outUniqueIds.size(), "Returned an invalid list size!");
        Assertions.assertEquals(inUniqueId, outUniqueIds.get(0), "Wrong unique id returned. Did not registered this!");

        // Check updating existing players
        final var inDifferentUsername = "NotAPlayer";
        database.registerPlayer(inUniqueId, inDifferentUsername);
        final var outDifferentUsername = database.getPlayerName(inUniqueId);
        Assertions.assertEquals(inDifferentUsername, outDifferentUsername, "Resulting player name differs! Maybe it's not overriding existing players?");

        // Check multiple players
        final var inDifferentUniqueId = new UUID(0, 2);
        database.registerPlayer(inDifferentUniqueId, inUsername);
        final var outYetAnotherUsername = database.getPlayerName(inDifferentUniqueId);
        final var outPreviousUsername = database.getPlayerName(inUniqueId);
        Assertions.assertEquals(inUsername, outYetAnotherUsername, "Resulting player name differs!");
        Assertions.assertEquals(inDifferentUsername, outPreviousUsername, "Resulting player name differs! Does it not override existing players?");
        final var outAgainUniqueIds = database.getPlayerUniqueId(inUsername);
        Assertions.assertEquals(1, outAgainUniqueIds.size(), "Returned an invalid list size!");
        Assertions.assertEquals(inDifferentUniqueId, outAgainUniqueIds.get(0), "Wrong unique id returned. We did not registered this!");
        final var outPreviousUniqueIds = database.getPlayerUniqueId(inDifferentUsername);
        Assertions.assertEquals(1, outPreviousUniqueIds.size(), "Returned an invalid list size!");
        Assertions.assertEquals(inUniqueId, outPreviousUniqueIds.get(0), "Wrong unique id returned. We did not registered this!");

        // Check multiple equal usernames
        database.registerPlayer(inUniqueId, inUsername);
        final var outFirstUsername = database.getPlayerName(inUniqueId);
        final var outSecondUsername = database.getPlayerName(inDifferentUniqueId);
        Assertions.assertEquals(inUsername, outFirstUsername, "Resulting player name differs!");
        Assertions.assertEquals(inUsername, outSecondUsername, "Resulting player name differs!");
        final var outTwoUniqueIds = database.getPlayerUniqueId(inUsername);
        Assertions.assertEquals(2, outTwoUniqueIds.size(), "Returned an invalid list size!");
        Assertions.assertTrue(outTwoUniqueIds.contains(inUniqueId), "List does not contains that player!");
        Assertions.assertTrue(outTwoUniqueIds.contains(inDifferentUniqueId), "List does not contain that player!");

    }

    @Test
    public void checkHomes() throws ExecutionException, InterruptedException {

        final var uniqueId_A = new UUID(0, 1);
        final var uniqueId_B = new UUID(0, 2);
        database.registerPlayer(uniqueId_A, "A");
        database.registerPlayer(uniqueId_B, "B");

        // Check adding homes
        final var home_A = new HomeLocation(1.0D, 2.0D, 3.0D, 90.0F, 30.0F, "worldA", "serverA");
        final var home_B = new HomeLocation(3.0D, 2.0D, 1.0D, 20.0F, 10.0F, "worldB", "serverB");
        checkHomes_AddHomes(uniqueId_A, "A", home_A, "B", home_B);
        checkHomes_AddHomes(uniqueId_B, "B", home_A, "A", home_B); // Homes are flipped to check if they override each other
        final var outHome_A = database.getHome(uniqueId_A, "A");
        final var outHome_B = database.getHome(uniqueId_A, "B");
        Assertions.assertEquals(home_A, outHome_A, "The homes differ. Did a different player override homes with the same name?");
        Assertions.assertEquals(home_B, outHome_B, "The homes differ. Did a different player override homes with the same name?");

        // Check if it fails adding homes where the player is not registered
        final var uniqueId_NotRegistered = new UUID(0, 3);
        final var failNotRegistered = database.addHome(uniqueId_NotRegistered, "A", home_A);
        Assertions.assertFalse(failNotRegistered, "This is supposed to fail because the player is not registered.");

        // Check setting homes
        checkHomes_SetHome(uniqueId_A, "A", home_B);
        checkHomes_SetHome(uniqueId_A, "B", home_A);
        checkHomes_SetHome(uniqueId_B, "A", home_A);
        checkHomes_SetHome(uniqueId_B, "B", home_B);

        // Getting homes
        final var homes_A = database.getHomes(uniqueId_A);
        Assertions.assertEquals(2, homes_A.size(), "Player A has a wrong amount of homes!");
        Assertions.assertEquals(home_B, homes_A.get("A"), "Wrong home for player A returned for home A!");
        Assertions.assertEquals(home_A, homes_A.get("B"), "Wrong home for player A returned for home B!");

        final var homes_B = database.getHomes(uniqueId_B);
        Assertions.assertEquals(2, homes_B.size(), "Player B has a wrong amount of homes!");
        Assertions.assertEquals(home_A, homes_B.get("A"), "Wrong home for player B returned for home A!");
        Assertions.assertEquals(home_B, homes_B.get("B"), "Wrong home for player B returned for home B!");

        final var homes_Invalid = database.getHomes(uniqueId_NotRegistered);
        Assertions.assertEquals(0, homes_Invalid.size(), "This player should have no homes.");

    }

    private void checkHomes_AddHomes(UUID uniqueId, String homeName_A, HomeLocation home_A, String homeName_B, HomeLocation home_B) throws ExecutionException, InterruptedException {

        // Assertions for tests
        Assertions.assertNotEquals(homeName_A, homeName_B, "This is a mistake in the tests. They should differ or the next tests will fail.");
        Assertions.assertNotEquals(home_A, home_B, "This is a mistake in the tests. They should differ or the next tests will fail.");

        // Adding homes
        checkHomes_AddHome(uniqueId, homeName_A, home_A);
        checkHomes_AddHome(uniqueId, homeName_B, home_B);

        // Check if home b did override a
        final var outHome_A = database.getHome(uniqueId, homeName_A);
        Assertions.assertEquals(home_A, outHome_A, "Homes differ! Did adding a different home override this one?");

    }

    private void checkHomes_AddHome(UUID uniqueId, String homeName, HomeLocation home) throws ExecutionException, InterruptedException {
        final var homeSuccess= database.addHome(uniqueId, homeName, home);
        Assertions.assertTrue(homeSuccess, "Adding homes must succeed here!");
        final var outHome = database.getHome(uniqueId, homeName);
        Assertions.assertEquals(home, outHome, "Homes differ!");
        final var homeFail = database.addHome(uniqueId, homeName, home);
        Assertions.assertFalse(homeFail, "Adding homes must fail here, because we already have that home!");
    }

    private void checkHomes_SetHome(UUID uniqueId, String homeName, HomeLocation home) throws ExecutionException, InterruptedException {
        database.setHome(uniqueId, homeName, home);
        final var outHome = database.getHome(uniqueId, homeName);
        Assertions.assertEquals(home, outHome, "Homes differ!");
    }

}
