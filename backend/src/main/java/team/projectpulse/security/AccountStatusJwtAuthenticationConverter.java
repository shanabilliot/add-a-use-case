package team.projectpulse.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

import java.math.BigDecimal;

@Component
public class AccountStatusJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public AccountStatusJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;

        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");

        this.jwtAuthenticationConverter = new JwtAuthenticationConverter();
        this.jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer userId = getUserId(jwt);
        PeerEvaluationUser user = this.userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("The account is unavailable."));
        if (!user.isEnabled()) {
            throw new BadCredentialsException("The account is disabled.");
        }
        return this.jwtAuthenticationConverter.convert(jwt);
    }

    private Integer getUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim instanceof Number number) {
            try {
                return new BigDecimal(number.toString()).intValueExact();
            } catch (NumberFormatException | ArithmeticException exception) {
                throw new BadCredentialsException("The account claim is invalid.", exception);
            }
        } else if (claim instanceof String value) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException exception) {
                throw new BadCredentialsException("The account claim is invalid.", exception);
            }
        }
        throw new BadCredentialsException("The account claim is invalid.");
    }
}
