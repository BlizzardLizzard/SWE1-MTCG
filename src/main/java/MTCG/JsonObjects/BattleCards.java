package MTCG.JsonObjects;

public class BattleCards {
    String name;
    float damage;
    String element;
    String type;

    public BattleCards(String name, float damage, String element, String type) {
        this.name = name;
        this.damage = damage;
        this.element = element;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
