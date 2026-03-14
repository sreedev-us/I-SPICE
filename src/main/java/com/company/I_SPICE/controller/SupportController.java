package com.company.I_SPICE.controller;

import com.company.I_SPICE.model.SupportTicket;
import com.company.I_SPICE.model.SupportTicketReply;
import com.company.I_SPICE.model.User;
import com.company.I_SPICE.repository.SupportTicketReplyRepository;
import com.company.I_SPICE.repository.SupportTicketRepository;
import com.company.I_SPICE.services.SubscriptionPlanService;
import com.company.I_SPICE.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/support")
public class SupportController {

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private SupportTicketReplyRepository replyRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private SubscriptionPlanService subscriptionPlanService;

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return null;
        return userService.getUserFromPrincipal(auth).orElse(null);
    }

    @GetMapping
    public String showSupportPage(Authentication auth, Model model) {
        User user = getAuthenticatedUser(auth);

        // Populate the cartCount as required by the global layout
        model.addAttribute("cartCount", 0); // Assuming 0 for now as it needs a CartService

        if (user != null) {
            model.addAttribute("user", user);
            List<SupportTicket> tickets = ticketRepository.findByUserOrderByCreatedAtDesc(user);
            model.addAttribute("tickets", tickets);
        } else {
            // User is not authenticated, redirect them to login before using support
            return "redirect:/login";
        }

        return "support";
    }

    @PostMapping("/ticket")
    public String submitNewTicket(@RequestParam String subject,
            @RequestParam String category,
            @RequestParam String description,
            Authentication auth,
            RedirectAttributes attr) {
        User user = getAuthenticatedUser(auth);
        if (user == null) {
            return "redirect:/login";
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setSubject(subject);
        ticket.setCategory(category);
        ticket.setDescription(description);
        ticket.setStatus("OPEN");
        ticket.setPriority(subscriptionPlanService.getBenefitsForUser(user).supportPriority());
        ticket.setTicketNumber("TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        ticketRepository.save(ticket);

        attr.addFlashAttribute("success", "Support ticket created successfully. Our team will get back to you soon!");
        return "redirect:/support";
    }

    @GetMapping("/ticket/{id}")
    public String viewTicketDetails(@PathVariable Long id, Authentication auth, Model model) {
        User user = getAuthenticatedUser(auth);
        if (user == null) {
            return "redirect:/login";
        }

        Optional<SupportTicket> optTicket = ticketRepository.findById(id);
        if (optTicket.isEmpty() || !optTicket.get().getUser().getId().equals(user.getId())) {
            // Either ticket doesn't exist, or it belongs to another user
            return "redirect:/support";
        }

        SupportTicket ticket = optTicket.get();
        List<SupportTicketReply> replies = replyRepository.findByTicketOrderByCreatedAtAsc(ticket);

        model.addAttribute("user", user);
        model.addAttribute("ticket", ticket);
        model.addAttribute("replies", replies);
        model.addAttribute("cartCount", 0);

        return "ticket-view";
    }

    @PostMapping("/ticket/{id}/reply")
    public String submitReply(@PathVariable Long id,
            @RequestParam String message,
            Authentication auth,
            RedirectAttributes attr) {
        User user = getAuthenticatedUser(auth);
        if (user == null) {
            return "redirect:/login";
        }

        Optional<SupportTicket> optTicket = ticketRepository.findById(id);
        if (optTicket.isEmpty() || !optTicket.get().getUser().getId().equals(user.getId())) {
            return "redirect:/support";
        }

        SupportTicket ticket = optTicket.get();

        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setUser(user);
        reply.setMessage(message);
        reply.setIsStaffReply(false); // Because it is the customer replying

        replyRepository.save(reply);

        // Automatically mark the ticket as OPEN again if the user replies to a CLOSED
        // ticket
        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("OPEN");
            ticketRepository.save(ticket);
        }

        attr.addFlashAttribute("success", "Reply added successfully!");
        return "redirect:/support/ticket/" + id;
    }
}
