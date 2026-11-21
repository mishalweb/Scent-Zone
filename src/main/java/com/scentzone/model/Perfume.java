package com.scentzone.model;

import java.math.BigDecimal;

public record Perfume(int id, int brandId, String brandName, String name, String category,
                      int sizeMl, BigDecimal price, int stock, String description, boolean active) {
    @Override public String toString() { return name + " (" + sizeMl + "ml)"; }
}
