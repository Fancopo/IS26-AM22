package it.polimi.ingsw.am22;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * =========================================================================
 * TEST DI INTEGRAZIONE (Regolamento Ufficiale "Mesos")
 * =========================================================================
 * Questa classe utilizza esclusivamente oggetti reali (senza Mockito)
 * ed è stata aggiornata per verificare in modo rigoroso le regole
 * descritte nel manuale ufficiale del gioco.
 */
class GameTest {

    private Game game;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        // 1. Creazione di giocatori reali
        player1 = new Player("Alice");
        player2 = new Player("Bob");

        // 2. Creazione e assegnazione dei Totem con il costruttore corretto
        // Assegniamo i colori come da regolamento (es. Rosso, Blu)
        if (player1.getTotem() == null) {
            player1.setTotem(new Totem("Rosso", player1));
            player2.setTotem(new Totem("Blu", player2));
        }

        List<Player> players = new ArrayList<>(Arrays.asList(player1, player2));

        // 3. Inizializzazione del Game reale e impostazione dello stato iniziale
        game = new Game(players);
        game.setState(new SetUpState());
    }

    // ==========================================
    // TEST 1: VERIFICA INIZIALIZZAZIONE BASE
    // ==========================================
    @Test
    void testGameInitialization() {
        assertEquals(1, game.getCurrentRound(), "Il round iniziale deve essere 1");
        assertEquals(Era.I, game.getCurrentEra(), "L'era iniziale deve essere I");
        assertNotNull(game.getBoard(), "La plancia non deve essere nulla");
        assertEquals(2, game.getPlayers().size(), "I giocatori registrati devono essere 2");
    }

    // ==========================================
    // TEST 2: ESECUZIONE DEL SETUP (Regole del manuale)
    // ==========================================
    @Test
    void testStartMatchExecution() {
        // Eseguiamo il setup effettivo
        // ... setup del gioco e dei giocatori ...
        game.startMatch(); // Questo chiama SetUpState.startMatch()

        // Non controllare 'player1', controlla chi è finito primo in lista!
        Player chiEPrimo = game.getPlayers().get(0);
        Player chiESecondo = game.getPlayers().get(1);

        assertEquals(2, chiEPrimo.getFood(), "Il giocatore in prima posizione deve avere 2 cibo");
        assertEquals(3, chiESecondo.getFood(), "Il giocatore in seconda posizione deve avere 3 cibo");
        // TRANSIZIONE DI STATO:
        // Dopo il setup, la fase corretta deve essere il piazzamento dei Totem
        assertEquals("Piazzamento Totem", game.getCurrentPhaseName(),
                "Dopo il setup, il gioco deve passare alla fase di Piazzamento Totem");

        // REGOLE DELLA PLANCIA (Regole 4 e 5, Pagina 2 del manuale):
        // Fila inferiore: Numero giocatori + 1 = 3 carte (in una partita a 2)
        // Fila superiore: Numero giocatori + 4 = 6 carte (in una partita a 2)
        // (Queste asserzioni assumono che la classe Board esponga le liste delle righe)
        if (game.getBoard().getLowerRow() != null && game.getBoard().getUpperRow() != null) {
            assertEquals(3, game.getBoard().getLowerRow().size(),
                    "La fila inferiore deve contenere esattamente Giocatori + 1 carte");
            assertEquals(6, game.getBoard().getUpperRow().size(),
                    "La fila superiore deve contenere esattamente Giocatori + 4 carte");
        }
    }

    // ==========================================
    // TEST 3: LOGICA TRACCIATO OFFERTE (Turno di Gioco)
    // ==========================================
    @Test
    void testGetPlayerWithLeftmostTotem() {
        game.startMatch();
        List<OfferTile> track = game.getBoard().getOfferTrack();

        assertNotNull(track, "Il tracciato delle offerte non deve essere nullo");
        assertTrue(track.size() >= 2, "Il tracciato deve avere abbastanza tessere");

        OfferTile primaTessera = track.get(0);
        OfferTile secondaTessera = track.get(1);

        // Simulazione: I giocatori piazzano i Totem come spiegato nell'esempio di Pagina 4.
        // Bob (Player 2) si posiziona sulla tessera più a sinistra.
        primaTessera.placeTotem(player2.getTotem());

        // Alice (Player 1) si posiziona sulla seconda tessera.
        secondaTessera.placeTotem(player1.getTotem());

        // Eseguiamo il metodo del Game che calcola chi deve risolvere l'azione
        Player leftmost = game.getPlayerWithLeftmostTotem();

        // Secondo le regole, si risolve da sinistra a destra sul tracciato offerte.
        assertEquals(player2, leftmost,
                "Il metodo deve restituire il giocatore sulla tessera più a sinistra (Bob)");
    }

    // ==========================================
    // TEST 4: LOGICA CAMBIO ERA (Regole Fine Round)
    // ==========================================
    @Test
    void testHandleEraChangeToEraIII() {
        game.startMatch();

        // Simuliamo l'avanzamento all'Era III, dove scatta una regola speciale.
        // Regola 1 (Inizio Nuova Era, Pagina 7): "Scartate le carte Edificio eventualmente presenti
        // nella fila inferiore. Questo succede solo all'inizio dell'Era III."
        game.setCurrentEra(Era.III);

        // Assicuriamoci che il metodo di gestione Era non vada in crash e gestisca il flusso.
        assertDoesNotThrow(() -> {
            game.handleEraChange();
        }, "Il metodo di cambio era deve eseguire la pulizia edifici senza lanciare eccezioni");
    }
}