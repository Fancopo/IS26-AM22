package it.polimi.ingsw.am22.controller.server.datebases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the {@code match_results} table: persists the result of every finished
 * match and provides the queries used by the server to build the historical
 * leaderboard and compute a player's position. Every operation opens a
 * connection, runs, and closes it in a try-with-resources (no pool).
 */
public class MatchResultDao {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "MySQL driver not found. Check the mysql-connector-j " +
                    "dependency in pom.xml.", e);
        }
    }

    /**
     * Opens a JDBC connection to the database using the credentials loaded from
     * {@link DatabaseConfig}. The caller is responsible for closing it
     * (typically with try-with-resources).
     */
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.url(),
                DatabaseConfig.user(),
                DatabaseConfig.password());
    }

    /**
     * Saves the results of a just-finished match. Inserts one row per player
     * with nickname, final score, the current timestamp and the number of
     * participants — the latter allows filtering the leaderboard by match size.
     *
     * @param players    per-player results (nickname + score)
     * @param numPlayers total number of players in the match
     * @throws SQLException if the insert fails
     */
    public void saveMatch(List<PlayerResult> players, int numPlayers)
            throws SQLException {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Empty player list");
        }
        String sql = "INSERT INTO match_results " +
                     "(nickname, final_score, end_date, num_players) " +
                     "VALUES (?, ?, ?, ?)";
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (PlayerResult p : players) {
                ps.setString(1, p.nickname());
                ps.setInt(2, p.score());
                ps.setTimestamp(3, now);
                ps.setInt(4, numPlayers);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Returns the historical leaderboard of all matches with the given player
     * count, ordered by descending score (tie-break by ascending date: whoever
     * reached the score first ranks higher).
     *
     * @param numPlayers the match size to filter by
     * @return the leaderboard rows
     * @throws SQLException if the query fails
     */
    public List<RankRow> getLeaderboard(int numPlayers) throws SQLException {
        String sql = "SELECT nickname, final_score, end_date " +
                     "FROM match_results WHERE num_players = ? " +
                     "ORDER BY final_score DESC, end_date ASC";
        List<RankRow> rows = new ArrayList<>();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, numPlayers);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new RankRow(
                            rs.getString("nickname"),
                            rs.getInt("final_score"),
                            rs.getTimestamp("end_date").toLocalDateTime()));
                }
            }
        }
        return rows;
    }

    /**
     * Computes the position a given score would occupy in the historical
     * leaderboard for matches with that player count (1-based: position 1 = best
     * score). Counts how many rows have a strictly higher score and adds 1.
     *
     * @param numPlayers  the match size to filter by
     * @param playerScore the score whose position is wanted
     * @return the 1-based leaderboard position
     * @throws SQLException if the query fails
     */
    public int getPosition(int numPlayers, int playerScore) throws SQLException {
        String sql = "SELECT 1 + COUNT(*) AS pos FROM match_results " +
                     "WHERE num_players = ? AND final_score > ?";
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, numPlayers);
            ps.setInt(2, playerScore);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("pos");
            }
        }
    }

    /**
     * Nickname + final score pair, used as input to {@link #saveMatch}.
     *
     * @param nickname the player's nickname
     * @param score    the player's final score
     */
    public record PlayerResult(String nickname, int score) {}

    /**
     * A leaderboard row read from the DB.
     *
     * @param nickname the player's nickname
     * @param score    the final score
     * @param endDate  the match end date
     */
    public record RankRow(String nickname, int score, LocalDateTime endDate) {}
}
