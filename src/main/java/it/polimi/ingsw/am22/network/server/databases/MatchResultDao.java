package it.polimi.ingsw.am22.network.server.databases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MatchResultDao {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Driver MySQL non trovato. Controlla la dipendenza " +
                    "mysql-connector-j nel pom.xml.", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.url(),
                DatabaseConfig.user(),
                DatabaseConfig.password());
    }

    public void saveMatch(List<PlayerResult> players, int numPlayers)
            throws SQLException {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Lista giocatori vuota");
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

    public record PlayerResult(String nickname, int score) {}

    public record RankRow(String nickname, int score, LocalDateTime endDate) {}
}
