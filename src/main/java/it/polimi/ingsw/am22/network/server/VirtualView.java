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

    public VirtualView(ModelDtoMapper mapper) {
        this.mapper = mapper;
        this.channelsByNickname = new ConcurrentHashMap<>();
        this.batchDepth = 0;
        this.pendingGame = null;
    }

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

    public void bindOrReplace(String nickname, ClientChannel channel) {
        if (nickname == null || nickname.isBlank() || channel == null) {
            return;
        }
        String key = normalize(nickname);
        channel.setBoundNickname(nickname);
        channelsByNickname.put(key, channel);
    }

    public ClientChannel getChannel(String nickname) {
        return nickname == null ? null : channelsByNickname.get(normalize(nickname));
    }

    public boolean isBound(String nickname) {
        return getChannel(nickname) != null;
    }

    public void unbind(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return;
        }
        ClientChannel channel = channelsByNickname.remove(normalize(nickname));
        if (channel != null) {
            channel.setBoundNickname(null);
        }
    }

    public void broadcast(ServerMessage message) {
        Collection<ClientChannel> snapshot = channelsByNickname.values();
        for (ClientChannel channel : snapshot) {
            safeSend(channel, message);
        }
    }

    public void sendTo(String nickname, ServerMessage message) {
        ClientChannel channel = getChannel(nickname);
        if (channel != null) {
            safeSend(channel, message);
        }
    }

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

    private void safeClose(ClientChannel channel) {
        try {
            channel.close();
        } catch (Exception ignored) {
        }
    }

    private String normalize(String value) {
        return value.strip().toLowerCase();
    }
}
