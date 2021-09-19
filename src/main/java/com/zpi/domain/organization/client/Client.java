package com.zpi.domain.organization.client;

import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class Client {
    private final Set<String> availableRedirectUri = new HashSet<>();
    private final List<String> hardcodedDefaultScope = List.of("profile".split(" "));
    private final String id;
    @Setter
    String organizationName;
}