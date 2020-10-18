public class User {
    private int coins = 20;
    private int elo = 100;
    private int numberOfGames = 0;

    public int getCoins() {
        return coins;
    }

    private void setCoins(int coins) {
        this.coins = coins;
    }

    public int getElo() {
        return elo;
    }

    private void setElo(int elo) {
        this.elo = elo;
    }
}
