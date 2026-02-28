package org.voice.membership.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.ChildRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChildService
 * Tests child CRUD operations with ownership validation
 */
@ExtendWith(MockitoExtension.class)
class ChildServiceTest {

    @Mock
    private ChildRepository childRepository;

    @InjectMocks
    private ChildService childService;

    @Captor
    private ArgumentCaptor<Child> childCaptor;

    private User testUser;
    private User otherUser;
    private Child testChild;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("parent@example.com");

        otherUser = new User();
        otherUser.setId(2);
        otherUser.setEmail("other@example.com");

        testChild = Child.builder()
                .id(100)
                .name("Test Child")
                .age(5)
                .hearingLossType("Sensorineural")
                .equipmentType("Hearing Aid")
                .siblingsNames("Sibling 1")
                .chapterLocation("Toronto")
                .user(testUser)
                .build();
    }

    // ==================== createChild Tests ====================

    @Test
    void createChild_WithValidData_ShouldCreateAndSave() {
        // Arrange
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Child created = childService.createChild(
                testUser,
                "New Child",
                6,
                "2018-05-15",
                "Conductive",
                "Cochlear Implant",
                "Brother, Sister",
                "Vancouver"
        );

        // Assert
        verify(childRepository).save(childCaptor.capture());
        Child savedChild = childCaptor.getValue();

        assertThat(savedChild.getName()).isEqualTo("New Child");
        assertThat(savedChild.getAge()).isEqualTo(6);
        assertThat(savedChild.getHearingLossType()).isEqualTo("Conductive");
        assertThat(savedChild.getEquipmentType()).isEqualTo("Cochlear Implant");
        assertThat(savedChild.getSiblingsNames()).isEqualTo("Brother, Sister");
        assertThat(savedChild.getChapterLocation()).isEqualTo("Vancouver");
        assertThat(savedChild.getUser()).isEqualTo(testUser);
        assertThat(savedChild.getDateOfBirth()).isNotNull();
    }

    @Test
    void createChild_WithValidDateOfBirth_ShouldParseDateCorrectly() throws Exception {
        // Arrange
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date expectedDate = sdf.parse("2018-05-15");

        // Act
        Child created = childService.createChild(
                testUser, "Child", 6, "2018-05-15",
                "Type", "Equipment", "Siblings", "Location"
        );

        // Assert
        verify(childRepository).save(childCaptor.capture());
        Child savedChild = childCaptor.getValue();

        assertThat(savedChild.getDateOfBirth()).isNotNull();
        assertThat(sdf.format(savedChild.getDateOfBirth())).isEqualTo("2018-05-15");
    }

    @Test
    void createChild_WithInvalidDateOfBirth_ShouldCreateWithNullDate() {
        // Arrange
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Child created = childService.createChild(
                testUser, "Child", 6, "invalid-date",
                "Type", "Equipment", "Siblings", "Location"
        );

        // Assert
        verify(childRepository).save(childCaptor.capture());
        Child savedChild = childCaptor.getValue();

        assertThat(savedChild.getDateOfBirth()).isNull();
    }

    @Test
    void createChild_WithEmptyDateOfBirth_ShouldCreateWithNullDate() {
        // Arrange
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Child created = childService.createChild(
                testUser, "Child", 6, "",
                "Type", "Equipment", "Siblings", "Location"
        );

        // Assert
        verify(childRepository).save(childCaptor.capture());
        Child savedChild = childCaptor.getValue();

        assertThat(savedChild.getDateOfBirth()).isNull();
    }

    @Test
    void createChild_WithNullDateOfBirth_ShouldCreateWithNullDate() {
        // Arrange
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Child created = childService.createChild(
                testUser, "Child", 6, null,
                "Type", "Equipment", "Siblings", "Location"
        );

        // Assert
        verify(childRepository).save(childCaptor.capture());
        Child savedChild = childCaptor.getValue();

        assertThat(savedChild.getDateOfBirth()).isNull();
    }

    // ==================== updateChild Tests ====================

    @Test
    void updateChild_WithValidOwner_ShouldUpdateAndSave() {
        // Arrange
        when(childRepository.findById(100)).thenReturn(Optional.of(testChild));
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<Child> updated = childService.updateChild(
                100, testUser,
                "Updated Name", 7, "2017-03-20",
                "Mixed", "FM System", "Updated Siblings", "Montreal"
        );

        // Assert
        assertThat(updated).isPresent();
        verify(childRepository).save(childCaptor.capture());
        Child savedChild = childCaptor.getValue();

        assertThat(savedChild.getName()).isEqualTo("Updated Name");
        assertThat(savedChild.getAge()).isEqualTo(7);
        assertThat(savedChild.getHearingLossType()).isEqualTo("Mixed");
        assertThat(savedChild.getEquipmentType()).isEqualTo("FM System");
        assertThat(savedChild.getSiblingsNames()).isEqualTo("Updated Siblings");
        assertThat(savedChild.getChapterLocation()).isEqualTo("Montreal");
    }

    @Test
    void updateChild_WithNonExistentChild_ShouldReturnEmpty() {
        // Arrange
        when(childRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        Optional<Child> updated = childService.updateChild(
                999, testUser,
                "Name", 5, "2019-01-01",
                "Type", "Equipment", "Siblings", "Location"
        );

        // Assert
        assertThat(updated).isEmpty();
        verify(childRepository, never()).save(any());
    }

    @Test
    void updateChild_WithDifferentOwner_ShouldReturnEmpty() {
        // Arrange
        when(childRepository.findById(100)).thenReturn(Optional.of(testChild));

        // Act
        Optional<Child> updated = childService.updateChild(
                100, otherUser,
                "Name", 5, "2019-01-01",
                "Type", "Equipment", "Siblings", "Location"
        );

        // Assert
        assertThat(updated).isEmpty();
        verify(childRepository, never()).save(any());
    }

    @Test
    void updateChild_WithInvalidDateFormat_ShouldKeepExistingDate() throws Exception {
        // Arrange
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date originalDate = sdf.parse("2018-05-15");
        testChild.setDateOfBirth(originalDate);

        when(childRepository.findById(100)).thenReturn(Optional.of(testChild));
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<Child> updated = childService.updateChild(
                100, testUser,
                "Name", 5, "invalid-date",
                "Type", "Equipment", "Siblings", "Location"
        );

        // Assert
        assertThat(updated).isPresent();
        assertThat(updated.get().getDateOfBirth()).isEqualTo(originalDate);
    }

    // ==================== deleteChild Tests ====================

    @Test
    void deleteChild_WithValidOwner_ShouldDeleteAndReturnTrue() {
        // Arrange
        when(childRepository.findById(100)).thenReturn(Optional.of(testChild));

        // Act
        boolean deleted = childService.deleteChild(100, testUser);

        // Assert
        assertThat(deleted).isTrue();
        verify(childRepository).delete(testChild);
    }

    @Test
    void deleteChild_WithNonExistentChild_ShouldReturnFalse() {
        // Arrange
        when(childRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        boolean deleted = childService.deleteChild(999, testUser);

        // Assert
        assertThat(deleted).isFalse();
        verify(childRepository, never()).delete(any());
    }

    @Test
    void deleteChild_WithDifferentOwner_ShouldReturnFalse() {
        // Arrange
        when(childRepository.findById(100)).thenReturn(Optional.of(testChild));

        // Act
        boolean deleted = childService.deleteChild(100, otherUser);

        // Assert
        assertThat(deleted).isFalse();
        verify(childRepository, never()).delete(any());
    }

    // ==================== getChildByIdForUser Tests ====================

    @Test
    void getChildByIdForUser_WithValidOwner_ShouldReturnChild() {
        // Arrange
        when(childRepository.findById(100)).thenReturn(Optional.of(testChild));

        // Act
        Optional<Child> found = childService.getChildByIdForUser(100, testUser);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(testChild);
    }

    @Test
    void getChildByIdForUser_WithNonExistentChild_ShouldReturnEmpty() {
        // Arrange
        when(childRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        Optional<Child> found = childService.getChildByIdForUser(999, testUser);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void getChildByIdForUser_WithDifferentOwner_ShouldReturnEmpty() {
        // Arrange
        when(childRepository.findById(100)).thenReturn(Optional.of(testChild));

        // Act
        Optional<Child> found = childService.getChildByIdForUser(100, otherUser);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void getChildByIdForUser_WithCorrectOwner_ShouldVerifyOwnership() {
        // Arrange
        when(childRepository.findById(100)).thenReturn(Optional.of(testChild));

        // Act
        Optional<Child> found = childService.getChildByIdForUser(100, testUser);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
    }
}
