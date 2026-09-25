package com.ga.gestionstock.dto;

import jakarta.validation.constraints.NotNull;
public record EtatArticleRequest(@NotNull Boolean actif) {}

