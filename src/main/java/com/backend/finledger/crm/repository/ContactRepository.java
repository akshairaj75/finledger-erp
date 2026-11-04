package com.backend.finledger.crm.repository;

import com.backend.finledger.crm.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    Optional<Contact> findByCompanyIdAndId(Long companyId, Long id);

    List<Contact> findByCompanyIdAndCustomerId(Long companyId, Long customerId);

    List<Contact> findByCompanyIdAndLeadId(Long companyId, Long leadId);

    Optional<Contact> findFirstByCompanyIdAndCustomerIdAndIsPrimaryTrue(Long companyId, Long customerId);

    Page<Contact> findByCompanyId(Long companyId, Pageable pageable);
}

