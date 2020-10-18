public class Monster extends Card {
    private String species;

    public Monster() {
        System.out.println("Monster has been created");

    }

    public String getSpecies() {
        return species;
    }

    private void setSpecies(String species) {
        this.species = species;
    }
}
