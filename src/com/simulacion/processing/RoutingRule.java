package com.simulacion.processing;

public class RoutingRule {
    private final String destinationLocation;
    private final double probability;
    private final int quantity;
    private final String moveLogic;
    private final String resourceName;

    public RoutingRule(String destinationLocation, double probability, int quantity, 
                      String moveLogic, String resourceName) {
        this.destinationLocation = destinationLocation;
        this.probability = probability;
        this.quantity = quantity;
        this.moveLogic = moveLogic;
        this.resourceName = resourceName;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public double getProbability() {
        return probability;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getMoveLogic() {
        return moveLogic;
    }

    public String getResourceName() {
        return resourceName;
    }
}
