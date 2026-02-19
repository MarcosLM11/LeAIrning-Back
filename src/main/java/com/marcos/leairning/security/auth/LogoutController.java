package com.marcos.leairning.security.auth;

import com.marcos.leairning.security.annotations.BusinessAuthorityOnly;
import com.marcos.leairning.util.web.CurrentUserId;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@BusinessAuthorityOnly
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogoutController {

    AuthService authService;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentUserId UUID userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
