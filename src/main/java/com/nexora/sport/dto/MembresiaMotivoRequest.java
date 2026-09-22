package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;

/** Usado para suspender y para cancelar: ambas acciones exigen que quien la ejecuta la justifique. */
public record MembresiaMotivoRequest(@NotBlank String motivo) {}
