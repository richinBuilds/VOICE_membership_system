package org.voice.membership.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.voice.membership.entities.Cart;
import org.voice.membership.entities.CartItem;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.CartItemRepository;
import org.voice.membership.repositories.CartRepository;
import org.voice.membership.repositories.ChildRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@Order(3)
@SuppressWarnings("null")
/**
 * Seeds a demo user with profile, children, and cart data so the dashboard
 * is not empty when the database is fresh.
 * This runs once and is safe to re-run (idempotent).
 */
public class DummyDataInitializer implements CommandLineRunner {

    private static final String DEMO_EMAIL = "demo.user@voice.local";
    private static final String DEMO_EMAIL_2 = "brampton.family@voice.local";
    private static final String DEMO_EMAIL_3 = "brampton.parent@voice.local";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        Membership membership = resolveDemoMembership();

        seedDemoUser(
            DEMO_EMAIL,
            "Riley",
            "Morgan",
            "123 Community Ave",
            "Vancouver",
            "BC",
            "V6B 1A1",
            "Greater Vancouver",
            membership,
            daysAgo(90),
            List.of(
                buildChild("Avery Morgan", 7, 7 * 365, "Sensorineural", "Hearing Aid", "Liam",
                    "Greater Vancouver"),
                buildChild("Liam Morgan", 5, 5 * 365, "Conductive", "Cochlear Implant", "Avery",
                    "Greater Vancouver")));

        seedDemoUser(
            DEMO_EMAIL_2,
            "Sofia",
            "Patel",
            "77 Queen Street W",
            "Brampton",
            "Ontario",
            "L6Y 1M2",
            "Peel Region",
            membership,
            daysAgo(60),
            List.of(
                buildChild("Kiran Patel", 9, 9 * 365, "Mixed", "Hearing Aid", "",
                    "Peel Region")));

        seedDemoUser(
            DEMO_EMAIL_3,
            "Daniel",
            "Nguyen",
            "15 Main Street N",
            "Brampton",
            "Ontario",
            "L6X 1N1",
            "Peel Region",
            membership,
            daysAgo(30),
            List.of(
                buildChild("Maya Nguyen", 6, 6 * 365, "Sensorineural", "Cochlear Implant", "",
                    "Peel Region"),
                buildChild("Leo Nguyen", 3, 3 * 365, "Conductive", "Hearing Aid", "Maya",
                    "Peel Region")));
    }

        private void seedDemoUser(
            String email,
            String firstName,
            String lastName,
            String address,
            String city,
            String province,
            String postalCode,
            String chapterLocation,
            Membership membership,
            Date createdDate,
            List<Child> children) {
        if (userRepository.findByEmail(email) != null) {
            log.info("Demo user already exists. Skipping demo data bootstrap for {}", email);
            return;
        }

        User demoUser = User.builder()
            .firstName(firstName)
            .middleName(null)
            .lastName(lastName)
            .email(email)
            .password(passwordEncoder.encode("DemoUser123!"))
            .phone("555-0108")
            .address(address)
            .city(city)
            .province(province)
            .postalCode(postalCode)
            .role(Role.USER.name())
            .creation(createdDate)
            .membership(membership)
            .membershipStartDate(createdDate)
            .membershipExpiryDate(daysFromNow(275))
            .build();

        demoUser = userRepository.save(demoUser);

        List<Child> linkedChildren = new ArrayList<>();
        for (Child child : children) {
            linkedChildren.add(Child.builder()
                .name(child.getName())
                .age(child.getAge())
                .dateOfBirth(child.getDateOfBirth())
                .hearingLossType(child.getHearingLossType())
                .equipmentType(child.getEquipmentType())
                .siblingsNames(child.getSiblingsNames())
                .chapterLocation(chapterLocation)
                .user(demoUser)
                .build());
        }

        if (!linkedChildren.isEmpty()) {
            childRepository.saveAll(linkedChildren);
        }

        Cart cart = Cart.builder()
            .user(demoUser)
            .build();
        cart = cartRepository.save(cart);

        if (membership != null && membership.getPrice() != null) {
            BigDecimal unitPrice = membership.getPrice();
            CartItem cartItem = CartItem.builder()
                .cart(cart)
                .membership(membership)
                .quantity(1)
                .unitPrice(unitPrice)
                .totalPrice(unitPrice)
                .build();
            cartItemRepository.save(cartItem);
        }

        log.info("Demo user and sample data created successfully. Email: {}", email);
        }

        private Child buildChild(
            String name,
            Integer age,
            int daysSinceBirth,
            String hearingLossType,
            String equipmentType,
            String siblingsNames,
            String chapterLocation) {
        return Child.builder()
            .name(name)
            .age(age)
            .dateOfBirth(daysAgo(daysSinceBirth))
            .hearingLossType(hearingLossType)
            .equipmentType(equipmentType)
            .siblingsNames(siblingsNames)
            .chapterLocation(chapterLocation)
            .build();
        }

    private Membership resolveDemoMembership() {
        List<Membership> paidMemberships = membershipRepository.findByIsFree(false);
        if (!paidMemberships.isEmpty()) {
            return paidMemberships.get(0);
        }

        List<Membership> activeMemberships = membershipRepository.findByActiveTrue();
        if (!activeMemberships.isEmpty()) {
            return activeMemberships.get(0);
        }

        Membership freeMembership = Membership.builder()
                .name("Free")
                .description("Get started with VOICE community")
                .price(null)
                .features("Basic access" + System.lineSeparator() + "Community forum access")
                .isFree(true)
                .displayOrder(1)
                .active(true)
                .build();
        return membershipRepository.save(freeMembership);
    }

    private Date daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return cal.getTime();
    }

    private Date daysFromNow(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }
}
