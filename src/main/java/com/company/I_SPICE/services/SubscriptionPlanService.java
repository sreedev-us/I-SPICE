package com.company.I_SPICE.services;

import com.company.I_SPICE.model.SubscriptionPlan;
import com.company.I_SPICE.model.Product;
import com.company.I_SPICE.model.User;
import com.company.I_SPICE.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionPlanService {
    private static final BigDecimal DEFAULT_FREE_SHIPPING_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal STARTER_FREE_SHIPPING_THRESHOLD = new BigDecimal("299");

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlanService(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional
    public void ensureDefaults() {
        if (subscriptionPlanRepository.count() > 0) {
            return;
        }

        subscriptionPlanRepository.save(defaultPlan(
                "STARTER",
                "Starter",
                "Perfect for curious explorers",
                new BigDecimal("299"),
                new BigDecimal("2868"),
                "Up to Rs 500 of products per month",
                "fas fa-seedling",
                "Choose Starter",
                null,
                false,
                1,
                """
                        Free delivery on orders over Rs 299
                        5% off all products
                        Access to member-only deals
                        Priority customer support
                        """));

        subscriptionPlanRepository.save(defaultPlan(
                "PRO",
                "Spice Pro",
                "Best value for spice lovers",
                new BigDecimal("599"),
                new BigDecimal("5750"),
                "Up to Rs 1,200 of products per month",
                "fas fa-fire",
                "Get Spice Pro",
                "Most Popular",
                true,
                2,
                """
                        Always free delivery
                        10% off all products
                        2x loyalty points on every order
                        Early access to new arrivals
                        Member-only weekly deals
                        Priority support with 24h response
                        """));

        subscriptionPlanRepository.save(defaultPlan(
                "ELITE",
                "Spice Elite",
                "The ultimate spice experience",
                new BigDecimal("999"),
                new BigDecimal("9590"),
                "Up to Rs 2,500 of products per month",
                "fas fa-crown",
                "Go Elite",
                null,
                false,
                3,
                """
                        Always free express delivery
                        20% off all products
                        Monthly curated spice surprise box
                        3x loyalty points on every order
                        Exclusive chef recipes and guides
                        VIP priority support with 4h response
                        Invite-only product launches
                        """));
    }

    @Transactional
    public List<SubscriptionPlan> getAllPlans() {
        ensureDefaults();
        return subscriptionPlanRepository.findAllByOrderByDisplayOrderAscIdAsc();
    }

    @Transactional
    public List<SubscriptionPlan> getActivePlans() {
        ensureDefaults();
        return subscriptionPlanRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionPlan> getPlan(Long id) {
        return subscriptionPlanRepository.findById(id);
    }

    @Transactional
    public Optional<SubscriptionPlan> getPlanByCode(String code) {
        ensureDefaults();
        return subscriptionPlanRepository.findByCodeIgnoreCase(code);
    }

    @Transactional
    public SubscriptionPlan save(SubscriptionPlan plan) {
        return subscriptionPlanRepository.save(plan);
    }

    @Transactional
    public void delete(Long id) {
        subscriptionPlanRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public SubscriptionBenefits getBenefitsForUser(User user) {
        if (user == null || !user.hasActiveSubscription()) {
            return SubscriptionBenefits.none();
        }
        return getBenefitsForPlanCode(user.getSubscriptionPlan());
    }

    @Transactional(readOnly = true)
    public SubscriptionBenefits getBenefitsForPlanCode(String code) {
        if (code == null || code.isBlank()) {
            return SubscriptionBenefits.none();
        }

        return switch (code.trim().toUpperCase()) {
            case "STARTER" -> new SubscriptionBenefits(5, 1, false, STARTER_FREE_SHIPPING_THRESHOLD, "HIGH");
            case "PRO" -> new SubscriptionBenefits(10, 2, true, BigDecimal.ZERO, "HIGH");
            case "ELITE" -> new SubscriptionBenefits(20, 3, true, BigDecimal.ZERO, "URGENT");
            default -> SubscriptionBenefits.none();
        };
    }

    @Transactional(readOnly = true)
    public BigDecimal getBaseProductPrice(Product product) {
        if (product == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal basePrice = product.getDiscountedPrice() != null ? product.getDiscountedPrice() : product.getPrice();
        if (basePrice == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return basePrice.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getEffectiveProductPrice(User user, Product product) {
        if (product == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal basePrice = getBaseProductPrice(product);
        SubscriptionBenefits benefits = getBenefitsForUser(user);
        if (benefits.productDiscountPercent() <= 0) {
            return basePrice.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal multiplier = BigDecimal.valueOf(100 - benefits.productDiscountPercent())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public boolean hasSubscriptionSavings(User user, Product product) {
        return getEffectiveProductPrice(user, product).compareTo(getBaseProductPrice(product)) < 0;
    }

    @Transactional(readOnly = true)
    public boolean userHasRequiredPlan(User user, String requiredPlanCode) {
        if (requiredPlanCode == null || requiredPlanCode.isBlank()) {
            return true;
        }
        if (user == null || !user.hasActiveSubscription()) {
            return false;
        }
        return planRank(user.getSubscriptionPlan()) >= planRank(requiredPlanCode);
    }

    private int planRank(String code) {
        if (code == null) {
            return 0;
        }
        return switch (code.trim().toUpperCase()) {
            case "STARTER" -> 1;
            case "PRO" -> 2;
            case "ELITE" -> 3;
            default -> 0;
        };
    }

    private SubscriptionPlan defaultPlan(String code,
            String name,
            String tagline,
            BigDecimal monthlyPrice,
            BigDecimal annualPrice,
            String valueLimitText,
            String iconClass,
            String buttonText,
            String badgeText,
            boolean popular,
            int displayOrder,
            String featuresText) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode(code);
        plan.setName(name);
        plan.setTagline(tagline);
        plan.setMonthlyPrice(monthlyPrice);
        plan.setAnnualPrice(annualPrice);
        plan.setValueLimitText(valueLimitText);
        plan.setIconClass(iconClass);
        plan.setButtonText(buttonText);
        plan.setBadgeText(badgeText);
        plan.setPopular(popular);
        plan.setActive(true);
        plan.setDisplayOrder(displayOrder);
        plan.setFeaturesText(featuresText);
        return plan;
    }

    public record SubscriptionBenefits(
            int productDiscountPercent,
            int loyaltyMultiplier,
            boolean alwaysFreeShipping,
            BigDecimal freeShippingThreshold,
            String supportPriority) {

        public static SubscriptionBenefits none() {
            return new SubscriptionBenefits(0, 1, false, DEFAULT_FREE_SHIPPING_THRESHOLD, "MEDIUM");
        }
    }
}
