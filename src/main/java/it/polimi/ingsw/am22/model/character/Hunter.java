package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.PickSimulation;
import it.polimi.ingsw.am22.model.Player;


public class Hunter extends TribeCharacter implements CharacterEffect {
    private final boolean hasFoodIcon;

    public Hunter(String id, Era era, int minPlayers, boolean hasFoodIcon) {
        super(id, era, minPlayers, CharacterType.HUNTER, null);
        this.hasFoodIcon = hasFoodIcon;
        setEffect(this);
    }

    public boolean hasFoodIcon() {
        return hasFoodIcon;
    }

    @Override
    protected void onAddedToTribe(Player player) {
        if (hasFoodIcon) {
            int foodToAdd = player.getTribe().countCharacters(getCharacterType());
            player.addFood(foodToAdd);
        }
    }

    /**
     * Hunters come in two variants. The food-icon variant is shown as
     * {@code HUNTER*} so the user can tell at a glance which copies trigger
     * the on-add food bonus. The starred form is for display only — it is
     * never parsed back.
     */
    @Override
    public String cardDetailType() {
        return hasFoodIcon ? "HUNTER*" : "HUNTER";
    }

    /** Validation: mirror {@link #onAddedToTribe} on the simulation — bump the
     *  hunter count first, then (for Hunter*) feed the player by that count. */
    @Override
    public void applyPickEffect(PickSimulation sim) {
        sim.incrementHunterCount();
        if (hasFoodIcon) {
            sim.addFood(sim.getHunterCount());
        }
    }

    @Override
    public String describe() {
        String base = "Hunter: scores PP and food during the Hunting event "
                + "(more hunters in your tribe -> more reward).";
        return hasFoodIcon
                ? base + " Food-icon variant: when added to the tribe, immediately grants "
                        + "food equal to the total number of Hunters you own."
                : base;
    }
}
