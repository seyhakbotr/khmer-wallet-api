package com.wallet.api.rate;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsdKhrRateRepository extends JpaRepository<UsdKhrRate, Long> {

	Optional<UsdKhrRate> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate date);

	Optional<UsdKhrRate> findByEffectiveFrom(LocalDate effectiveFrom);

}
