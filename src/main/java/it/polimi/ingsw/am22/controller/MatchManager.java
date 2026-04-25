package it.polimi.ingsw.am22.controller;

import it.polimi.ingsw.am22.network.common.dto.MatchInfoDTO;
import it.polimi.ingsw.am22.network.server.VirtualView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry centrale di tutte le partite gestite dal server.
 *
 * Ogni partita è rappresentata da una coppia {@link GameController} +
 * {@link VirtualView} dedicata: la prima contiene lo stato logico,
 * la seconda tiene traccia dei canali dei giocatori iscritti a quella partita.
 *
 * Le operazioni sul registry sono thread-safe, mentre le operazioni interne a un
 * singolo match devono essere sincronizzate sul relativo {@link GameController}.
 */
public class MatchManager {

    /** Mappa matchId → handle contenente il controller e la view di un singolo match. */
    private final Map<String, MatchHandle> matches;

    public MatchManager() {
        this.matches = new ConcurrentHashMap<>();
    }

    /**
     * Crea un nuovo match con matchId generato automaticamente.
     *
     * @param hostNickname nickname del creatore (sarà il primo giocatore della lobby)
     * @param expectedPlayers numero totale di giocatori attesi (2-5)
     * @return handle del match appena creato
     */
    public MatchHandle createMatch(String hostNickname, int expectedPlayers) {
        if (hostNickname == null || hostNickname.isBlank()) {
            throw new IllegalArgumentException("hostNickname cannot be null or blank.");
        }
        if (expectedPlayers < 2 || expectedPlayers > 5) {
            throw new IllegalArgumentException("expectedPlayers must be between 2 and 5.");
        }

        String matchId = generateMatchId();
        GameController controller = new GameController(matchId);
        VirtualView view = new VirtualView();
        MatchHandle handle = new MatchHandle(controller, view);
        matches.put(matchId, handle);

        controller.addPlayerToLobby(hostNickname);
        controller.setExpectedPlayers(hostNickname, expectedPlayers);

        return handle;
    }

    /**
     * Restituisce l'handle del match indicato, oppure null se non esiste.
     *
     * @param matchId identificativo del match
     * @return handle del match, o null se non trovato
     */
    public MatchHandle getMatch(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return null;
        }
        return matches.get(matchId);
    }

    /**
     * Restituisce una fotografia delle partite aperte e non ancora iniziate.
     *
     * @return lista di MatchInfoDTO, vuota se non ci sono match aperti
     */
    public List<MatchInfoDTO> listOpenMatches() {
        List<MatchInfoDTO> result = new ArrayList<>();
        for (MatchHandle handle : matches.values()) {
            GameController controller = handle.controller();
            if (controller.hasStarted()) {
                continue;
            }
            result.add(new MatchInfoDTO(
                    controller.getMatchId(),
                    controller.getHostNickname(),
                    controller.getExpectedPlayers(),
                    controller.getLobbyPlayers().size(),
                    false
            ));
        }
        return result;
    }

    /**
     * Restituisce la lista di tutte le partite (anche quelle già avviate), utile per
     * operazioni di cleanup o diagnostica.
     *
     * @return lista di MatchInfoDTO con tutti i match noti al registry
     */
    public List<MatchInfoDTO> listAllMatches() {
        List<MatchInfoDTO> result = new ArrayList<>();
        for (MatchHandle handle : matches.values()) {
            GameController controller = handle.controller();
            result.add(new MatchInfoDTO(
                    controller.getMatchId(),
                    controller.getHostNickname(),
                    controller.getExpectedPlayers(),
                    controller.getLobbyPlayers().size(),
                    controller.hasStarted()
            ));
        }
        return result;
    }

    /**
     * Rimuove un match dal registry, tipicamente a fine partita o a seguito di
     * disconnessione irreversibile.
     *
     * @param matchId identificativo del match da rimuovere
     */
    public void removeMatch(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return;
        }
        MatchHandle removed = matches.remove(matchId);
        if (removed != null) {
            removed.view().closeAll();
        }
    }

    private String generateMatchId() {
        String id;
        do {
            id = UUID.randomUUID().toString().substring(0, 8);
        } while (matches.containsKey(id));
        return id;
    }

    /**
     * Coppia di controller e view che rappresentano un singolo match nel registry.
     */
    public record MatchHandle(GameController controller, VirtualView view) {
    }
}
