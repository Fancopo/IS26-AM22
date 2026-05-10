package it.polimi.ingsw.am22.view.tui;

import it.polimi.ingsw.am22.network.client.ClientSession;
import it.polimi.ingsw.am22.network.client.ClientUpdateHandler;
import it.polimi.ingsw.am22.network.common.dto.CardDTO;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.LeaderboardEntryDTO;
import it.polimi.ingsw.am22.network.common.dto.LobbyPlayerDTO;
import it.polimi.ingsw.am22.network.common.dto.LobbyStateDTO;
import it.polimi.ingsw.am22.network.common.dto.MatchInfoDTO;
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
import it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    /**
     * True when the client is shutting down because the server connection was
     * lost (vs. a user-initiated {@code quit}/{@code disconnect}). Used by
     * {@link TuiRunner} to pick the right exit code: 1 when the server dropped
     * us, 0 for a clean user-initiated shutdown.
     */
    private volatile boolean disconnectedByServer;

    /**
     * True between the {@code EndGameMessage} and the user's choice on the
     * end-game menu. Used to: (1) suppress the "Server connection lost"
     * banner when the server tears down the channel ~3s later (it's
     * expected, not an error); (2) signal to {@link TuiRunner} that no
     * server notification is needed when closing the local session.
     */
    private volatile boolean expectingDisconnect;

    /**
     * True while the player is sitting on the end-game menu (winner banner +
     * standings + leaderboard). The {@link TuiRunner} command loop uses this
     * to enable the {@code back}/{@code leaderboard} commands.
     */
    private volatile boolean inEndGame;

    /**
     * Set when the player picks {@code back} from the end-game menu. The
     * runner will then close this session, reopen a connection with the
     * stored transport/host/port, and start a fresh session — same flow
     * the GUI uses in {@code endGameAndShowMatches}.
     */
    private volatile boolean reconnectRequested;

    /**
     * Last {@code EndGameMessage} received, kept around so the
     * {@code leaderboard} command can re-print the historical leaderboard
     * without needing the server (which has already closed the channel).
     */
    private volatile EndGameMessage lastEndGame;

    public TuiView(ClientSession session) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
    }

    /**
     * @return {@code true} se è stato ricevuto un evento che richiede di terminare il client
     */
    public boolean isStopRequested() {
        return stopRequested;
    }

    /** @return {@code true} when the shutdown was caused by losing the server connection. */
    public boolean wasDisconnectedByServer() {
        return disconnectedByServer;
    }

    /** @return {@code true} while the player is on the end-game menu. */
    public boolean isInEndGame() {
        return inEndGame;
    }

    /**
     * @return {@code true} when the player picked {@code back} on the
     *         end-game menu and the runner should reopen a fresh session.
     */
    public boolean isReconnectRequested() {
        return reconnectRequested;
    }

    /**
     * @return {@code true} when the channel close is expected (post-EndGame):
     *         the runner uses this to skip the server-notify on local close
     *         and to avoid an exit-code-1 shutdown.
     */
    public boolean isExpectingDisconnect() {
        return expectingDisconnect;
    }

    /** Forza la richiesta di uscita (es. dopo comando {@code quit}). */
    public void requestStop() {
        this.stopRequested = true;
    }

    /**
     * Marks the session for a clean teardown + reopen by the runner. Used by
     * the end-game menu's {@code back} command: stops the command loop, but
     * unlike a user {@code quit} the runner will then start a brand-new
     * session against the same host/port — matching the GUI's behavior in
     * {@code endGameAndShowMatches}.
     */
    public void requestReconnect() {
        this.reconnectRequested = true;
        this.stopRequested = true;
    }

    /**
     * Re-prints the historical leaderboard from the cached
     * {@code EndGameMessage}. No server round-trip — by the time the menu is
     * up the channel is already closed. Bound to the {@code leaderboard}
     * command on the end-game menu.
     */
    public void replayHistoricalLeaderboard() {
        EndGameMessage end = lastEndGame;
        if (end == null) {
            println("(no leaderboard cached — has the game ended yet?)");
            return;
        }
        String me = session.getLocalNickname();
        int numPlayers = end.finalGameState() == null
                ? 0 : end.finalGameState().players().size();
        List<LeaderboardEntryDTO> leaderboard = end.leaderboard();
        synchronized (printLock) {
            System.out.println();
            if (leaderboard == null || leaderboard.isEmpty()) {
                System.out.println(Ansi.yellow(
                        "[Historical leaderboard unavailable — DB offline?]"));
            } else {
                printHistoricalLeaderboard(leaderboard, numPlayers, me);
            }
        }
    }

    /**
     * Stampa la sequenza che il comando {@code pick} sta per spedire al server,
     * risolvendo ogni id contro la upper/lower row dell'ultimo {@code GameStateDTO}
     * conosciuto. Ogni token mostra posizione, id e {@code detailType}
     * (es. {@code BUILDER}, {@code BUILDING}, {@code HUNTER*}/{@code HUNTER},
     * {@code INVENTOR-D}…), colorato come nelle righe della board, così che il
     * giocatore possa verificare a colpo d'occhio l'ordine PRIMA di vedere la
     * risposta del server. Risolve il problema dell'ambiguità del comando
     * {@code pick 9 98}: senza echo, il giocatore non sa se ha messo prima il
     * Builder o prima il Building, e l'effetto su food/sconto cambia.
     *
     * <p>Carte non trovate (id sbagliato, già state pescate, ecc.) vengono
     * marcate con {@code (?)}: il server le rifiuterà, ma intanto il
     * giocatore vede subito quale è il problema.
     */
    public void echoPickOrder(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        GameStateDTO state = session.getLatestGameState();
        List<CardDTO> board = new ArrayList<>();
        if (state != null) {
            if (state.upperRow() != null) board.addAll(state.upperRow());
            if (state.lowerRow() != null) board.addAll(state.lowerRow());
        }
        StringBuilder sb = new StringBuilder("Picking in order: ");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(" -> ");
            String id = ids.get(i);
            CardDTO c = findCardById(board, id);
            String body = "[" + (i + 1) + "] " + id
                    + (c == null ? "(?)" : "(" + c.detailType() + ")");
            sb.append(c == null ? body : colorizeCard(c, body));
        }
        println(Ansi.green(Ansi.BOLD + ">>> ") + sb);
    }

    private CardDTO findCardById(List<CardDTO> cards, String id) {
        if (id == null) return null;
        for (CardDTO c : cards) {
            if (id.equals(c.id())) return c;
        }
        return null;
    }

    // -------------------- ClientUpdateHandler --------------------

    @Override
    public void onServerMessage(ServerMessage message) {
        // Ogni tipo di messaggio ha un rendering dedicato.
        switch (message) {
            case MatchesListMessage list       -> renderMatchesList(list.matches());
            case MatchJoinedMessage joined     -> renderMatchJoined(joined);
            case LobbyStateMessage lobby       -> renderLobby(lobby.lobbyState());
            case GameStartedMessage started    -> renderGameStarted(started.initialGameState());
            case GameStateMessage state        -> renderGameState(state.gameState());
            case EndGameMessage end            -> renderEndGame(end);
            case MatchClosedMessage closed     -> {
                // Match abortito da remoto: stampiamo l'avviso ma NON chiudiamo
                // il client. La connessione col server resta viva (il server non
                // chiude più i canali in questo scenario) e il ClientSession ha
                // già pulito il binding locale, quindi il giocatore può tornare
                // a usare list/create/join come dalla situazione iniziale.
                println(Ansi.red(Ansi.BOLD + "[MATCH CLOSED] " + Ansi.RESET) + closed.reason());
                println(Ansi.dim("(back to matches selection — type 'list' to see open matches)"));
            }
            case ErrorMessage err              -> println(Ansi.red("[ERROR] ") + err.message());
            case InfoMessage info              -> println(Ansi.yellow("[INFO]  ") + info.message());
            default                            -> println("[?] " + message);
        }
    }

    @Override
    public void onConnectionClosed(Throwable cause) {
        if (expectingDisconnect) {
            // EndGameMessage just arrived; the server tears down the channel
            // ~3s later by design. This is normal end-of-match cleanup, not
            // a failure: don't surface the scary banner and don't stop the
            // command loop — the player is sitting on the end-game menu and
            // can still pick back / leaderboard / exit.
            return;
        }
        // Unified disconnect message. Exception details are intentionally
        // omitted: they're noise for the player. A developer who needs them
        // can add a verbose flag.
        println(Ansi.red("[CONN] Server connection lost  closing client."));
        disconnectedByServer = true;
        requestStop();
    }

    // -------------------- Rendering --------------------

    private void renderMatchesList(List<MatchInfoDTO> matches) {
        synchronized (printLock) {
            System.out.println();
            System.out.println("=== OPEN MATCHES ===");
            if (matches.isEmpty()) {
                System.out.println("(no open matches — use 'create <expectedPlayers> <nickname>' to start one)");
            } else {
                for (MatchInfoDTO info : matches) {
                    System.out.println(String.format("  %s  host=%s  %d/%s  %s",
                            info.matchId(),
                            info.hostNickname(),
                            info.currentPlayers(),
                            info.expectedPlayers() > 0 ? String.valueOf(info.expectedPlayers()) : "?",
                            info.started() ? "(started)" : "(open)"));
                }
                System.out.println("Use 'join <matchId> <nickname>' to enter one.");
            }
            System.out.println("====================");
        }
    }

    private void renderMatchJoined(MatchJoinedMessage joined) {
        println(Ansi.green(Ansi.BOLD + "[JOINED] " + Ansi.RESET)
                + "match " + joined.matchId() + " as " + joined.nickname());
    }

    private void renderLobby(LobbyStateDTO lobby) {
        synchronized (printLock) {
            System.out.println();
            System.out.println("=== LOBBY ===");
            System.out.println("Match id        : " + lobby.matchId());
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
            System.out.print(Ansi.CLEAR_SCREEN);
            System.out.println(Ansi.magenta(Ansi.BOLD + "=== GAME STATE ==="));
            System.out.println("Round: " + Ansi.bold(String.valueOf(state.currentRound()))
                    + " | Era: " + Ansi.bold(state.currentEra())
                    + " | Phase: " + Ansi.bold(state.currentPhase()));
            System.out.println("Active player: " + Ansi.yellow(Ansi.BOLD + state.activePlayer()));
            System.out.println(sectionHeader("Players"));
            for (PlayerDTO p : state.players()) {
                String line = String.format("  %s%s [%s]  PP=%d (proj %d)  food=%d",
                        p.active() ? "> " : "  ",
                        p.nickname(),
                        p.totemColor(),
                        p.prestigePoints(),
                        p.projectedFinalPrestigePoints(),
                        p.food());
                System.out.println(p.active() ? Ansi.bold(line) : line);
                if (!p.tribeCharacters().isEmpty()) {
                    System.out.println("      tribe    : " + summarizeCards(p.tribeCharacters()));
                }
                if (!p.buildings().isEmpty()) {
                    System.out.println("      buildings: " + summarizeCards(p.buildings()));
                }
            }
            System.out.println(sectionHeader("Offer tiles"));
            for (OfferTileDTO t : state.offerTrack()) {
                String status = t.occupiedBy() == null
                        ? Ansi.dim("(free)")
                        : "occupied by " + t.occupiedBy();
                System.out.println(String.format("  %c  upper=%d lower=%d food=%d  %s",
                        t.letter(),
                        t.upperCardsToTake(),
                        t.lowerCardsToTake(),
                        t.foodReward(),
                        status));
            }
            System.out.println(sectionHeader("Upper row"));
            System.out.println("  " + summarizeCards(state.upperRow()));
            System.out.println(sectionHeader("Lower row"));
            System.out.println("  " + summarizeCards(state.lowerRow()));
            System.out.println(sectionHeader("Turn order"));
            for (TurnSlotDTO slot : state.turnOrder()) {
                String lastSpace = slot.lastSpace() ? Ansi.red(" (last)") : "";
                // Quando il foodBonus è negativo, lo slot impone la penalità:
                // l'ultimo player che finisce qui senza cibo perde 2 PP.
                // Lo esplicitiamo accanto al food invece di lasciarlo implicito.
                String penalty = slot.foodBonus() < 0 ? "/points=-2" : "";
                System.out.println(String.format("  pos=%d food=%d%s%s %s",
                        slot.positionIndex(),
                        slot.foodBonus(),
                        penalty,
                        lastSpace,
                        slot.occupiedBy() == null ? "" : "-> " + slot.occupiedBy()));
            }
            System.out.println(Ansi.magenta(Ansi.BOLD + "=================="));
            // If it's my turn, contextual command hint.
            String me = session.getLocalNickname();
            if (me != null && me.equalsIgnoreCase(state.activePlayer())) {
                System.out.println();
                System.out.println(Ansi.green(Ansi.BOLD + "*** YOUR TURN — phase: " + state.currentPhase() + " ***"));
                System.out.println(Ansi.dim("    Commands: place <letter> | pick <id...> | bonus <id>"));
                System.out.print(Ansi.BELL); // beep / flash della finestra
            }
        }
    }

    /** Magenta-bold section header used to break the game-state output into blocks. */
    private String sectionHeader(String label) {
        return Ansi.magenta(Ansi.BOLD + "-- " + label + " --");
    }

    private static final DateTimeFormatter LB_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private void renderEndGame(EndGameMessage end) {
        WinnerDTO winner = end.winner();
        GameStateDTO finalState = end.finalGameState();
        List<LeaderboardEntryDTO> leaderboard = end.leaderboard() == null
                ? List.of() : end.leaderboard();
        Map<String, Integer> positions = end.positionByNickname() == null
                ? Map.of() : end.positionByNickname();
        int numPlayers = finalState == null ? 0 : finalState.players().size();

        // Stash for on-demand re-render via 'leaderboard' on the end-game menu.
        this.lastEndGame = end;
        // Server closes the channel ~3s after this message: silence
        // onConnectionClosed() and stay on the menu instead of exiting.
        this.expectingDisconnect = true;
        this.inEndGame = true;

        String me = session.getLocalNickname();

        synchronized (printLock) {
            // Deliberately no CLEAR_SCREEN: previous game-state output stays
            // scrollable above the summary, mirroring the GUI which keeps the
            // last board visible behind the EndGameScreen panel. The previous
            // version delegated to renderGameState() which DID clear the
            // screen, wiping the WINNER banner.
            System.out.println();
            System.out.println(Ansi.yellow(Ansi.BOLD
                    + "================================================="));
            System.out.println(Ansi.yellow(Ansi.BOLD
                    + "                  G A M E   O V E R              "));
            System.out.println(Ansi.yellow(Ansi.BOLD
                    + "================================================="));
            if (winner != null) {
                System.out.println(Ansi.yellow(Ansi.BOLD + ">> WINNER: "
                        + winner.nickname() + " [" + winner.totemColor() + "]"));
                System.out.println("   Prestige Points: "
                        + Ansi.bold(String.valueOf(winner.finalPrestigePoints()))
                        + "   Food left: " + winner.remainingFood());
            } else {
                System.out.println(Ansi.yellow("   (no winner declared)"));
            }

            // Final standings, sorted PP desc then food desc — same ordering
            // as the GUI's standings table.
            System.out.println();
            System.out.println(sectionHeader("Final standings (this match)"));
            if (finalState != null) {
                List<PlayerDTO> sorted = new ArrayList<>(finalState.players());
                sorted.sort(Comparator
                        .comparingInt(PlayerDTO::prestigePoints).reversed()
                        .thenComparingInt(PlayerDTO::food).reversed());
                int rank = 1;
                for (PlayerDTO p : sorted) {
                    boolean isWinner = winner != null
                            && p.nickname().equals(winner.nickname());
                    boolean isMe = me != null && me.equalsIgnoreCase(p.nickname());
                    String marker = (isWinner ? " *" : "") + (isMe ? "  <- you" : "");
                    String line = String.format(" %2d. %-20s [%-7s]  PP=%-3d  food=%-2d%s",
                            rank++, p.nickname(), p.totemColor(),
                            p.prestigePoints(), p.food(), marker);
                    if (isWinner) {
                        System.out.println(Ansi.yellow(Ansi.BOLD + line));
                    } else if (isMe) {
                        System.out.println(Ansi.green(line));
                    } else {
                        System.out.println(line);
                    }
                    if (!p.tribeCharacters().isEmpty()) {
                        System.out.println("       tribe    : "
                                + summarizeCards(p.tribeCharacters()));
                    }
                    if (!p.buildings().isEmpty()) {
                        System.out.println("       buildings: "
                                + summarizeCards(p.buildings()));
                    }
                }
            }

            // DB-backed historical position + leaderboard.
            System.out.println();
            if (positions.isEmpty()) {
                System.out.println(Ansi.yellow(
                        "[Historical leaderboard unavailable — DB offline?]"));
            } else if (me != null && positions.containsKey(me)) {
                System.out.println(Ansi.green(Ansi.BOLD + String.format(
                        ">> Your position in all %d-player matches: #%d (out of %d)",
                        numPlayers, positions.get(me), leaderboard.size())));
            }

            if (!leaderboard.isEmpty()) {
                System.out.println();
                printHistoricalLeaderboard(leaderboard, numPlayers, me);
            }

            // End-game menu — mirrors the three GUI buttons
            // (Back to matches / Show full leaderboard / Exit).
            System.out.println();
            System.out.println(Ansi.cyan(Ansi.BOLD + "-- What's next? --"));
            System.out.println("  back         return to matches selection (reopens a connection)");
            System.out.println("  leaderboard  re-display the historical leaderboard");
            System.out.println("  exit         quit the client");
            System.out.println();
        }
        // NOTE: deliberately no requestStop() here — the GUI keeps its window
        // open after the EndGameScreen and lets the user click a button; the
        // TUI now does the same via the menu above.
    }

    private void printHistoricalLeaderboard(List<LeaderboardEntryDTO> leaderboard,
                                            int numPlayers, String me) {
        System.out.println(Ansi.magenta(Ansi.BOLD + "-- Historical leaderboard ("
                + numPlayers + "-player matches) --"));
        int rank = 1;
        for (LeaderboardEntryDTO row : leaderboard) {
            String date = row.endDate() == null ? "" : row.endDate().format(LB_DATE_FMT);
            boolean isMe = me != null && me.equals(row.nickname());
            String line = String.format(" %3d. %-20s %4d   %s",
                    rank++, row.nickname(), row.score(), date);
            System.out.println(isMe ? Ansi.green(line) : line);
        }
    }

    private String summarizeCards(List<CardDTO> cards) {
        if (cards == null || cards.isEmpty()) return Ansi.dim("(none)");
        // Build the joined string with a single space between tokens, WITHOUT a trailing
        // separator. We must not call .trim() on the result: each colored token begins
        // with the ESC byte (, 0x1B), and String.trim() strips every character
        // with code <= 0x20 (space) — including ESC. That would erase the leading
        // ANSI escape from the first card and the terminal would print "[33m..." raw
        // instead of coloring the token.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(' ');
            CardDTO c = cards.get(i);
            String token = c.id() + "(" + c.detailType() + ")";
            sb.append(colorizeCard(c, token));
        }
        return sb.toString();
    }

    /**
     * Colors a card token by category so the player can tell at a glance what is on
     * the board: characters in blue (the engine you're building), buildings in yellow
     * (cost food, attention), events in red (hostile / unpickable). The food-icon
     * Hunter (detail type {@code HUNTER*}) is also bolded so it stands out from
     * the regular Hunter.
     */
    private String colorizeCard(CardDTO c, String token) {
        String category = c.category() == null ? "" : c.category();
        String detail = c.detailType() == null ? "" : c.detailType();
        return switch (category) {
            case "BUILDING" -> Ansi.yellow(token);
            case "EVENT"    -> Ansi.red(token);
            case "CHARACTER" -> detail.endsWith("*")
                    ? Ansi.blue(Ansi.BOLD + token)
                    : Ansi.blue(token);
            default -> token;
        };
    }

    private void println(String line) {
        synchronized (printLock) {
            System.out.println(line);
        }
    }
}
