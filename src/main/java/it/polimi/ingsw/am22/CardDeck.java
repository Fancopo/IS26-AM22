package it.polimi.ingsw.am22;

public class CardDeck {
    public static List<Card> createAllCards() {
        List<Card> deck = new ArrayList<>();
        deck.add(new Character("Hunter", 1, 2)); // Era 1, min 2 giocatori
        deck.add(new Character("Shaman", 1, 3));   // Era 1, min 3 giocatori
        deck.add(new Character("Artist", 2, 4)); // Era 2, min 4 giocatori
        return deck;
}
