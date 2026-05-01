package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * registro nickname → ClientChannel con ConcurrentHashMap.
 * Offre broadcast, sendTo, bindOrReplace, invio fault-tolerant (rimuove canali rotti).
 */
public class VirtualView {
    private final Map<String, ClientChannel> channelsByNickname;

    /** Crea una VirtualView con una mappa concorrente vuota. */
    public VirtualView() {
        this.channelsByNickname = new ConcurrentHashMap<>();
    }

    /**
     * Associa un canale a un nickname; se esisteva già un canale per quel
     * nickname (es. riconnessione) viene sostituito.
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
     * @param nickname nickname da cercare
     * @return il canale associato, oppure {@code null} se non presente
     */
    public ClientChannel getChannel(String nickname) {
        return nickname == null ? null : channelsByNickname.get(normalize(nickname));
    }

    /**
     * @param nickname nickname da verificare
     * @return {@code true} se esiste un canale associato a quel nickname
     */
    public boolean isBound(String nickname) {
        return getChannel(nickname) != null;
    }

    /**
     * Rimuove l'associazione nickname → canale, se presente.
     *
     * @param nickname nickname da rimuovere
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
     * Invia lo stesso messaggio a tutti i canali registrati.
     * I canali che sollevano eccezione durante l'invio vengono rimossi.
     *
     * @param message messaggio da inviare
     */
    public void broadcast(ServerMessage message) {
        Collection<ClientChannel> snapshot = channelsByNickname.values();
        for (ClientChannel channel : snapshot) {
            safeSend(channel, message);
        }
    }

    /**
     * Invia un messaggio al singolo canale associato al nickname indicato.
     *
     * @param nickname destinatario
     * @param message  messaggio da inviare
     */
    public void sendTo(String nickname, ServerMessage message) {
        ClientChannel channel = getChannel(nickname);
        if (channel != null) {
            safeSend(channel, message);
        }
    }

    /** Chiude tutti i canali e svuota la mappa. Usato alla terminazione della partita. */
    public void closeAll() {
        for (ClientChannel channel : channelsByNickname.values()) {
            safeClose(channel);
        }
        channelsByNickname.clear();
    }

    /**
     * Invio fault-tolerant: in caso di errore chiude e rimuove il canale
     * senza propagare l'eccezione, così da non interrompere il broadcast.
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

    /** Chiusura tollerante agli errori (li ignora). */
    private void safeClose(ClientChannel channel) {
        try {
            channel.close();
        } catch (Exception ignored) {
        }
    }

    /** Normalizza il nickname per l'uso come chiave della mappa (trim + lowercase). */
    private String normalize(String value) {
        return value.strip().toLowerCase();
    }
}
