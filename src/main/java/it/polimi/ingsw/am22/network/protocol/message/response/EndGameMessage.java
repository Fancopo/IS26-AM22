package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.protocol.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.protocol.dto.LeaderboardEntryDTO;
import it.polimi.ingsw.am22.network.protocol.dto.WinnerDTO;

import java.util.List;
import java.util.Map;

/**
 * Broadcast message sent when the match ends.
 *
 * Carries the winner's data, the final game-state snapshot and
 * the historical leaderboard (filtered by the same number of players)
 * together with each player's position in that leaderboard.
 *
 * @param winner             winning player data
 * @param finalGameState     final match state
 * @param leaderboard        historical leaderboard for matches with the
 *                           same number of players, sorted from best to worst
 * @param positionByNickname per-player position in {@code leaderboard}
 *                           (1 = best). Empty if persistence is unavailable.
 */
public record EndGameMessage(
        WinnerDTO winner,
        GameStateDTO finalGameState,
        List<LeaderboardEntryDTO> leaderboard,
        Map<String, Integer> positionByNickname
) implements ServerMessage {

    public EndGameMessage {
        leaderboard = leaderboard == null ? List.of() : List.copyOf(leaderboard);
        positionByNickname = positionByNickname == null ? Map.of() : Map.copyOf(positionByNickname);
    }

    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
