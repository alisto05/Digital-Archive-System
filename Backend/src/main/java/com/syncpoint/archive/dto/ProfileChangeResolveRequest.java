package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotNull;

/** The reviewing staff member comes from the session. */
public record ProfileChangeResolveRequest(@NotNull Boolean approve) {}
