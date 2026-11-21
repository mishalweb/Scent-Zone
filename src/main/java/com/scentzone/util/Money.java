package com.scentzone.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private Money() {}
    public static String format(BigDecimal amount) {
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);
        return "৳ " + fmt.format(amount);
    }
}
