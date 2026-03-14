package com.company.I_SPICE.controller;

import com.company.I_SPICE.model.*;
import com.company.I_SPICE.repository.*;
import com.company.I_SPICE.services.UserService;
import com.company.I_SPICE.services.OrderService;
import com.company.I_SPICE.services.SubscriptionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private SupportTicketRepository ticketRepository;
    @Autowired
    private SupportTicketReplyRepository replyRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private SubscriptionPlanService subscriptionPlanService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─── DASHBOARD ────────────────────────────────────────────────────────────

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalOrders", orderRepository.count());
        model.addAttribute("openTickets", ticketRepository.countByStatus("OPEN"));
        model.addAttribute("recentOrders", orderRepository.findAllByOrderByOrderDateDesc()
                .stream().limit(5).toList());
        model.addAttribute("recentTickets", ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream().limit(5).toList());
        return "admin/admin-dashboard";
    }

    // ─── PRODUCTS ─────────────────────────────────────────────────────────────

    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "admin/admin-products";
    }

    @GetMapping("/products/new")
    public String newProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("editing", false);
        return "admin/admin-product-form";
    }

    @PostMapping("/products/new")
    public String createProduct(@ModelAttribute Product product, RedirectAttributes attr) {
        try {
            productRepository.save(product);
            attr.addFlashAttribute("success", "Product created successfully!");
        } catch (Exception e) {
            attr.addFlashAttribute("error", "Failed to create product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model, RedirectAttributes attr) {
        Optional<Product> p = productRepository.findById(id);
        if (p.isEmpty()) {
            attr.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }
        model.addAttribute("product", p.get());
        model.addAttribute("editing", true);
        return "admin/admin-product-form";
    }

    @PostMapping("/products/{id}/edit")
    public String updateProduct(@PathVariable Long id, @ModelAttribute Product form, RedirectAttributes attr) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            attr.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }
        Product p = opt.get();
        p.setName(form.getName());
        p.setDescription(form.getDescription());
        p.setPrice(form.getPrice());
        p.setCategory(form.getCategory());
        p.setImageUrl(form.getImageUrl());
        p.setAdditionalImages(form.getAdditionalImages());
        p.setStock(form.getStock());
        p.setFeatured(form.isFeatured());
        p.setDiscount(form.getDiscount());
        p.setWeightGrams(form.getWeightGrams());
        p.setOrigin(form.getOrigin());
        p.setOrganic(form.getOrganic());
        p.setSpicyLevel(form.getSpicyLevel());
        p.setSubscriptionTierRequired(form.getSubscriptionTierRequired());
        p.setEarlyAccessOnly(form.getEarlyAccessOnly());
        productRepository.save(p);
        attr.addFlashAttribute("success", "Product updated successfully!");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes attr) {
        try {
            productRepository.deleteById(id);
            attr.addFlashAttribute("success", "Product deleted.");
        } catch (Exception e) {
            attr.addFlashAttribute("error", "Cannot delete product: it may have existing order references.");
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/subscriptions")
    public String listSubscriptions(Model model) {
        model.addAttribute("plans", subscriptionPlanService.getAllPlans());
        return "admin/admin-subscriptions";
    }

    @GetMapping("/subscriptions/new")
    public String newSubscriptionForm(Model model) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setActive(true);
        plan.setDisplayOrder(subscriptionPlanService.getAllPlans().size() + 1);
        plan.setIconClass("fas fa-star");
        model.addAttribute("plan", plan);
        model.addAttribute("editing", false);
        model.addAttribute("formAction", "/admin/subscriptions/new");
        return "admin/admin-subscription-form";
    }

    @PostMapping("/subscriptions/new")
    public String createSubscription(@ModelAttribute("plan") SubscriptionPlan plan, RedirectAttributes attr) {
        try {
            subscriptionPlanService.save(plan);
            attr.addFlashAttribute("success", "Subscription plan created successfully.");
        } catch (Exception e) {
            attr.addFlashAttribute("error", "Failed to create subscription plan: " + e.getMessage());
        }
        return "redirect:/admin/subscriptions";
    }

    @GetMapping("/subscriptions/{id}/edit")
    public String editSubscriptionForm(@PathVariable Long id, Model model, RedirectAttributes attr) {
        Optional<SubscriptionPlan> opt = subscriptionPlanService.getPlan(id);
        if (opt.isEmpty()) {
            attr.addFlashAttribute("error", "Subscription plan not found");
            return "redirect:/admin/subscriptions";
        }
        model.addAttribute("plan", opt.get());
        model.addAttribute("editing", true);
        model.addAttribute("formAction", "/admin/subscriptions/" + id + "/edit");
        return "admin/admin-subscription-form";
    }

    @PostMapping("/subscriptions/{id}/edit")
    public String updateSubscription(@PathVariable Long id,
            @ModelAttribute("plan") SubscriptionPlan form,
            RedirectAttributes attr) {
        Optional<SubscriptionPlan> opt = subscriptionPlanService.getPlan(id);
        if (opt.isEmpty()) {
            attr.addFlashAttribute("error", "Subscription plan not found");
            return "redirect:/admin/subscriptions";
        }

        try {
            SubscriptionPlan plan = opt.get();
            copySubscriptionFields(plan, form);
            subscriptionPlanService.save(plan);
            attr.addFlashAttribute("success", "Subscription plan updated successfully.");
        } catch (Exception e) {
            attr.addFlashAttribute("error", "Failed to update subscription plan: " + e.getMessage());
        }
        return "redirect:/admin/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/delete")
    public String deleteSubscription(@PathVariable Long id, RedirectAttributes attr) {
        try {
            subscriptionPlanService.delete(id);
            attr.addFlashAttribute("success", "Subscription plan deleted.");
        } catch (Exception e) {
            attr.addFlashAttribute("error", "Could not delete subscription plan: " + e.getMessage());
        }
        return "redirect:/admin/subscriptions";
    }

    // ─── USERS ────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/admin-users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model, RedirectAttributes attr) {
        Optional<User> u = userRepository.findById(id);
        if (u.isEmpty()) {
            attr.addFlashAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        model.addAttribute("editUser", u.get());
        return "admin/admin-user-form";
    }

    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable Long id,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phoneNumber,
            @RequestParam String role,
            @RequestParam(required = false) String newPassword,
            RedirectAttributes attr) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            attr.addFlashAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        User u = opt.get();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setFullName(firstName + " " + lastName);
        u.setEmail(email);
        u.setPhoneNumber(phoneNumber);
        u.setRole(role);
        if (newPassword != null && !newPassword.isBlank()) {
            u.setPassword(passwordEncoder.encode(newPassword));
        }
        userRepository.save(u);
        attr.addFlashAttribute("success", "User updated successfully!");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes attr) {
        try {
            userRepository.deleteById(id);
            attr.addFlashAttribute("success", "User deleted.");
        } catch (Exception e) {
            attr.addFlashAttribute("error", "Cannot delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ─── ORDERS ───────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderRepository.findAllByOrderByOrderDateDesc());
        return "admin/admin-orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model, RedirectAttributes attr) {
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) {
            attr.addFlashAttribute("error", "Order not found");
            return "redirect:/admin/orders";
        }
        model.addAttribute("order", opt.get());
        model.addAttribute("statuses", Order.OrderStatus.values());
        return "admin/admin-order-detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, RedirectAttributes attr) {
        try {
            Order o = orderService.cancelOrder(id);
            attr.addFlashAttribute("success", "Order " + o.getOrderNumber() + " cancelled successfully.");
        } catch (Exception e) {
            attr.addFlashAttribute("error", "Failed to cancel order: " + e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes attr) {
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isPresent()) {
            Order o = opt.get();
            o.setStatus(Order.OrderStatus.valueOf(status));
            orderRepository.save(o);
            attr.addFlashAttribute("success", "Order status updated to " + status);
        }
        return "redirect:/admin/orders/" + id;
    }

    // ─── SUPPORT TICKETS ──────────────────────────────────────────────────────

    @GetMapping("/support")
    public String listTickets(@RequestParam(required = false) String status, Model model) {
        List<SupportTicket> tickets = (status != null && !status.isBlank())
                ? ticketRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase())
                : ticketRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("tickets", tickets);
        model.addAttribute("filterStatus", status);
        model.addAttribute("openCount", ticketRepository.countByStatus("OPEN"));
        model.addAttribute("closedCount", ticketRepository.countByStatus("CLOSED"));
        return "admin/admin-support";
    }

    @GetMapping("/support/{id}")
    public String ticketDetail(@PathVariable Long id, Model model, RedirectAttributes attr) {
        Optional<SupportTicket> opt = ticketRepository.findById(id);
        if (opt.isEmpty()) {
            attr.addFlashAttribute("error", "Ticket not found");
            return "redirect:/admin/support";
        }
        SupportTicket ticket = opt.get();
        model.addAttribute("ticket", ticket);
        model.addAttribute("replies", replyRepository.findByTicketOrderByCreatedAtAsc(ticket));
        return "admin/admin-ticket-detail";
    }

    @PostMapping("/support/{id}/reply")
    public String adminReplyToTicket(@PathVariable Long id,
            @RequestParam String message,
            org.springframework.security.core.Authentication auth,
            RedirectAttributes attr) {
        Optional<SupportTicket> opt = ticketRepository.findById(id);
        if (opt.isEmpty()) {
            attr.addFlashAttribute("error", "Ticket not found");
            return "redirect:/admin/support";
        }

        User adminUser = userService.getUserFromPrincipal(auth).orElse(null);
        SupportTicket ticket = opt.get();

        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setUser(adminUser);
        reply.setMessage(message);
        reply.setIsStaffReply(true); // Admin/staff reply

        replyRepository.save(reply);

        // Mark ticket as ANSWERED
        ticket.setStatus("ANSWERED");
        ticketRepository.save(ticket);

        attr.addFlashAttribute("success", "Reply sent successfully!");
        return "redirect:/admin/support/" + id;
    }

    @PostMapping("/support/{id}/close")
    public String closeTicket(@PathVariable Long id, RedirectAttributes attr) {
        Optional<SupportTicket> opt = ticketRepository.findById(id);
        if (opt.isPresent()) {
            SupportTicket t = opt.get();
            t.setStatus("CLOSED");
            ticketRepository.save(t);
            attr.addFlashAttribute("success", "Ticket closed.");
        }
        return "redirect:/admin/support/" + id;
    }

    private void copySubscriptionFields(SubscriptionPlan target, SubscriptionPlan source) {
        target.setCode(source.getCode());
        target.setName(source.getName());
        target.setTagline(source.getTagline());
        target.setMonthlyPrice(source.getMonthlyPrice());
        target.setAnnualPrice(source.getAnnualPrice());
        target.setValueLimitText(source.getValueLimitText());
        target.setIconClass(source.getIconClass());
        target.setButtonText(source.getButtonText());
        target.setBadgeText(source.getBadgeText());
        target.setPopular(source.isPopular());
        target.setActive(source.isActive());
        target.setDisplayOrder(source.getDisplayOrder());
        target.setFeaturesText(source.getFeaturesText());
    }
}
