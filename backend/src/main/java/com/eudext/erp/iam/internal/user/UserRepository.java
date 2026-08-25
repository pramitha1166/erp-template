package com.eudext.erp.iam.internal.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * No explicit tenant filtering in the query methods here — RLS (V4
 * migration) enforces it at the database level once {@code TenantContext}
 * is set for the request, same convention as {@code DummyDocumentRepository}.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
}
