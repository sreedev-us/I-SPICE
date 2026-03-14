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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
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

        try {
            List<String> attachmentUrls = storeAttachments(attachments);
            if (!attachmentUrls.isEmpty()) {
                ticket.setAttachmentUrls(String.join(",", attachmentUrls));
            }
        } catch (IllegalArgumentException | IOException ex) {
            attr.addFlashAttribute("error", ex.getMessage());
            return "redirect:/support";
        }

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
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
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
        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            attr.addFlashAttribute("error", "This ticket is closed. Please open a new ticket for further help.");
            return "redirect:/support/ticket/" + id;
        }

        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setUser(user);
        reply.setMessage(message);
        reply.setIsStaffReply(false); // Because it is the customer replying

        try {
            List<String> attachmentUrls = storeAttachments(attachments);
            if (!attachmentUrls.isEmpty()) {
                reply.setAttachmentUrls(String.join(",", attachmentUrls));
            }
        } catch (IllegalArgumentException | IOException ex) {
            attr.addFlashAttribute("error", ex.getMessage());
            return "redirect:/support/ticket/" + id;
        }

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

    private List<String> storeAttachments(MultipartFile[] attachments) throws IOException {
        List<String> urls = new ArrayList<>();
        if (attachments == null || attachments.length == 0) {
            return urls;
        }

        Path uploadDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "static",
                "uploads", "support");
        Files.createDirectories(uploadDir);

        for (MultipartFile file : attachments) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                throw new IllegalArgumentException("Only image and video files are allowed for support attachments.");
            }

            String originalName = file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename();
            String safeName = UUID.randomUUID().toString() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = uploadDir.resolve(safeName);
            Files.copy(file.getInputStream(), target);
            urls.add("/uploads/support/" + safeName);
        }

        return urls;
    }
}
