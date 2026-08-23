package org.ipan.nrgyrent.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Row of the "Депозиты" report table.
 *
 * Column mapping (SQL -> field):
 * id -> id, Дата -> date, TRX -> trx, USDT -> usdt, Тип Баланса -> balanceType,
 * Группа\Логин -> groupLogin, Имя -> name, Активация аккаунта -> accountActivated,
 * Депозит кош. -> depositWallet, Отправитель -> sender, Статус -> status,
 * Транзакция -> transaction.
 */
public record DepositRowDto(
        Long id,
        LocalDateTime date,
        BigDecimal trx,
        BigDecimal usdt,
        String balanceType,
        String groupLogin,
        String name,
        Boolean accountActivated,
        String depositWallet,
        String sender,
        String status,
        String transaction) {
}
