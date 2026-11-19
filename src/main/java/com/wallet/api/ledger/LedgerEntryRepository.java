package com.wallet.api.ledger;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

	@Query("select coalesce(sum(e.amountMinor), 0) from LedgerEntry e where e.account.id = :accountId")
	long sumAmountMinorByAccountId(@Param("accountId") Long accountId);

	@Query("""
			select coalesce(sum(e.amountMinor), 0) from LedgerEntry e
			where e.account.id = :accountId and e.createdAt < :instant
			""")
	long sumAmountMinorBefore(@Param("accountId") Long accountId, @Param("instant") Instant instant);

	@Query("""
			select coalesce(sum(e.amountMinor), 0) from LedgerEntry e
			where e.account.id = :accountId and e.entryType = :entryType
			and e.createdAt >= :start and e.createdAt < :end
			""")
	long sumAmountMinorByEntryTypeBetween(@Param("accountId") Long accountId,
			@Param("entryType") EntryType entryType, @Param("start") Instant start, @Param("end") Instant end);

	@Query("""
			select count(e) from LedgerEntry e
			where e.account.id = :accountId and e.createdAt >= :start and e.createdAt < :end
			""")
	long countBetween(@Param("accountId") Long accountId, @Param("start") Instant start, @Param("end") Instant end);

}
