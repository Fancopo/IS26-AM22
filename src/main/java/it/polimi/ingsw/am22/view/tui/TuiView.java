package it.polimi.ingsw.am22.view.tui;

import it.polimi.ingsw.am22.controller.client.VirtualServer;
import it.polimi.ingsw.am22.controller.client.ClientSession;
import it.polimi.ingsw.am22.network.client.ServerHandler;
import it.polimi.ingsw.am22.network.protocol.dto.CardDTO;
import it.polimi.ingsw.am22.network.protocol.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.protocol.dto.LeaderboardEntryDTO;
import it.polimi.ingsw.am22.network.protocol.dto.LobbyPlayerDTO;
import it.polimi.ingsw.am22.network.protocol.dto.LobbyStateDTO;
import it.polimi.ingsw.am22.network.protocol.dto.MatchInfoDTO;
import it.polimi.ingsw.am22.network.protocol.dto.OfferTileDTO;
import it.polimi.ingsw.am22.network.protocol.dto.PlayerDTO;
import it.polimi.ingsw.am22.network.protocol.dto.TurnSlotDTO;
import it.polimi.ingsw.am22.network.protocol.dto.WinnerDTO;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.protocol.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchClosedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchRecoveringMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchJoinedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchesListMessage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Text view. Receives server messages on the reader/RMI thread and prints them
 * to the terminal; user input is read by {@link TuiRunner} and forwarded to
 * the {@link VirtualServer}.
 * No game logic here — rendering and input only.
 */
public final class TuiView implements ServerHandler {

    /** Serializes prints so concurrent threads don't interleave output lines. */
    private final Object printLock = new Object();

    private final ClientSession session;

    private volatile boolean stopRequested;

    /**
     * Set when the shutdown was caused by a lost server connection (vs. a
     * user-initiated quit). The runner uses this to pick the exit code:
     * 1 if the server dropped us, 0 for a clean local shutdown.
     */
    private volatile boolean disconnectedByServer;

    /**
     * True between EndGameMessage and the user's choice on the end-game menu.
     * Suppresses the "Server connection lost" banner when the server tears
     * down the channel ~3s later (it's expected, not an error), and tells
     * the runner not to send a disconnect on local close.
     */
    private volatile boolean expectingDisconnect;

    /** True while the player is sitting on the end-game menu. */
    private volatile boolean inEndGame;

    /**
     * Set when the player picks {@code back} from the end-game menu — the
     * runner will close this session and reopen a fresh one against the
     * same host/port (mirrors the GUI's endGameAndShowMatches).
     */
    private volatile boolean reconnectRequested;

    /** Kept around so the {@code leaderboard} command can re-print without a round-trip. */
    private volatile EndGameMessage lastEndGame;

    /**
     * Server-crash recovery handshake state. While {@link #awaitingReconnect}
     * is set, the next GameStartedMessage means the reconnection succeeded and
     * the next ErrorMessage (or a connection drop) means it was refused —
     * {@code TuiRunner} polls {@link #reconnectAccepted}/{@link #reconnectRejected}
     * to drive the retry loop.
     */
    private volatile boolean awaitingReconnect;
    private volatile boolean reconnectAccepted;
    private volatile boolean reconnectRejected;

    public TuiView(ClientSession session) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
    }

    public boolean isStopRequested() { return stopRequested; }
    public boolean wasDisconnectedByServer() { return disconnectedByServer; }
    public boolean isInEndGame() { return inEndGame; }
    public boolean isReconnectRequested() { return reconnectRequested; }
    public boolean isExpectingDisconnect() { return expectingDisconnect; }

    /** Session this view renders — used by the runner to keep a resumed session. */
    public ClientSession getSession() { return session; }

    /** Arms the recovery handshake watch before a ReconnectRequest is sent. */
    public void armReconnect() {
        awaitingReconnect = true;
        reconnectAccepted = false;
        reconnectRejected = false;
    }

    public boolean isReconnectAccepted() { return reconnectAccepted; }
    public boolean isReconnectRejected() { return reconnectRejected; }

    public void requestStop() {
        this.stopRequested = true;
    }

    /** End-game menu's {@code back}: stop the loop but signal the runner to reopen a session. */
    public void requestReconnect() {
        this.reconnectRequested = true;
        this.stopRequested = true;
    }

    /** Re-prints the cached leaderboard; the channel is already closed at this point. */
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
     * Echoes the pick order before sending it: resolves each id against the
     * latest known board and colors each token by detail type. Order matters
     * (Builder→Building gets a discount, Building→Builder doesn't), so the
     * echo lets the player catch a typo before seeing the server's reply.
     * Unknown ids are marked with {@code (?)}.
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

    /**
     * 'check <cardId>': looks the card up across every visible source — the
     * offer rows on the board and every player's tribe / buildings — and prints
     * full info plus a human-readable effect description.
     * <p>Searched in this order so a board card wins over a tribe card with the
     * same id (shouldn't happen in practice, but keeps results predictable):
     * upper row, lower row, then each player's tribe characters and buildings.
     */
    public void renderCardCheck(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            println("usage: check <cardId>");
            return;
        }
        GameStateDTO state = session.getLatestGameState();
        if (state == null) {
            println("(no game in progress — 'check' is only available during a match)");
            return;
        }

        String location = null;
        CardDTO card = findCardById(state.upperRow(), cardId);
        if (card != null) location = "upper row (board)";
        if (card == null) {
            card = findCardById(state.lowerRow(), cardId);
            if (card != null) location = "lower row (board)";
        }
        if (card == null) {
            for (PlayerDTO p : state.players()) {
                CardDTO hit = findCardById(p.tribeCharacters(), cardId);
                if (hit != null) {
                    card = hit;
                    location = "tribe of " + p.nickname();
                    break;
                }
                hit = findCardById(p.buildings(), cardId);
                if (hit != null) {
                    card = hit;
                    location = "buildings of " + p.nickname();
                    break;
                }
            }
        }

        if (card == null) {
            println(Ansi.red("[ERROR] ") + "no visible card with id '" + cardId + "'.");
            return;
        }

        synchronized (printLock) {
            System.out.println();
            System.out.println(Ansi.magenta(Ansi.BOLD + "-- Card "
                    + colorizeCard(card, card.id()) + " --"));
            System.out.println("  Location  : " + location);
            System.out.println("  Category  : " + card.category());
            System.out.println("  Detail    : " + colorizeCard(card, card.detailType()));
            System.out.println("  Era       : " + card.era());
            System.out.println("  Min players: " + card.minPlayers());
            if (card.foodCost() != null) {
                System.out.println("  Food cost : " + card.foodCost());
            }
            if (card.numStars() > 0) {
                System.out.println("  Stars     : " + card.numStars());
            }
            String desc = card.description();
            if (desc != null && !desc.isBlank()) {
                System.out.println("  Effect    : " + desc);
            }
        }
    }

    // -------------------- ServerHandler --------------------

    @Override
    public void onServerMessage(ServerMessage message) {
        message.accept(messageRenderer);
    }

    /** Polymorphic dispatcher: each {@code visit} renders one message kind. */
    private final ServerMessageVisitor messageRenderer = new ServerMessageVisitor() {
        @Override public void visit(MatchesListMessage m)  { renderMatchesList(m.matches()); }
        @Override public void visit(MatchJoinedMessage m)  { renderMatchJoined(m); }
        @Override public void visit(LobbyStateMessage m)   { renderLobby(m.lobbyState()); }
        @Override public void visit(GameStartedMessage m)  {
            if (awaitingReconnect) {
                reconnectAccepted = true;
                awaitingReconnect = false;
            }
            renderGameStarted(m.initialGameState());
        }
        @Override public void visit(GameStateMessage m)    { renderGameState(m.gameState()); }
        @Override public void visit(EndGameMessage m)      { renderEndGame(m); }
        @Override public void visit(MatchClosedMessage m) {
            // Aborted remotely; connection stays alive, session has cleared
            // its local binding — player can list/create/join again.
            println(Ansi.red(Ansi.BOLD + "[MATCH CLOSED] " + Ansi.RESET) + m.reason());
            println(Ansi.dim("(back to matches selection — type 'list' to see open matches)"));
        }
        @Override public void visit(MatchRecoveringMessage m) {
            if (awaitingReconnect) {
                reconnectAccepted = true;
                awaitingReconnect = false;
            }
            renderRecovering(m);
        }
        @Override public void visit(ErrorMessage m) {
            if (awaitingReconnect) {
                reconnectRejected = true;
                awaitingReconnect = false;
            }
            println(Ansi.red("[ERROR] ") + m.message());
        }
    };

    @Override
    public void onConnectionClosed(Throwable cause) {
        if (awaitingReconnect) {
            // The channel dropped before the server answered the reconnection:
            // treat it as a refusal so the recovery loop can retry.
            reconnectRejected = true;
            awaitingReconnect = false;
        }
        if (expectingDisconnect) {
            // EndGameMessage just arrived; the server tears down the channel
            // ~3s later by design. This is normal end-of-match cleanup, not
            // a failure: don't surface the scary banner and don't stop the
            // command loop — the player is sitting on the end-game menu and
            // can still pick back / leaderboard / exit.
            return;
        }
        disconnectedByServer = true;
        requestStop();
        if (session.isGameStarted()) {
            // The server crashed mid-match: the match was persisted server-side
            // and can be recovered. commandLoop is blocked reading stdin, so
            // the player must press ENTER to reach the recovery prompt.
            println(Ansi.red("[CONN] Server connection lost — your match was saved on the server."));
            println(Ansi.yellow("       Press ENTER to try to recover the previous match."));
        } else {
            println(Ansi.red("[CONN] Server connection lost — closing client."));
        }
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
                    String status = info.recovering()
                            ? Ansi.yellow("(reconnecting " + info.currentPlayers()
                                    + "/" + info.expectedPlayers() + ")")
                            : info.started() ? "(started)" : "(open)";
                    System.out.println(String.format("  %s  host=%s  %d/%s  %s",
                            info.matchId(),
                            info.hostNickname(),
                            info.currentPlayers(),
                            info.expectedPlayers() > 0 ? String.valueOf(info.expectedPlayers()) : "?",
                            status));
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
            // Hint only shown to the host before they pick a player count.
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

    /**
     * Renders the frozen board plus a banner saying the crash-recovered match
     * is paused until every player reconnects.
     */
    private void renderRecovering(MatchRecoveringMessage m) {
        if (m.gameState() != null) {
            renderGameState(m.gameState());
        }
        synchronized (printLock) {
            System.out.println();
            System.out.println(Ansi.yellow(Ansi.BOLD
                    + "*** MATCH PAUSED — WAITING FOR PLAYERS TO RECONNECT ("
                    + m.reconnectedCount() + "/" + m.totalPlayers() + ") ***"));
            if (m.missingNicknames() != null && !m.missingNicknames().isEmpty()) {
                System.out.println(Ansi.yellow("    Still missing: "
                        + String.join(", ", m.missingNicknames())));
            }
            System.out.println(Ansi.dim(
                    "    The game resumes automatically once every player is back."));
        }
    }

    /** Clears the screen and prints the full board summary; rings the bell on the player's turn. */
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
                // Negative foodBonus means the slot imposes the -2 PP penalty
                // for the player who finishes there without food.
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
                System.out.print(Ansi.BELL);
            }
        }
    }

    /** Magenta-bold section header used to break the game-state output into blocks. */
    private String sectionHeader(String label) {
        return Ansi.magenta(Ansi.BOLD + "-- " + label + " --");
    }

    private static final DateTimeFormatter LB_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Renders GAME OVER + winner + standings + historical leaderboard, then
     * the end-game menu. Sets {@link #expectingDisconnect} (server will close
     * the channel shortly) and {@link #inEndGame} (enables back/leaderboard).
     */
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
