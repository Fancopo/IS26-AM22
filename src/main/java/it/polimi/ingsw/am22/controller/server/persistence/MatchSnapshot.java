package it.polimi.ingsw.am22.controller.server.persistence;

import it.polimi.ingsw.am22.model.Game;

import java.io.Serial;
import java.io.Serializable;

/**
 * Serializable unit written to disk for a single started match.
 *
 * Holds the full {@link Game} object graph plus the lobby metadata
 * ({@code hostNickname}, {@code expectedPlayers}) needed to rebuild the
 * match's controller after a server restart.
 *
 * @param matchId         id of the persisted match
 * @param hostNickname    nickname of the match host
 * @param expectedPlayers number of players the match was started with
 * @param game            the in-progress game state
 */
public record MatchSnapshot(String matchId,
                            String hostNickname,
                            int expectedPlayers,
                            Game game) implements Serializable {

}
