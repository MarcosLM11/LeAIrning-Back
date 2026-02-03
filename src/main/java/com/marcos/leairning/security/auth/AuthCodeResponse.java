package com.marcos.leairning.security.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthCodeResponse(
        @JsonProperty("auth_code") String authCode
) {
}