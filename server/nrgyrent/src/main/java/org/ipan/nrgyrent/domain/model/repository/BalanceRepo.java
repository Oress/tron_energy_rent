package org.ipan.nrgyrent.domain.model.repository;

import java.util.List;
import java.util.Set;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceType;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.domain.model.projections.ReferralDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceRepo extends JpaRepository<Balance, Long> {
    Page<Balance> findAllByTypeOrderById(BalanceType type, Pageable pageable);
    Page<Balance> findAllByTypeAndLabelContainingIgnoreCaseOrderById(BalanceType group, String label, PageRequest of);
    List<Balance> findAllByIsActive(Boolean isActive);
    List<Balance> findAllByTariffId(Long tariffId);

    @Query("select b.id from Balance b where b.isActive = :isActive")
    List<Long> findAllIdsByIsActive(Boolean isActive);

    @Query("select distinct b from ReferralCommission c join c.balanceReferralProgram brp join brp.balance b where c.status = ReferralCommissionStatus.PENDING")
    Set<Balance> findAllWithPendingReferralCommissions();

    Balance findByDepositAddress(String depositAddress);

    @Query("select b.depositAddress from Balance b where b.isActive = true")
    Set<String> findAllActiveAddresses();

    @Query("""
        select new org.ipan.nrgyrent.domain.model.projections.ReferralDto(b.type, u.telegramId, u.telegramUsername, u.telegramFirstName, b.label)
            from Balance b join b.referralProgram rp left join AppUser u on u.balance.id = b.id 
            where rp.id = :balanceRefProgramId
            order by b.id asc
    """)
    List<ReferralDto> findAllByBalRefProgId(Long balanceRefProgramId);

    @Modifying
    @Query("update Balance b set b.energyProvider = :value")
    void updateAllBalancesForEnergyProvider(EnergyProviderName value);

    @Modifying
    @Query("update Balance b set b.autoEnergyProvider = :value")
    void updateAllBalancesForAutoEnergyProvider(EnergyProviderName value);
}
