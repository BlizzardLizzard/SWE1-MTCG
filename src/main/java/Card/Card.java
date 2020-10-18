package Card;

public abstract class Card {
    private String name;
    private int damage;
    private String elementType;

    public String getName() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
    }

    public int getDamage() {
        return damage;
    }

    private void setDamage(int damage) {
        this.damage = damage;
    }

    public String getElementType() {
        return elementType;
    }

    private void setElementType(String elementType) {
        this.elementType = elementType;
    }

}
