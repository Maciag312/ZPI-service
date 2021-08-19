package com.zpi.domain.authCode.authorizationRequest;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthorizationResponse {
    private final String ticket;
    private final String state;
}