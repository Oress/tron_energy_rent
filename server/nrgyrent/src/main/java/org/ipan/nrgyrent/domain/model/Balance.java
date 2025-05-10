package org.ipan.nrgyrent.domain.model;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

/*@Entity
@Getter
@Setter
public class Balance {
    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id") // With possibility to change for one-to-many
    private AppUser user;

    private BigDecimal balance;

    private String tokenSymbol;

    private String tokenType; // e.g., ERC20, ERC721, etc.
}*/
