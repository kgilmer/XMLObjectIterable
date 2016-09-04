package com.abk.xmlobjectiterable.model;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by kgilmer on 9/3/16.
 */
public class Donut {

    public static abstract class Ingredient {
        private final int id;
        private final String name;

        public Ingredient(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class Batter extends Ingredient {
        public Batter(int id, String name) {
            super(id, name);
        }
    }

    public static class Topping extends Ingredient {
        public Topping(int id, String name) {
            super(id, name);
        }
    }

    public static class Filling extends Ingredient {
        float cost;
        public Filling(int id, String name, float cost) {
            super(id, name);
            this.cost = cost;
        }

        public float getCost() {
            return cost;
        }
    }

    private final int id;
    private final String name;
    private final float ppu;
    private final String type;
    private final Set<Batter> batters;
    private final Set<Topping> topping;
    private final Set<Filling> filling;

    public Donut(int id, String name, float ppu, String type, Set<Batter> batters, Set<Topping> topping, Set<Filling> filling) {
        this.id = id;
        this.name = name;
        this.ppu = ppu;
        this.type = type;
        this.batters = Sets.newHashSet(batters);
        this.topping = Sets.newHashSet(topping);
        this.filling = Sets.newHashSet(filling);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public float getPpu() {
        return ppu;
    }

    public String getType() {
        return type;
    }

    public Set<Batter> getBatters() {
        return batters;
    }

    public Set<Topping> getTopping() {
        return topping;
    }

    public Set<Filling> getFilling() {
        return filling;
    }
}
