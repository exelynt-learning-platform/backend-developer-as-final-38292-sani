package com.assessment.booking.seeder;

import com.assessment.booking.entity.Reservation;
import com.assessment.booking.entity.ReservationStatus;
import com.assessment.booking.entity.Resource;
import com.assessment.booking.entity.Role;
import com.assessment.booking.entity.User;
import com.assessment.booking.repository.ReservationRepository;
import com.assessment.booking.repository.ResourceRepository;
import com.assessment.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
        seedReservations();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            log.info("Seeding initial users (ADMIN & USER)...");

            User admin = User.builder()
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .fullName("System Administrator")
                    .role(Role.ROLE_ADMIN)
                    .build();

            User user1 = User.builder()
                    .email("user@example.com")
                    .password(passwordEncoder.encode("User@123"))
                    .fullName("Alice Johnson")
                    .role(Role.ROLE_USER)
                    .build();

            User user2 = User.builder()
                    .email("user2@example.com")
                    .password(passwordEncoder.encode("User2@123"))
                    .fullName("Bob Smith")
                    .role(Role.ROLE_USER)
                    .build();

            userRepository.saveAll(List.of(admin, user1, user2));
            log.info("Users seeded successfully: admin@example.com, user@example.com, user2@example.com");
        }
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            log.info("Seeding initial bookable resources...");

            Resource confRoom = Resource.builder()
                    .name("Executive Conference Room A")
                    .description("Equipped with 4K display, polycom audio, and seating for 20 people.")
                    .type("ROOM")
                    .capacity(20)
                    .location("Building 1, Floor 4, Suite 402")
                    .pricePerHour(new BigDecimal("75.00"))
                    .active(true)
                    .build();

            Resource studio = Resource.builder()
                    .name("4K Multimedia Production Studio")
                    .description("Acoustically treated studio with 4K cameras, ring lights, and podcast microphones.")
                    .type("STUDIO")
                    .capacity(6)
                    .location("Building 2, Basement Studio B")
                    .pricePerHour(new BigDecimal("120.00"))
                    .active(true)
                    .build();

            Resource workstation = Resource.builder()
                    .name("MacBook Pro M3 Max Mobile Workstation")
                    .description("16-inch MacBook Pro 64GB RAM 2TB SSD for high-intensity mobile computing.")
                    .type("EQUIPMENT")
                    .capacity(1)
                    .location("IT Asset Locker 12")
                    .pricePerHour(new BigDecimal("25.50"))
                    .active(true)
                    .build();

            Resource gpuServer = Resource.builder()
                    .name("NVIDIA H100 AI Compute Node")
                    .description("Dedicated 8x H100 80GB GPU server instance for deep learning model training.")
                    .type("COMPUTE")
                    .capacity(1)
                    .location("Data Center Rack 08")
                    .pricePerHour(new BigDecimal("250.00"))
                    .active(true)
                    .build();

            Resource tesla = Resource.builder()
                    .name("Company Fleet Vehicle - Tesla Model Y")
                    .description("Electric SUV for client site visits and company travel.")
                    .type("VEHICLE")
                    .capacity(5)
                    .location("Parking Bay P-104")
                    .pricePerHour(new BigDecimal("45.00"))
                    .active(true)
                    .build();

            Resource inactiveRoom = Resource.builder()
                    .name("Renovating Boardroom C")
                    .description("Under maintenance and interior refurbishment.")
                    .type("ROOM")
                    .capacity(15)
                    .location("Building 1, Floor 2")
                    .pricePerHour(new BigDecimal("50.00"))
                    .active(false)
                    .build();

            resourceRepository.saveAll(List.of(confRoom, studio, workstation, gpuServer, tesla, inactiveRoom));
            log.info("Resources seeded successfully.");
        }
    }

    private void seedReservations() {
        if (reservationRepository.count() == 0) {
            log.info("Seeding initial sample reservations...");

            User user1 = userRepository.findByEmail("user@example.com").orElse(null);
            User user2 = userRepository.findByEmail("user2@example.com").orElse(null);
            List<Resource> resources = resourceRepository.findAll();

            if (user1 != null && user2 != null && resources.size() >= 3) {
                Resource confRoom = resources.get(0);
                Resource studio = resources.get(1);
                Resource workstation = resources.get(2);

                LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

                Reservation res1 = Reservation.builder()
                        .user(user1)
                        .resource(confRoom)
                        .startTime(now.plusDays(1).withHour(9))
                        .endTime(now.plusDays(1).withHour(11))
                        .status(ReservationStatus.CONFIRMED)
                        .totalPrice(new BigDecimal("150.00"))
                        .notes("Quarterly Strategy Alignment with Stakeholders")
                        .build();

                Reservation res2 = Reservation.builder()
                        .user(user1)
                        .resource(workstation)
                        .startTime(now.plusDays(2).withHour(14))
                        .endTime(now.plusDays(2).withHour(18))
                        .status(ReservationStatus.PENDING)
                        .totalPrice(new BigDecimal("102.00"))
                        .notes("App development sprint testing")
                        .build();

                Reservation res3 = Reservation.builder()
                        .user(user2)
                        .resource(studio)
                        .startTime(now.plusDays(3).withHour(10))
                        .endTime(now.plusDays(3).withHour(13))
                        .status(ReservationStatus.CONFIRMED)
                        .totalPrice(new BigDecimal("360.00"))
                        .notes("Product launch keynote recording")
                        .build();

                Reservation res4 = Reservation.builder()
                        .user(user1)
                        .resource(confRoom)
                        .startTime(now.plusDays(4).withHour(15))
                        .endTime(now.plusDays(4).withHour(17))
                        .status(ReservationStatus.CANCELLED)
                        .totalPrice(new BigDecimal("150.00"))
                        .notes("Cancelled due to client reschedule")
                        .build();

                reservationRepository.saveAll(List.of(res1, res2, res3, res4));
                log.info("Reservations seeded successfully.");
            }
        }
    }
}
