package com.zpi.api.authCode;

import com.zpi.api.authCode.authenticationRequest.AuditMetadataDTO;
import com.zpi.api.authCode.authenticationRequest.AuthenticationResponseDTO;
import com.zpi.api.authCode.consentRequest.ConsentRequestDTO;
import com.zpi.api.authCode.consentRequest.ConsentResponseDTO;
import com.zpi.api.authCode.ticketRequest.TicketRequestDTO;
import com.zpi.api.authCode.ticketRequest.TicketResponseDTO;
import com.zpi.api.common.dto.UserDTO;
import com.zpi.api.common.exception.ErrorResponseException;
import com.zpi.domain.authCode.AuthCodeService;
import com.zpi.domain.authCode.authenticationRequest.AuthenticationRequest;
import com.zpi.domain.authCode.consentRequest.ErrorConsentResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthCodeController {
    private final AuthCodeService authCodeService;

    public final static String authorizeUri = "/authorize";
    public final static String authenticateUri = "/authenticate";
    public final static String consentUri = "/consent";

    private static String AUTH_PAGE_URI(String organization) {
        return "/organization/" + organization + "/signin";
    }

    @GetMapping(authorizeUri)
    public ResponseEntity<?> authorize(@RequestParam String client_id,
                                       @RequestParam(required = false) String redirect_uri,
                                       @RequestParam String response_type,
                                       @RequestParam(required = false) String scope,
                                       @RequestParam(required = false) String state) {

        var requestDTO = new TicketRequestDTO(client_id,
                redirect_uri,
                response_type,
                scope,
                state);
        var request = requestDTO.toDomain();
        AtomicReference<String> organization = new AtomicReference<>("");
        return getRedirectInfo(request, organization.get());
    }

    private ResponseEntity<?> getRedirectInfo(AuthenticationRequest request, String organization) {
        String location;

        try {
            var response = new AuthenticationResponseDTO(authCodeService.validateAndFillRequest(request));
            location = response.toUrl(AUTH_PAGE_URI(organization));
        } catch (ErrorResponseException e) {
            var response = e.getErrorResponse();
            location = response.toUrl(AUTH_PAGE_URI(organization));
        }

        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location).body(null);
    }


    @PostMapping(authenticateUri)
    public ResponseEntity<?> authenticate(@RequestBody UserDTO userDTO,
                                          @RequestParam String client_id,
                                          @RequestParam(required = false) String redirect_uri,
                                          @RequestParam String response_type,
                                          @RequestParam(required = false) String scope,
                                          @RequestParam String state,
                                          @RequestHeader("host") String host,
                                          @RequestHeader("user-agent") String userAgent) {

        var requestDTO = new TicketRequestDTO(client_id,
                redirect_uri,
                response_type,
                scope,
                state);
        var request = requestDTO.toDomain();
        var metadata = (new AuditMetadataDTO(host, userAgent)).toDomain();

        try {
            var user = userDTO.toHashedDomain();
            var body = new TicketResponseDTO(authCodeService.authenticationTicket(user, request, metadata));
            return ResponseEntity.ok(body);
        } catch (ErrorResponseException e) {
            return new ResponseEntity<>(e.getErrorResponse(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(consentUri)
    public ResponseEntity<?> consent(@RequestBody ConsentRequestDTO requestDTO) {
        var request = requestDTO.toDomain();

        try {
            var response = new ConsentResponseDTO(authCodeService.consentRequest(request));
            var location = response.toUrl();
            return ResponseEntity.status(HttpStatus.OK).body(location);
        } catch (ErrorConsentResponseException e) {
            var location = e.toUrl(AUTH_PAGE_URI(""), request.getState());
            return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location).body(null);
        }
    }
}
