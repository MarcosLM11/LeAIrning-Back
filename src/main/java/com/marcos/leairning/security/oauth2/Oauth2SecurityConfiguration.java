package com.marcos.leairning.security.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class Oauth2SecurityConfiguration extends AbstractSecurityConfiguration {


}
