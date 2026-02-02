package org.voice.membership.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ChildRepository
 * Tests child data access operations
 **/
@DataJpaTest
@ActiveProfiles("test")
class ChildRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChildRepository childRepository;

    private User testUser;
    private Child child1;
    private Child child2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .firstName("Parent")
                .middleName(null)
                .lastName("User")
                .email("parent@example.com")
                .password("password123")
                .phone("1234567890")
                .role(Role.USER.name())
                .creation(new Date())
                .build();

        entityManager.persist(testUser);

        child1 = Child.builder()
                .name("Child One")
                .dateOfBirth(new Date())
                .hearingLossType("Profound")
                .equipmentType("Cochlear Implant")
                .user(testUser)
                .build();

        child2 = Child.builder()
                .name("Child Two")
                .dateOfBirth(new Date())
                .hearingLossType("Moderate")
                .equipmentType("Hearing Aid")
                .user(testUser)
                .build();
    }

    @Test
    void save_ShouldPersistChild() {
        Child saved = childRepository.save(child1);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getUser()).isEqualTo(testUser);
    }

    @Test
    void findById_WithExistingId_ShouldReturnChild() {
        entityManager.persist(child1);
        entityManager.flush();
        int childId = child1.getId();
        Child found = childRepository.findById(childId).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Child One");
        assertThat(found.getHearingLossType()).isEqualTo("Profound");
    }

    @Test
    void delete_ShouldRemoveChild() {
        entityManager.persist(child1);
        entityManager.flush();
        int childId = child1.getId();
        childRepository.delete(child1);
        entityManager.flush();

        Child found = entityManager.find(Child.class, childId);
        assertThat(found).isNull();
    }

    @Test
    void findAll_ShouldReturnAllChildren() {
        entityManager.persist(child1);
        entityManager.persist(child2);
        entityManager.flush();
        List<Child> children = childRepository.findAll();

        assertThat(children).hasSize(2);
        assertThat(children).extracting(Child::getName)
                .containsExactlyInAnyOrder("Child One", "Child Two");
    }

    @Test
    void cascadeDelete_WhenUserDeleted_ShouldDeleteChildren() {
        childRepository.save(child1);
        childRepository.save(child2);
        entityManager.flush();
        entityManager.clear();

        List<Child> childrenBefore = childRepository.findAll();
        assertThat(childrenBefore).hasSize(2);

        User managedUser = entityManager.find(User.class, testUser.getId());
        entityManager.remove(managedUser);
        entityManager.flush();

        List<Child> childrenAfter = childRepository.findAll();
        assertThat(childrenAfter).isEmpty();
    }
}

