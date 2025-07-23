package org.ipan.nrgyrent.domain.model.repository;

import java.util.List;

import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.projections.WalletWithAutoTopupSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoDelegationSessionRepo extends JpaRepository<AutoDelegationSession, Long> {
    List<AutoDelegationSession> findByActive(Boolean active);
    List<AutoDelegationSession> findByAddressAndActive(String wallet, Boolean active);

    @Query(nativeQuery = true,
            value = """
            select
                coalesce(uv.address, s.address),
                uv.label,
                s.id
            from nrg_user_wallets uv
                full join nrg_autodelegation_sessions s on uv.address = s.address and s.is_active = true
            where (uv.user_id is null or uv.user_id = :userId) and (s.is_active = true or s.is_active is null)
    """)
    List<WalletWithAutoTopupSession> findActiveSessionsWithWalletInfo(Long userId);

//    @Query("select s from AutoDelegationSession s join s.events e join e.order o where o.id = :orderId")
//    AutoDelegationSession findSessionByOrderId(Long orderId);

    AutoDelegationSession findByUserTelegramIdAndActive(Long userId, Boolean active);

    List<AutoDelegationSession> findAllByActive(Boolean active);
}
