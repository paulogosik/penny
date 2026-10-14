package com.penny.repository;

import java.math.BigDecimal;
import java.util.UUID;

public record AmountByKey(UUID key, BigDecimal total) {
}
