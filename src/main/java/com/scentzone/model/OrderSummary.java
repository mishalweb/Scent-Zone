package com.scentzone.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummary(int id, String customerName, BigDecimal total, String status,
                           LocalDateTime date, int itemCount) {}
