package MTCG.JsonObjects;

public class CardStack {
    public String id;
    public float damage;
    public String name;

    public CardStack(String id, float damage, String name) {
        this.id = id;
        this.damage = damage;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
