package it.polimi.ingsw.am22.network.server.databases;

import java.util.List;

public class DbTestMain {

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
