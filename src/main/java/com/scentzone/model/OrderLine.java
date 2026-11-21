package com.scentzone.model;

import java.math.BigDecimal;

public record OrderLine(String perfumeName, int quantity, BigDecimal unitPrice) {
    public BigDecimal subtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
