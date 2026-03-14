package com.company.I_SPICE.repository;

import com.company.I_SPICE.model.SupportTicket;
import com.company.I_SPICE.model.SupportTicketReply;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketReplyRepository extends JpaRepository<SupportTicketReply, Long> {
    @EntityGraph(attributePaths = "user")
    List<SupportTicketReply> findByTicketOrderByCreatedAtAsc(SupportTicket ticket);
}
