package com.company.I_SPICE.repository;

import com.company.I_SPICE.model.SupportTicket;
import com.company.I_SPICE.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserOrderByCreatedAtDesc(User user);

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);
}
