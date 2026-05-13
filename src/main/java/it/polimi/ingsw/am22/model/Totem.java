package it.polimi.ingsw.am22.model;

public class Totem {
    private final String color;
    private final Player owner;
    private OfferTile currentOfferTile;
    private Slot currentSlot;

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

    public String getColor() {
        return color;
    }

    public Player getOwner() {
        return owner;
    }

    public OfferTile getCurrentOfferTile() {
        return currentOfferTile;
    }

    public Slot getCurrentSlot() {
        return currentSlot;
    }

    public boolean isOnOfferTrack() {
        return currentOfferTile != null;
    }

    public boolean isOnTurnOrderTrack() {
        return currentSlot != null;
    }

    public void moveToTurnOrder(Slot slot) {
        detachFromCurrent();
        this.currentSlot = slot;
        slot.placeTotem(this);
    }

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