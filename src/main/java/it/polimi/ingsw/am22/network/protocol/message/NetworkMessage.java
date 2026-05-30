package it.polimi.ingsw.am22.network.protocol.message;

import java.io.Serializable;

/**
 * Marker interface for all messages exchanged between client and server.
 *
 * Extends Serializable because every message must travel both via
 * socket (through {@code ObjectOutputStream}) and via RMI.
 */
public interface NetworkMessage extends Serializable {
}
