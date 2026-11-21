package com.scentzone.model;

import java.math.BigDecimal;

public class CartLine {
    private Perfume perfume;
    private int quantity;

    public CartLine(Perfume perfume, int quantity) {
        this.perfume = perfume;
        this.quantity = quantity;
    }
    public Perfume getPerfume() { return perfume; }
    public int getQuantity() { return quantity; }
    public void increase(int amount) { quantity += amount; }
    public BigDecimal getSubtotal() { return perfume.price().multiply(BigDecimal.valueOf(quantity)); }
}
