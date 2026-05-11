package it.polimi.ingsw.am22.network.server.databases;

import java.util.List;

/**
 * Main di utilita' per testare manualmente il {@link MatchResultDao}.
 *
 * <p>Salva una partita fittizia a 3 giocatori, stampa la classifica delle
 * partite a 3 giocatori e la posizione di un punteggio dato. Va lanciato
 * a mano da IDE: serve solo per verificare che la connessione al DB e le
 * query funzionino, NON e' parte del flusso di gioco.
 */
public class DbTestMain {

    /**
     * Esegue uno scenario di test contro il database configurato:
     * insert di una partita fittizia, query della classifica e
     * calcolo della posizione di un punteggio.
     */
    public static void main(String[] args) throws Exception {
        MatchResultDao dao = new MatchResultDao();

        System.out.println(">> Salvo una partita finta a 3 giocatori...");
        dao.saveMatch(List.of(
                new MatchResultDao.PlayerResult("Mario", 25),
                new MatchResultDao.PlayerResult("Luigi", 22),
                new MatchResultDao.PlayerResult("Peach", 30)
        ), 3);
        System.out.println("   OK");

        System.out.println();
        System.out.println(">> Classifica partite a 3 giocatori:");
        List<MatchResultDao.RankRow> classifica = dao.getLeaderboard(3);
        int i = 1;
        for (MatchResultDao.RankRow r : classifica) {
            System.out.printf("   %d. %-15s %4d punti   (%s)%n",
                    i++, r.nickname(), r.score(), r.endDate());
        }

        System.out.println();
        int posMario = dao.getPosition(3, 25);
        System.out.println(">> Mario (25 punti) e' in posizione " + posMario);
    }
}
