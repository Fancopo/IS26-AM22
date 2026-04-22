package it.polimi.ingsw.am22.view.tui;

import it.polimi.ingsw.am22.network.client.ClientSession;
import it.polimi.ingsw.am22.network.client.ClientUpdateHandler;
import it.polimi.ingsw.am22.network.common.dto.CardDTO;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.LobbyPlayerDTO;
import it.polimi.ingsw.am22.network.common.dto.LobbyStateDTO;
import it.polimi.ingsw.am22.network.common.dto.OfferTileDTO;
import it.polimi.ingsw.am22.network.common.dto.PlayerDTO;
import it.polimi.ingsw.am22.network.common.dto.TurnSlotDTO;
import it.polimi.ingsw.am22.network.common.dto.WinnerDTO;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.InfoMessage;
import it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchClosedMessage;

import java.util.List;
import java.util.Objects;

/**
 * Vista testuale (TUI).
 *
 * <p>Implementa {@link ClientUpdateHandler}: riceve i messaggi dal server
 * (sul reader thread socket o sul thread RMI) e li stampa a terminale.
 * I comandi dell'utente vengono letti da {@link TuiRunner} sullo stdin e
 * inoltrati al {@link it.polimi.ingsw.am22.network.client.ClientController}
 * esposto dalla {@link ClientSession}.
 *
 * <p>Pattern MVC: questa classe è la View testuale; nota che:
 * <ul>
 *   <li>non contiene logica di gioco (solo rendering e parsing input);</li>
 *   <li>tutte le decisioni sono delegate al Controller remoto via ClientController.</li>
 * </ul>
 */
public final class TuiView implements ClientUpdateHandler {

    /** Oggetto di sincronizzazione per non mescolare righe di stampa concorrenti. */
    private final Object printLock = new Object();

    private final ClientSession session;

    /** Segnale usato da {@link TuiRunner} per uscire dal loop comandi. */
    private volatile boolean stopRequested;

    public TuiView(ClientSession session) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
    }

    /**
     * @return {@code true} se è stato ricevuto un evento che richiede di terminare il client
     */
    public boolean isStopRequested() {
        return stopRequested;
    }

    /** Forza la richiesta di uscita (es. dopo comando {@code quit}). */
    public void requestStop() {
        this.stopRequested = true;
    }

    // -------------------- ClientUpdateHandler --------------------

    @Override
    public void onServerMessage(ServerMessage message) {
        // Ogni tipo di messaggio ha un rendering dedicato.
        switch (message) {
            case LobbyStateMessage lobby       -> renderLobby(lobby.lobbyState());
            case GameStartedMessage started    -> renderGameStarted(started.initialGameState());
            case GameStateMessage state        -> renderGameState(state.gameState());
            case EndGameMessage end            -> renderEndGame(end.winner(), end.finalGameState());
            case MatchClosedMessage closed     -> {
                println("[MATCH CLOSED] " + closed.reason());
                requestStop();
            }
            case ErrorMessage err              -> println("[ERROR] " + err.message());
            case InfoMessage info              -> println("[INFO] " + info.message());
            default                            -> println("[?] " + message);
        }
    }

    @Override
    public void onConnectionClosed(Throwable cause) {
        if (cause == null) {
            println("[CONN] connection closed by the server.");
        } else {
            println("[CONN] connection lost: " + cause.getClass().getSimpleName()
                    + (cause.getMessage() == null ? "" : " - " + cause.getMessage()));
        }
        requestStop();
    }

    // -------------------- Rendering --------------------

    private void renderLobby(LobbyStateDTO lobby) {
        synchronized (printLock) {
            System.out.println();
            System.out.println("=== LOBBY ===");
            System.out.println("Host            : " + lobby.hostNickname());
            System.out.println("Expected players: " + (lobby.expectedPlayers() > 0
                    ? lobby.expectedPlayers()
                    : "(not set yet)"));
            System.out.println("Started         : " + lobby.started());
            System.out.println("Players in lobby:");
            for (LobbyPlayerDTO p : lobby.players()) {
                System.out.println("  - " + p.nickname()
                        + (p.host() ? " (host)" : "")
                        + (p.totemColor() == null ? "" : " [" + p.totemColor() + "]"));
            }
            // Suggerimento contestuale: se sono host e non ho ancora impostato il numero.
            String me = session.getLocalNickname();
            if (lobby.hostNickname() != null
                    && lobby.hostNickname().equalsIgnoreCase(me)
                    && lobby.expectedPlayers() <= 0) {
                System.out.println("(You are the host: use 'players <N>' to set the expected number.)");
            }
            System.out.println("===================");
        }
    }

    private void renderGameStarted(GameStateDTO state) {
        println(">>> GAME STARTED <<<");
        renderGameState(state);
    }

    private void renderGameState(GameStateDTO state) {
        synchronized (printLock) {
            System.out.println();
            System.out.println("=== GAME STATE ===");
            System.out.println("Round: " + state.currentRound()
                    + " | Era: " + state.currentEra()
                    + " | Phase: " + state.currentPhase());
            System.out.println("Active player: " + state.activePlayer());
            System.out.println("-- Players --");
            for (PlayerDTO p : state.players()) {
                System.out.println(String.format("  %s%s [%s]  PP=%d (proj %d)  food=%d",
                        p.active() ? "> " : "  ",
                        p.nickname(),
                        p.totemColor(),
                        p.prestigePoints(),
                        p.projectedFinalPrestigePoints(),
                        p.food()));
                if (!p.tribeCharacters().isEmpty()) {
                    System.out.println("      tribe    : " + summarizeCards(p.tribeCharacters()));
                }
                if (!p.buildings().isEmpty()) {
                    System.out.println("      buildings: " + summarizeCards(p.buildings()));
                }
            }
            System.out.println("-- Offer tiles --");
            for (OfferTileDTO t : state.offerTrack()) {
                System.out.println(String.format("  %c  upper=%d lower=%d food=%d  %s",
                        t.letter(),
                        t.upperCardsToTake(),
                        t.lowerCardsToTake(),
                        t.foodReward(),
                        t.occupiedBy() == null ? "(free)" : "occupied by " + t.occupiedBy()));
            }
            System.out.println("-- Upper row --");
            System.out.println("  " + summarizeCards(state.upperRow()));
            System.out.println("-- Lower row --");
            System.out.println("  " + summarizeCards(state.lowerRow()));
            System.out.println("-- Turn order --");
            for (TurnSlotDTO slot : state.turnOrder()) {
                System.out.println(String.format("  pos=%d food=%d%s %s",
                        slot.positionIndex(),
                        slot.foodBonus(),
                        slot.lastSpace() ? " (last)" : "",
                        slot.occupiedBy() == null ? "" : "-> " + slot.occupiedBy()));
            }
            System.out.println("==================");
            // Se è il mio turno, suggerimento contestuale di comando.
            String me = session.getLocalNickname();
            if (me != null && me.equalsIgnoreCase(state.activePlayer())) {
                System.out.println("(Your turn. Phase: " + state.currentPhase() + ".)");
                System.out.println("  Commands: place <letter> | pick <id...> | bonus <id>");
            }
        }
    }

    private void renderEndGame(WinnerDTO winner, GameStateDTO finalState) {
        synchronized (printLock) {
            System.out.println();
            System.out.println("*** GAME OVER ***");
            System.out.println("Winner: " + winner.nickname()
                    + " [" + winner.totemColor() + "]"
                    + "  PP=" + winner.finalPrestigePoints()
                    + "  food=" + winner.remainingFood());
            System.out.println("Final snapshot:");
        }
        renderGameState(finalState);
        requestStop();
    }

    private String summarizeCards(List<CardDTO> cards) {
        if (cards == null || cards.isEmpty()) return "(none)";
        StringBuilder sb = new StringBuilder();
        for (CardDTO c : cards) {
            sb.append(c.id()).append('(').append(c.detailType()).append(") ");
        }
        return sb.toString().trim();
    }

    private void println(String line) {
        synchronized (printLock) {
            System.out.println(line);
        }
    }
}
