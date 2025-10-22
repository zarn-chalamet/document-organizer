package com.projects.document_organizer.respository;

import com.projects.document_organizer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    User findByEmail(String email);

    Optional<User> findByGoogleAccessToken(String accessToken);
}
