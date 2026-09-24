package com.marcos.leairning.users;

import lombok.Builder;
import java.time.Instant;
import java.util.UUID;

@Builder
public record UserResponseDTO(
        UUID id,
        String email,
        String username,
        Instant createdTimestamp
) {}