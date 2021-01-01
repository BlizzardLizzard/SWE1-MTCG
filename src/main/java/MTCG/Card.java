package MTCG;

public class Card {
    public String Id;
    public float Damage;
    public String Name;

    public Card() {
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public float getDamage() {
        return Damage;
    }

    public void setDamage(float damage) {
        Damage = damage;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }
}
