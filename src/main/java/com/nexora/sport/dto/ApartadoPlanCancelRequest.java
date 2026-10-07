package com.nexora.sport.dto;

import jakarta.validation.constraints.Size;

public record ApartadoPlanCancelRequest(@Size(max = 500) String motivo) {}
