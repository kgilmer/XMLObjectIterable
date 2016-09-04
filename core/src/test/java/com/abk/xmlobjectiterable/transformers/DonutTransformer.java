package com.abk.xmlobjectiterable.transformers;

import com.abk.xmlobjectiterable.XMLElement;
import com.abk.xmlobjectiterable.XMLObjectIterable;
import com.abk.xmlobjectiterable.XMLTransformer;
import com.abk.xmlobjectiterable.model.Donut;
import com.google.common.base.Optional;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by kgilmer on 9/3/16.
 */
public class DonutTransformer implements XMLTransformer<Donut> {
    private Integer id;
    private String type;
    private String name;
    private Float ppu;
    private Set<Donut.Batter> batters = new HashSet<>();
    private Set<Donut.Filling> fillings = new HashSet<>();
    private Set<Donut.Topping> toppings = new HashSet<>();
    private String currentFillingName;
    private Float currentFillingCost;

    @Override
    public Optional<Donut> transform() {
        if (canTransform()) {
            return Optional.of(new Donut(id, name, ppu, type, batters, toppings, fillings));
        }
        return Optional.absent();
    }

    @Override
    public void visit(XMLElement node) {
        if (node.getName().equals("item")) {
            this.id = Integer.parseInt(node.getAttribs().get("id"));
            this.type = node.getAttribs().get("type");
        }

        if (node.getName().equals("name")) {
            this.name = node.getValue();
        }

        if (node.getName().equals("ppu")) {
            this.ppu = Float.parseFloat(node.getValue());
        }

        if (node.getName().equals("batter")) {
            int batterId = Integer.parseInt(node.getAttribs().get("id"));
            String batterName = node.getValue();
            batters.add(new Donut.Batter(batterId, batterName));
        }

        if (node.getName().equals("topping")) {
            int toppingId = Integer.parseInt(node.getAttribs().get("id"));
            String toppingName = node.getValue();
            toppings.add(new Donut.Topping(toppingId, toppingName));
        }

        if (node.getName().equals("filling")) {
            int currentFillingId = Integer.parseInt(node.getAttribs().get("id"));
            if (currentFillingName == null || currentFillingCost == null) {
                throw new IllegalStateException("bad filling");
            }
            fillings.add(new Donut.Filling(currentFillingId, currentFillingName, currentFillingCost));
            currentFillingName = null;
            currentFillingCost = null;
        }

        if (node.getName().equals("name")) {
            currentFillingName = node.getValue();
        }

        if (node.getName().equals("addcost")) {
            currentFillingCost = Float.parseFloat(node.getValue());
        }

    }

    @Override
    public void reset() {
        currentFillingName = null;
        currentFillingCost = null;
        id = null;
        name = null;
        type = null;
        ppu = null;
        batters.clear();
        toppings.clear();
        fillings.clear();
    }

    @Override
    public boolean canTransform() {
        return name != null
                && id != null
                && type != null
                && ppu != null;
    }
}
