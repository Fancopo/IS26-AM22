package il.polimi.ingse.character;

import javafx.application.Application;

public class TribeCharacter extends Card {

    // Attributi definiti nel diagramma UML
    private CharacterType characterType;
    private int discountFood;
    private int PP;
    private boolean hasFoodIcon;
    private int numStars;
    private char iconperInventor;

    // Costruttore
    public TribeCharacter(Era era, int minPlayers, CharacterType characterType,
                          int discountFood, int PP, boolean hasFoodIcon,
                          int numStars, char iconperInventor) {
        super(era, minPlayers);
        this.characterType = characterType;
        this.discountFood = discountFood;
        this.PP = PP;
        this.hasFoodIcon = hasFoodIcon;
        this.numStars = numStars;
        this.iconperInventor = iconperInventor;
    }

    /**
     * Risolve l'effetto immediato o applica le meccaniche specifiche
     * del personaggio quando viene aggiunto alla tribù.
     */
    public void handleEffect(Player player, Tribe tribe) {

        switch (this.characterType) {
            case HUNTER:
                // Se il Cacciatore ha l'icona cibo, prendi 1 cibo per OGNI cacciatore nella tribù.
                // Altrimenti, non si ottiene nulla immediatamente[cite: 145].
                // Durante l'Evento Caccia forniranno ulteriore cibo e PP[cite: 147].
                if (this.hasFoodIcon) {
                    int hunterCount = tribe.countCharacters(CharacterType.HUNTER);
                    player.addFood(hunterCount);
                    System.out.println("Cacciatore con icona giocato! Aggiunto " + hunterCount + " cibo.");
                }
                break;

            case BUILDER:
                // I Costruttori forniscono uno sconto permanente sul costo in cibo degli Edifici.
                // A fine partita forniscono i Punti Prestigio (PP) indicati[cite: 124].
                // L'effetto qui è passivo, quindi potremmo semplicemente notificare il giocatore.
                System.out.println("Costruttore aggiunto. Sconto per gli edifici aumentato di " + this.discountFood);
                break;

            case COLLECTOR:
                // I Raccoglitori non danno cibo immediato, ma forniscono uno sconto di 3 cibo
                // durante l'Evento Sostentamento[cite: 126, 127].
                System.out.println("Raccoglitore aggiunto. Fornirà uno sconto di 3 cibo durante il Sostentamento.");
                break;

            case INVENTOR:
                // Gli Inventori non hanno effetti immediati.
                // A fine partita danno PP = (Numero di Inventori) x (Numero di icone invenzione diverse)[cite: 120].
                System.out.println("Inventore aggiunto con icona: " + this.iconperInventor);
                break;

            case SHAMAN:
                // Gli sciamani mostrano da 1 a 3 icone (rappresentate da numStars nell'UML)[cite: 129].
                // Servono durante l'Evento Rituale Sciamanico per guadagnare o perdere PP[cite: 130, 131].
                System.out.println("Sciamano aggiunto con " + this.numStars + " icone rituale.");
                break;

            case ARTIST:
                // Gli Artisti sono valutati durante l'Evento Pitture Rupestri (PP in base alla quantità)[cite: 140, 142].
                // A fine partita danno 10 PP ogni 2 Artisti[cite: 143].
                System.out.println("Artista aggiunto alla tribù.");
                break;
        }
    }

    // --- Getter e Setter ---

    public CharacterType getCharacterType() { return characterType; }
    public int getDiscountFood() { return discountFood; }
    public int getPP() { return PP; }
    public boolean hasFoodIcon() { return hasFoodIcon; }
    public int getNumStars() { return numStars; }
    public char getIconperInventor() { return iconperInventor; }
}