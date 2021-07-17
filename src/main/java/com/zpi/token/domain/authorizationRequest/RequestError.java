package com.zpi.token.domain.authorizationRequest;

import lombok.*;

@EqualsAndHashCode
@Getter
@Setter
@ToString
@Builder
public class RequestError {
    private final RequestErrorType error;
    private final String error_description;
}