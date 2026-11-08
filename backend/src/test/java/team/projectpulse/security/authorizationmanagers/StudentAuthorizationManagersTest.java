package team.projectpulse.security.authorizationmanagers;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import team.projectpulse.student.StudentSecurityService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StudentAuthorizationManagersTest {

    @Mock
    StudentSecurityService studentSecurityService;

    @InjectMocks
    StudentLifecycleAuthorizationManager studentLifecycleAuthorizationManager;

    @InjectMocks
    StudentViewAuthorizationManager studentViewAuthorizationManager;

    private RequestAuthorizationContext contextWith(Map<String, String> variables) {
        return new RequestAuthorizationContext(mock(HttpServletRequest.class), variables);
    }

    @Test
    void lifecycleGuardDelegatesTheMatchedStudentId() {
        given(this.studentSecurityService.canDeactivateStudent(7)).willReturn(true);

        AuthorizationResult result = this.studentLifecycleAuthorizationManager.authorize(
                () -> null, contextWith(Map.of("studentId", "7")));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void lifecycleGuardDeniesMalformedStudentIdWithoutCallingSecurityService() {
        AuthorizationResult result = this.studentLifecycleAuthorizationManager.authorize(
                () -> null, contextWith(Map.of("studentId", "not-a-number")));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verifyNoInteractions(this.studentSecurityService);
    }

    @Test
    void viewGuardDelegatesTheMatchedStudentId() {
        given(this.studentSecurityService.canViewStudent(7)).willReturn(true);

        AuthorizationResult result = this.studentViewAuthorizationManager.authorize(
                () -> null, contextWith(Map.of("studentId", "7")));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void viewGuardDeniesMissingStudentIdWithoutCallingSecurityService() {
        AuthorizationResult result = this.studentViewAuthorizationManager.authorize(
                () -> null, contextWith(Map.of()));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verifyNoInteractions(this.studentSecurityService);
    }
}
