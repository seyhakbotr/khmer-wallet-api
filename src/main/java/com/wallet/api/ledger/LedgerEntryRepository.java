package com.wallet.api.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

	@Query("select coalesce(sum(e.amountMinor), 0) from LedgerEntry e where e.account.id = :accountId")
	long sumAmountMinorByAccountId(@Param("accountId") Long accountId);

}
