package com.vermeg.collateralmanagement.repository;

import com.vermeg.collateralmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    List<User> findByIsActiveTrue();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
    /**
     * Count users created after specific date
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :date")
    Long countUsersCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * Find users created between dates for analytics
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Get user registration statistics by day for charts
     */
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count " +
            "FROM users WHERE created_at >= :startDate " +
            "GROUP BY DATE(created_at) ORDER BY date DESC",
            nativeQuery = true)
    List<Object[]> getUserRegistrationStats(@Param("startDate") LocalDateTime startDate);
}