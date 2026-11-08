package team.projectpulse.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudentSecurityServiceTest {

    @Mock
    StudentRepository studentRepository;
    @Mock
    UserUtils userUtils;

    @InjectMocks
    StudentSecurityService studentSecurityService;

    private Student student;
    private Instructor courseAdmin;
    private Instructor assignedInstructor;

    @BeforeEach
    void setUp() {
        Course course = new Course();
        this.courseAdmin = new Instructor("admin", "Course", "Admin", "admin@example.edu", "", true, "admin instructor");
        this.courseAdmin.setId(10);
        course.setCourseAdmin(this.courseAdmin);

        Section section = new Section();
        section.setSectionId(2);
        course.addSection(section);
        this.assignedInstructor = new Instructor("instructor", "Assigned", "Instructor", "instructor@example.edu", "", true, "instructor");
        this.assignedInstructor.setId(11);
        section.addInstructor(this.assignedInstructor);

        this.student = new Student("student", "Test", "Student", "student@example.edu", "", true, "student");
        this.student.setId(20);
        section.addStudent(this.student);

        given(this.studentRepository.findById(20)).willReturn(Optional.of(this.student));
    }

    @Test
    void courseAdminCanViewAndDeactivateStudentInOwnedCourse() {
        given(this.userUtils.getUserId()).willReturn(this.courseAdmin.getId());

        assertThat(this.studentSecurityService.canViewStudent(20)).isTrue();
        assertThat(this.studentSecurityService.canDeactivateStudent(20)).isTrue();
    }

    @Test
    void assignedInstructorCanViewAndDeactivateStudent() {
        given(this.userUtils.getUserId()).willReturn(this.assignedInstructor.getId());

        assertThat(this.studentSecurityService.canViewStudent(20)).isTrue();
        assertThat(this.studentSecurityService.canDeactivateStudent(20)).isTrue();
    }

    @Test
    void studentCanViewSelfButCannotDeactivateSelf() {
        given(this.userUtils.getUserId()).willReturn(this.student.getId());

        assertThat(this.studentSecurityService.canViewStudent(20)).isTrue();
        assertThat(this.studentSecurityService.canDeactivateStudent(20)).isFalse();
    }
}
