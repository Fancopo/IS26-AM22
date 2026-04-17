package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualView {
    private final Map<String, ClientChannel> channelsByNickname;

    public VirtualView() {
        this.channelsByNickname = new ConcurrentHashMap<>();
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
