package com.eudext.erp.documents.internal.dummy;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DummyDocumentRepository extends JpaRepository<DummyDocument, UUID> {}
