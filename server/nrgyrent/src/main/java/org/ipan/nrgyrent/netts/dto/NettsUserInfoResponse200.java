package org.ipan.nrgyrent.netts.dto;

import lombok.Data;

@Data
public class NettsUserInfoResponse200 {
    private String status;
    private String timestamp;
    private Long userId;
    private Stats stats;

    @Data
    public static class Stats {
        private Double balance;
        private Long totalDelegations;
        private Long totalEnergyDelegated;
        private Double totalTrxSpent;
        private Double totalDeposit;
        private Double avgRateSunEnergy;
        private Double saveByNettsPercent;
        private Double saveInDollars;
    }
}

/*{
    "status": "success",
    "timestamp": "2025-09-02 05:56:11",
    "user_id": 6,
    "stats": {
        "balance": 28361.15,
        "total_delegations": 96756,
        "total_energy_delegated": 21687058023,
        "total_trx_spent": 832181.95,
        "total_deposit": 0,
        "avg_rate_sun_energy": 38.37,
        "save_by_netts_percent": 61.63,
        "save_in_dollars": 453267.36
    },
    "network_info": {
        "trx_price": 0.3391,
        "network_energy_fee": 100.0
    }
}*/