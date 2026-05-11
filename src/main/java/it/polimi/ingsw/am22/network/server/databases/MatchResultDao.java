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

/**
 * DAO per la tabella {@code match_results}: persiste i risultati di ogni
 * partita finita e fornisce le query usate dal {@link NetworkGameService}
 * per costruire la classifica storica e calcolare la posizione di un
 * giocatore. Tutte le operazioni aprono una connessione, eseguono e la
 * chiudono in try-with-resources (nessun pool).
 */
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

    /**
     * Apre una connessione JDBC al database usando le credenziali
     * caricate da {@link DatabaseConfig}. Il chiamante e' responsabile
     * di chiuderla (tipicamente con try-with-resources).
     */
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.url(),
                DatabaseConfig.user(),
                DatabaseConfig.password());
    }

    /**
     * Salva i risultati di una partita appena terminata. Inserisce una
     * riga per ogni giocatore con nickname, punteggio finale, timestamp
     * corrente e numero di partecipanti — quest'ultimo permette di
     * filtrare la classifica per dimensione di partita.
     *
     * @param players  risultati per ciascun giocatore (nickname + score)
     * @param numPlayers numero totale di giocatori della partita
     */
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

    /**
     * Restituisce la classifica storica di tutte le partite con il numero
     * di giocatori indicato, ordinata per punteggio decrescente
     * (tie-break per data crescente, prima vince chi ha raggiunto il
     * punteggio per primo).
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
     * Calcola la posizione che un dato punteggio occuperebbe nella
     * classifica storica per partite con quel numero di giocatori
     * (1-based: posizione 1 = miglior punteggio). Conta quante righe
     * hanno un punteggio strettamente superiore e somma 1.
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

    /** Coppia nickname + punteggio finale, usata in input a {@link #saveMatch}. */
    public record PlayerResult(String nickname, int score) {}

    /** Riga di classifica letta dal DB: nickname, punteggio finale, data di fine partita. */
    public record RankRow(String nickname, int score, LocalDateTime endDate) {}
}
