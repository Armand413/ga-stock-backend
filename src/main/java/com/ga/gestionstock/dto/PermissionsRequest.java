package com.ga.gestionstock.dto;
import com.ga.gestionstock.entity.Permission;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
public record PermissionsRequest(@NotNull Set<@NotNull Permission> permissions) {}
