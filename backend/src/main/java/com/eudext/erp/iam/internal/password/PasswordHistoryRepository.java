package com.eudext.erp.iam.internal.password;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {

    @Query("select h from PasswordHistory h where h.userId = :userId order by h.createdAt desc")
    List<PasswordHistory> findRecentByUserId(@Param("userId") UUID userId);
}
