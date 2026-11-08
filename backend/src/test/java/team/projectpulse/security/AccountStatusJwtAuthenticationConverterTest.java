package team.projectpulse.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccountStatusJwtAuthenticationConverterTest {

    @Mock
    UserRepository userRepository;

    @Test
    void convertsTokenOnlyWhenCurrentAccountIsEnabled() {
        Student student = new Student("student", "Test", "Student", "student@example.edu", "", true, "student");
        student.setId(20);
        given(this.userRepository.findById(20)).willReturn(Optional.of(student));
        AccountStatusJwtAuthenticationConverter converter = new AccountStatusJwtAuthenticationConverter(this.userRepository);

        AbstractAuthenticationToken authentication = converter.convert(jwt(20));

        assertThat(authentication.getAuthorities()).extracting("authority").contains("ROLE_student");
    }

    @Test
    void rejectsTokenWhenAccountIsDisabled() {
        Student student = new Student("student", "Test", "Student", "student@example.edu", "", false, "student");
        student.setId(20);
        given(this.userRepository.findById(20)).willReturn(Optional.of(student));
        AccountStatusJwtAuthenticationConverter converter = new AccountStatusJwtAuthenticationConverter(this.userRepository);

        assertThatThrownBy(() -> converter.convert(jwt(20)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("The account is disabled.");
    }

    @Test
    void rejectsTokenWhenAccountNoLongerExists() {
        given(this.userRepository.findById(20)).willReturn(Optional.empty());
        AccountStatusJwtAuthenticationConverter converter = new AccountStatusJwtAuthenticationConverter(this.userRepository);

        assertThatThrownBy(() -> converter.convert(jwt(20)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("The account is unavailable.");
    }

    private static Jwt jwt(Integer userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("student")
                .claim("userId", userId)
                .claim("authorities", "ROLE_student")
                .build();
    }
}
