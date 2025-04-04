package kr.cocoh.api.repository;

import kr.cocoh.api.model.auth.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    
    List<UserActivity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<UserActivity> findByActivityTypeAndUserIdOrderByCreatedAtDesc(String activityType, Long userId);
}