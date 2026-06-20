package it.polimi.ingsw.am22.model;

import java.io.Serializable;

/**
 * A player's totem (pawn). It can sit either on an {@link OfferTile} or on a
 * {@link Slot} of the turn-order tile, but never both at once: moving it to one
 * location automatically detaches it from the other.
 */
public class Totem implements Serializable {
    private final String color;
    private final Player owner;
    private OfferTile currentOfferTile;
    private Slot currentSlot;

    /**
     * @param color the totem colour
     * @param owner the player who owns this totem
     * @throws IllegalArgumentException if {@code color} is null/blank or {@code owner} is null
     */
    public Totem(String color, Player owner) {

        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("Color cannot be null or blank.");
        }
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null.");
        }

        this.color = color;
        this.owner = owner;
        this.currentOfferTile = null;
        this.currentSlot = null;
    }

    /** @return the totem colour */
    public String getColor() {
        return color;
    }

    /** @return the player who owns this totem */
    public Player getOwner() {
        return owner;
    }

    /** @return the offer tile this totem sits on, or {@code null} */
    public OfferTile getCurrentOfferTile() {
        return currentOfferTile;
    }

    /** @return the turn-order slot this totem sits on, or {@code null} */
    public Slot getCurrentSlot() {
        return currentSlot;
    }

    /** @return {@code true} if the totem is currently on the offer track */
    public boolean isOnOfferTrack() {
        return currentOfferTile != null;
    }

    /** @return {@code true} if the totem is currently on the turn-order track */
    public boolean isOnTurnOrderTrack() {
        return currentSlot != null;
    }

    /**
     * Moves this totem onto a turn-order slot, detaching it from its current
     * location first.
     *
     * @param slot the destination slot
     */
    public void moveToTurnOrder(Slot slot) {
        detachFromCurrent();
        this.currentSlot = slot;
        slot.placeTotem(this);
    }

    /**
     * Moves this totem onto an offer tile, detaching it from its current
     * location first.
     *
     * @param offerTile the destination tile
     * @throws IllegalArgumentException if {@code offerTile} is null
     * @throws IllegalStateException    if the tile is already occupied
     */
    public void moveToOffer(OfferTile offerTile) {
        if (offerTile == null) {
            throw new IllegalArgumentException("OfferTile cannot be null.");
        }
        if (!offerTile.isAvailable()) {
            throw new IllegalStateException("OfferTile is already occupied. Choose another tile.");
        }

        detachFromCurrent();
        this.currentOfferTile = offerTile;
        offerTile.placeTotem(this);
    }

    // Detaches the totem from wherever it currently sits, clearing both sides.
    private void detachFromCurrent() {
        if (currentOfferTile != null) {
            currentOfferTile.clear();
            currentOfferTile = null;
        }
        if (currentSlot != null) {
            currentSlot.removeTotem();
            currentSlot = null;
        }
    }
}
