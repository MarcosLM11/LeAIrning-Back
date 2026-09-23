package com.marcos.leairning.security.oauth2;

import com.marcos.leairning.users.User;
import com.marcos.leairning.users.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthUserProvisioningService {
    private final UsersService usersService;
    private final OAuthIdentityRepository identityRepository;

    public User findOrCreateUser(OAuthUserInfo info) {
        var existing = identityRepository.findByProviderAndProviderId(info.provider(), info.providerId());
        if (existing.isPresent()) {
            return existing.get().getUser();
        }
        try {
            return provisionNewIdentity(info);
        } catch (DataIntegrityViolationException e) {
            return identityRepository.findByProviderAndProviderId(info.provider(), info.providerId())
                    .map(OAuthIdentity::getUser)
                    .orElseThrow(() -> e);
        }
    }

    private User provisionNewIdentity(OAuthUserInfo info) {
        var user = resolveUser(info);
        identityRepository.save(new OAuthIdentity(null, user, info.provider(), info.providerId()));
        return user;
    }

    private User resolveUser(OAuthUserInfo info) {
        if (info.emailVerified()) {
            var existingUser = usersService.findEntityByEmail(info.email());
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
        }
        return usersService.createOAuthUser(info.email(), info.name(), info.pictureUrl(), info.provider().name().toLowerCase());
    }
}