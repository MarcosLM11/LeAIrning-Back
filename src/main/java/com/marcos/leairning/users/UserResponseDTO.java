package com.marcos.leairning.users;

import lombok.Builder;
import java.util.UUID;

@Builder
public record UserResponseDTO(
        UUID id,
        String email
) {

}
