package it.polimi.ingsw.am22.view.server;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameObserver;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.server.transport.ClientChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side virtual View.
 *
 * <p>As a {@link GameObserver}, every model mutation triggers a
 * {@link GameStateMessage} broadcast to all bound {@link ClientChannel}s,
 * regardless of the transport. The Model stays unaware of the network.
 *
 * <p>Supports batching: a single user action may produce several model
 * notifications (setState, setActivePlayer, era/round change, ...). Inside
 * a batch we only remember a notification arrived and emit one final
 * {@link GameStateMessage} on close — avoids duplicate client renders.
 *
 * <p><b>Nickname policy.</b> Lookup is case-insensitive and locale-stable:
 * the map key is {@code nickname.strip().toLowerCase(Locale.ROOT)}, matching
 * the policy enforced by {@code GameController.containsNickname}. The
 * channel keeps the nickname in its original casing for display purposes
 * (see {@link ClientChannel#getBoundNickname}).
 */
public class ClientBroadcaster implements GameObserver {

    private final Map<String, ClientChannel> channelsByNickname;
    private final ModelDtoMapper mapper;

    private int batchDepth;
    private Game pendingGame;

    public ClientBroadcaster(ModelDtoMapper mapper) {
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

    public synchronized void beginBatch() {
        batchDepth++;
    }

    public void endBatch() {
        endBatch(true);
    }

    /**
     * Closes a batch. On the outermost close, if at least one notification
     * arrived during the batch and {@code broadcast} is true, emits a single
     * {@link GameStateMessage}. Pass {@code false} when the wrapped call is
     * only used to compute something (e.g. determineWinner) and the state
     * is delivered via a dedicated message (e.g. EndGameMessage).
     */
    public void endBatch(boolean broadcast) {
        Game toBroadcast = null;
        synchronized (this) {
            if (batchDepth > 0) batchDepth--;
            if (batchDepth == 0) {
                if (broadcast) toBroadcast = pendingGame;
                pendingGame = null;
            }
        }
        if (toBroadcast != null) {
            broadcast(new GameStateMessage(mapper.toGameState(toBroadcast)));
        }
    }

    /** Binds the nickname to the channel, replacing any previous binding (idempotent, case-insensitive). */
    public void bindOrReplace(String nickname, ClientChannel channel) {
        if (nickname == null || nickname.isBlank() || channel == null) return;
        channel.setBoundNickname(nickname);
        channelsByNickname.put(normalize(nickname), channel);
    }

    public ClientChannel getChannel(String nickname) {
        return nickname == null ? null : channelsByNickname.get(normalize(nickname));
    }

    public boolean isBound(String nickname) {
        return getChannel(nickname) != null;
    }

    /** Removes the binding without closing the channel. */
    public void unbind(String nickname) {
        if (nickname == null || nickname.isBlank()) return;
        ClientChannel channel = channelsByNickname.remove(normalize(nickname));
        if (channel != null) {
            channel.setBoundNickname(null);
        }
    }

    public void broadcast(ServerMessage message) {
        for (ClientChannel channel : channelsByNickname.values()) {
            safeSend(channel, message);
        }
    }

    public void closeAll() {
        for (ClientChannel channel : channelsByNickname.values()) {
            safeClose(channel);
        }
        channelsByNickname.clear();
    }

    public Collection<ClientChannel> snapshotChannels() {
        return new ArrayList<>(channelsByNickname.values());
    }

    /** Unbind all nicknames but keep the channels open (used when aborting a match). */
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
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
