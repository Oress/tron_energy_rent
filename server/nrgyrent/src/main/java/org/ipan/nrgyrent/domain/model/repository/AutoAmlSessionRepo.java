package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.autoaml.AutoAmlSession;
import org.ipan.nrgyrent.domain.model.projections.WalletWithAutoAmlSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutoAmlSessionRepo extends JpaRepository<AutoAmlSession, Long> {
    List<AutoAmlSession> findByAddressAndUser_TelegramIdAndActive(String address, Long userTelegramId, Boolean active);

    List<AutoAmlSession> findAllByActive(Boolean active);

    @Query(nativeQuery = true,
            value = """
            select
                coalesce(uv.address, s.address),
                uv.label,
                s.id,
                s.threshold_usdt
            from nrg_user_wallets uv
                full join nrg_auto_aml_sessions s
                    on uv.address = s.address
                   and s.is_active = true
                   and s.user_id = :userId
            where (uv.user_id is null or uv.user_id = :userId)
              and (s.is_active = true or s.is_active is null)
              and (s.user_id is null or s.user_id = :userId)
    """)
    List<WalletWithAutoAmlSession> findActiveSessionsWithWalletInfo(Long userId);
}
