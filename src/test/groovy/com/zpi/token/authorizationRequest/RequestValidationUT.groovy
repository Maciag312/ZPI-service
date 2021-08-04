package com.zpi.token.authorizationRequest


import com.zpi.token.api.authorizationRequest.RequestDTO
import com.zpi.token.domain.Client
import com.zpi.token.domain.authorizationRequest.request.InvalidRequestException
import com.zpi.token.domain.authorizationRequest.request.RequestError
import com.zpi.token.domain.authorizationRequest.request.RequestErrorType
import com.zpi.token.domain.authorizationRequest.request.RequestValidation
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Subject

class RequestValidationUT extends Specification {
    @Subject
    private RequestValidation requestValidation = new RequestValidation()

    def "should not throw when all parameters correct"() {
        given:
            def request = CommonFixtures.correctRequest().toDomain()
            def client = CommonFixtures.defaultClient()
        when:
            requestValidation.validate(request, client)

        then:
            noExceptionThrown()
    }

    def "should throw unauthorized_client on non existing client"() {
        given:
            def request = CommonFixtures.correctRequest().toDomain()

        when:
            requestValidation.validate(request, null)

        then:
            def exception = thrown(InvalidRequestException)
            def expected = RequestError.builder()
                    .error(RequestErrorType.UNAUTHORIZED_CLIENT)
                    .errorDescription("Unauthorized client id")
                    .build()

            exception.error == expected
            exception.status == HttpStatus.BAD_REQUEST
    }

    def "should throw when incorrect redirect_uri"() {
        given:
            def request = Fixtures.requestWithCustomUri("UnrecognizedUri").toDomain()
            def client = CommonFixtures.defaultClient()

        when:
            requestValidation.validate(request, client)

        then:
            def exception = thrown(InvalidRequestException)
            def expected = RequestError.builder()
                    .error(RequestErrorType.UNRECOGNIZED_REDIRECT_URI)
                    .errorDescription("Unrecognized redirect uri")
                    .build()

            exception.error == expected
            exception.status == HttpStatus.BAD_REQUEST
    }

    def "should return error message when client has no registered redirect uris"() {
        given:
            def request = Fixtures.requestWithCustomUri("UnrecognizedUri").toDomain()
            def client = Fixtures.clientWithNullRedirectUri()

        when:
            requestValidation.validate(request, client)

        then:
            def exception = thrown(InvalidRequestException)
            def expected = RequestError.builder()
                    .error(RequestErrorType.UNRECOGNIZED_REDIRECT_URI)
                    .errorDescription("Unrecognized redirect uri")
                    .build()

            exception.error == expected
            exception.status == HttpStatus.BAD_REQUEST
    }

    def "should throw invalid_request on missing required parameters"() {
        given:
            def client = CommonFixtures.defaultClient()

        when:
            requestValidation.validate(request.toDomain(), client)

        then:
            def exception = thrown(InvalidRequestException)
            def expected = RequestError.builder()
                    .error(RequestErrorType.INVALID_REQUEST)
                    .errorDescription("Missing: " + errorDescription)
                    .build()

            exception.error == expected
            exception.status == HttpStatus.BAD_REQUEST

        where:
            request                 | _ || errorDescription
            Fixtures.nullClientId() | _ || "client_id"
            Fixtures.nullState()    | _ || "state"
    }

    def "should throw unsupported_response_type on wrong responseType"() {
        given:
            def client = CommonFixtures.defaultClient()

        when:
            requestValidation.validate(request.toDomain(), client)

        then:
            def exception = thrown(InvalidRequestException)
            def expected = RequestError.builder()
                    .error(RequestErrorType.UNSUPPORTED_RESPONSE_TYPE)
                    .errorDescription(errorDescription)
                    .build()

            exception.error == expected
            exception.status == HttpStatus.BAD_REQUEST

        where:
            request                        | _ || errorDescription
            Fixtures.invalidResponseType() | _ || "Unrecognized response type: invalid"
    }

    def "should throw invalid_scope on invalid scope"() {
        given:
            def client = CommonFixtures.defaultClient()

        when:
            requestValidation.validate(request.toDomain(), client)

        then:
            def exception = thrown(InvalidRequestException)
            def expected = RequestError.builder()
                    .error(RequestErrorType.INVALID_SCOPE)
                    .errorDescription("Invalid scope")
                    .build()

            exception.error == expected
            exception.status == HttpStatus.BAD_REQUEST

        where:
            request                       | _
            Fixtures.emptyScope()         | _
            Fixtures.nullScope()          | _
            Fixtures.scopeWithoutOpenId() | _
    }

    private class Fixtures {
        static RequestDTO requestWithCustomUri(String uri) {
            return RequestDTO.builder()
                    .clientId(CommonFixtures.defaultClientId)
                    .redirectUri(uri)
                    .responseType("code")
                    .scope("openid")
                    .state(CommonFixtures.defaultState)
                    .build()
        }

        static RequestDTO nullClientId() {
            return RequestDTO.builder()
                    .clientId(null)
                    .redirectUri(CommonFixtures.defaultUri)
                    .responseType("code")
                    .scope("openid")
                    .state(CommonFixtures.defaultState)
                    .build()
        }

        static RequestDTO nullState() {
            return RequestDTO.builder()
                    .clientId(CommonFixtures.defaultClientId)
                    .redirectUri(CommonFixtures.defaultUri)
                    .responseType("code")
                    .scope("openid")
                    .state(null)
                    .build()
        }

        static RequestDTO invalidResponseType() {
            return RequestDTO.builder()
                    .clientId(CommonFixtures.defaultClientId)
                    .redirectUri(CommonFixtures.defaultUri)
                    .responseType("invalid")
                    .scope("openid")
                    .state(CommonFixtures.defaultState)
                    .build()
        }

        static RequestDTO emptyScope() {
            return RequestDTO.builder()
                    .clientId(CommonFixtures.defaultClientId)
                    .redirectUri(CommonFixtures.defaultUri)
                    .responseType("code")
                    .scope("")
                    .state(CommonFixtures.defaultClientId)
                    .build()
        }

        static RequestDTO nullScope() {
            return RequestDTO.builder()
                    .clientId(CommonFixtures.defaultClientId)
                    .redirectUri(CommonFixtures.defaultUri)
                    .responseType("code")
                    .scope(null)
                    .state(CommonFixtures.defaultClientId)
                    .build()
        }

        static RequestDTO scopeWithoutOpenId() {
            return RequestDTO.builder()
                    .clientId(CommonFixtures.defaultClientId)
                    .redirectUri(CommonFixtures.defaultUri)
                    .responseType("code")
                    .scope("profile phone unknown_value other_unknown")
                    .state(CommonFixtures.defaultState)
                    .build()
        }

        static Client clientWithNullRedirectUri() {
            return Client.builder()
                    .id(CommonFixtures.defaultClientId)
                    .availableRedirectUri(null)
                    .build()
        }
    }
}
