package it.polimi.ingsw.software;

public class Player {
    private final char id;
    private int prestigePoints;
    private int food;
    private final Tribe tribe;
    private char currentTitle;

    public Player(char id) {
        this.id = id;
        this.prestigePoints = 0;
        this.food = 0;
        this.tribe = new Tribe();
        this.currentTitle = '\0';
    }

    public Player(char id, int prestigePoints, int food, char currentTitle) {
        if (food < 0) {
            throw new IllegalArgumentException("Food cannot be negative.");
        }

        this.id = id;
        this.prestigePoints = prestigePoints;
        this.food = food;
        this.tribe = new Tribe();
        this.currentTitle = currentTitle;
    }

    public char getId() {
        return id;
    }

    public int getPrestigePoints() {
        return prestigePoints;
    }

    public int getFood() {
        return food;
    }

    public Tribe getTribe() {
        return tribe;
    }

    public char getCurrentTitle() {
        return currentTitle;
    }

    public void setCurrentTitle(char currentTitle) {
        this.currentTitle = currentTitle;
    }

    public void addFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
        this.food += amount;
    }

    public void removeFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
        if (amount > this.food) {
            throw new IllegalArgumentException("Not enough food available.");
        }
        this.food -= amount;
    }

    public void addPrestigePoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
        this.prestigePoints += amount;
    }

    public void removePrestigePoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
        this.prestigePoints -= amount;
    }

   /* private boolean isPositionAllowedByPlayerCount(char position, int numberOfPlayers) {
        switch (numberOfPlayers) {
            case 2:
                return position == 'B' || position == 'C' || position == 'E' || position == 'F';
            case 3:
                return position == 'B' || position == 'C' || position == 'D' || position == 'E' || position == 'F';
            case 4:
                return position == 'B' || position == 'C' || position == 'D' || position == 'E' || position == 'F' || position == 'G';
            case 5:
                return position == 'A' || position == 'B' || position == 'C' || position == 'D' || position == 'E' || position == 'F' || position == 'G';
            default:
                return false;
        }
    }


    public boolean isTotemPositionValid(char position, int numberOfPlayers, List<Player> players) {
        if (!isPositionAllowedByPlayerCount(position, numberOfPlayers)) {
            return false;
        }

        for (Player player : players) {
            if (player.getId() != this.id && player.getCurrentTitle() == position) {             //if this player is not me and He has already occupied the seat//
                return false;
            }
        }

        return true;
    }

    public void moveTotem(char position, int numberOfPlayers, List<Player> players) {
        if (!isTotemPositionValid(position, numberOfPlayers, players)) {
            throw new IllegalArgumentException("Invalid or already occupied totem position.");
        }
        this.currentTitle = position;
    }
*/

    @Override
    public String toString() {
        return "Player{" +
                "id='" + id + '\'' +
                ", prestigePoints=" + prestigePoints +
                ", food=" + food +
                ", currentTitle='" + currentTitle + '\'' +
                '}';
    }






































}
