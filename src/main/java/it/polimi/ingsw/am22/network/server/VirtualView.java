package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameObserver;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStateMessage;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side virtual View.
 *
 * <p>Implements {@link GameObserver}: when registered on a {@link Game},
 * every model mutation triggers a {@link GameStateMessage} broadcast to
 * all bound {@link ClientChannel}s, regardless of the underlying transport
 * (Socket or RMI). This way the Model stays completely unaware of the
 * network, and the same observer interface used by the client-side
 * {@code GameView} drives the server-to-client broadcast pipeline.
 */
public class VirtualView implements GameObserver {
    private final Map<String, ClientChannel> channelsByNickname;
    private final ModelDtoMapper mapper;

    /**
     * Quando &gt; 0 le notifiche dell'observer non producono broadcast immediati:
     * basta tenere traccia che almeno una è arrivata e, alla chiusura del batch,
     * emettere un unico {@link GameStateMessage} con lo stato finale. Serve a
     * coalizzare le notifiche multiple che il model fa dentro una stessa azione
     * (setState, setActivePlayer, era/round change e il notify finale del
     * wrapper di {@link Game}), che altrimenti causano render duplicati al client.
     */
    private int batchDepth;
    private Game pendingGame;

    /**
     * Costruisce la VirtualView. Riusa il {@link ModelDtoMapper} condiviso
     * dal {@link NetworkGameService} per convertire il modello in DTO ad
     * ogni broadcast. Invocato una volta per ogni {@code MatchSession}.
     */
    public VirtualView(ModelDtoMapper mapper) {
        this.mapper = mapper;
        this.channelsByNickname = new ConcurrentHashMap<>();
        this.batchDepth = 0;
        this.pendingGame = null;
    }

    /**
     * Callback dell'interfaccia {@link GameObserver}: invocato dal model
     * ad ogni mutazione. Se siamo dentro un batch attivo ({@code batchDepth > 0})
     * salva solo il riferimento al Game e ritorna; altrimenti emette subito
     * un {@link GameStateMessage} broadcast a tutti i canali.
     */
    @Override
    public void gameStatusChanged(Game game) {
        synchronized (this) {
            if (batchDepth > 0) {
                pendingGame = game;
                return;
            }
        }
        broadcast(new GameStateMessage(mapper.toGameState(game)));
    }

    /**
     * Apre un batch: le successive notifiche dell'observer vengono trattenute
     * fino al corrispondente {@link #endBatch()}. I batch sono annidabili
     * (contatore), quindi si possono chiamare in modo difensivo.
     */
    public synchronized void beginBatch() {
        batchDepth++;
    }

    /**
     * Chiude un batch emettendo (se ne è arrivata almeno una durante il batch)
     * un singolo {@link GameStateMessage} con lo stato finale. Equivalente a
     * {@code endBatch(true)}.
     */
    public void endBatch() {
        endBatch(true);
    }

    /**
     * Chiude un batch. Quando l'ultimo batch annidato si chiude:
     * <ul>
     *     <li>{@code broadcast=true}: se durante il batch è arrivata almeno una
     *         notifica dell'observer, emette un singolo {@link GameStateMessage}
     *         con lo stato finale.</li>
     *     <li>{@code broadcast=false}: scarta le notifiche accumulate. Utile
     *         quando la chiamata avvolta serve solo per leggere/calcolare
     *         qualcosa (es. {@code determineWinner}) e lo stato verrà comunque
     *         pubblicato altrimenti — tipicamente come parte di un messaggio
     *         dedicato (es. {@code EndGameMessage}).</li>
     * </ul>
     */
    public void endBatch(boolean broadcast) {
        Game toBroadcast = null;
        synchronized (this) {
            if (batchDepth > 0) {
                batchDepth--;
            }
            if (batchDepth == 0) {
                if (broadcast) {
                    toBroadcast = pendingGame;
                }
                pendingGame = null;
            }
        }
        if (toBroadcast != null) {
            broadcast(new GameStateMessage(mapper.toGameState(toBroadcast)));
        }
    }

    /**
     * Lega un nickname al suo {@link ClientChannel}; se il nickname era gia'
     * legato a un altro canale, lo rimpiazza (utile in caso di riconnessione
     * o richiesta proveniente da un canale diverso). La normalizzazione
     * della chiave rende il match case-insensitive sui nickname.
     */
    public void bindOrReplace(String nickname, ClientChannel channel) {
        if (nickname == null || nickname.isBlank() || channel == null) {
            return;
        }
        String key = normalize(nickname);
        channel.setBoundNickname(nickname);
        channelsByNickname.put(key, channel);
    }

    /**
     * Recupera il canale attualmente legato al nickname indicato.
     * Restituisce {@code null} se nessun canale e' legato a quel nickname.
     */
    public ClientChannel getChannel(String nickname) {
        return nickname == null ? null : channelsByNickname.get(normalize(nickname));
    }

    /** True se esiste un binding attivo per il nickname indicato. */
    public boolean isBound(String nickname) {
        return getChannel(nickname) != null;
    }

    /**
     * Rimuove il binding per il nickname indicato e azzera il riferimento
     * al nickname sul canale. Il canale NON viene chiuso: la connessione
     * di rete resta viva.
     */
    public void unbind(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return;
        }
        ClientChannel channel = channelsByNickname.remove(normalize(nickname));
        if (channel != null) {
            channel.setBoundNickname(null);
        }
    }

    /**
     * Invia il messaggio a tutti i client correntemente legati a questa
     * VirtualView. Eventuali errori d'invio su un singolo canale lo fanno
     * chiudere e rimuovere senza interrompere il broadcast agli altri.
     */
    public void broadcast(ServerMessage message) {
        Collection<ClientChannel> snapshot = channelsByNickname.values();
        for (ClientChannel channel : snapshot) {
            safeSend(channel, message);
        }
    }

    /**
     * Invio puntuale (unicast) al solo client legato al nickname indicato.
     * Se il nickname non e' legato a nessun canale, il messaggio viene scartato.
     */
    public void sendTo(String nickname, ServerMessage message) {
        ClientChannel channel = getChannel(nickname);
        if (channel != null) {
            safeSend(channel, message);
        }
    }

    /**
     * Chiude tutti i canali legati e svuota la mappa interna. Invocato dal
     * {@code endGameCloser} di {@link NetworkGameService} dopo il broadcast
     * dell'{@link it.polimi.ingsw.am22.network.common.message.response.EndGameMessage},
     * con un grace period per dare ai client il tempo di leggere il messaggio.
     */
    public void closeAll() {
        for (ClientChannel channel : channelsByNickname.values()) {
            safeClose(channel);
        }
        channelsByNickname.clear();
    }

    /**
     * Snapshot dei canali correntemente legati. Restituisce una copia per
     * permettere al chiamante di iterare senza preoccuparsi di mutazioni
     * concorrenti sulla mappa interna.
     */
    public Collection<ClientChannel> snapshotChannels() {
        return new java.util.ArrayList<>(channelsByNickname.values());
    }

    /**
     * Sgancia tutti i nickname attualmente legati senza chiudere i canali:
     * le mappe vengono svuotate e {@link ClientChannel#getBoundNickname()}
     * azzerato per ciascuno. Serve quando una partita viene abbattuta ma i
     * client devono restare connessi al server (per poter rifare list/join).
     */
    public void unbindAllKeepingChannels() {
        for (ClientChannel channel : channelsByNickname.values()) {
            channel.setBoundNickname(null);
        }
        channelsByNickname.clear();
    }

    /**
     * Invio "best-effort" su un singolo canale: se la send fallisce
     * (canale chiuso, errore di rete, ...) chiude il canale e lo rimuove
     * dalla mappa, in modo che broadcast successivi non vi ripassino.
     */
    private void safeSend(ClientChannel channel, ServerMessage message) {
        try {
            channel.send(message);
        } catch (Exception e) {
            safeClose(channel);
            String nickname = channel.getBoundNickname();
            if (nickname != null) {
                channelsByNickname.remove(normalize(nickname));
            }
        }
    }

    /** Chiusura del canale che ignora eventuali eccezioni — usato in cleanup. */
    private void safeClose(ClientChannel channel) {
        try {
            channel.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * Forma canonica della chiave nickname (trim + lowercase) usata per
     * il lookup nella {@link #channelsByNickname}. Rende l'uguaglianza
     * dei nickname case-insensitive lato server.
     */
    private String normalize(String value) {
        return value.strip().toLowerCase();
    }
}
