package com.company.I_SPICE.repository;

import com.company.I_SPICE.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findAllByOrderByDisplayOrderAscIdAsc();

    List<SubscriptionPlan> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    Optional<SubscriptionPlan> findByCodeIgnoreCase(String code);
}
