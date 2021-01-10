package MTCG.JsonObjects;

public class Trade {
    public String tradeId;
    public String cardID;
    public String token;
    public String type;
    public int damage;
    public boolean traded;

    public Trade(String tradeId, String cardID, String token, String type, int damage, boolean traded) {
        this.tradeId = tradeId;
        this.cardID = cardID;
        this.token = token;
        this.type = type;
        this.damage = damage;
        this.traded = traded;
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public String getCardID() {
        return cardID;
    }

    public void setCardID(String cardID) {
        this.cardID = cardID;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public boolean isTraded() {
        return traded;
    }

    public void setTraded(boolean traded) {
        this.traded = traded;
    }
}
