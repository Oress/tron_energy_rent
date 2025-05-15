package org.ipan.nrgyrent.telegram;

import java.text.DecimalFormat;

public class StaticLabels {
    // COMMON
    public static final String MSG_MAIN_MENU_TEXT = """
            ⚡ Приветствуем в нашем сервисе ⚡

            Выберите действие, нажав кнопку ниже
            """;

    public static final String MSG_TRANSACTION_65K_TEXT = """
            ⚡ Транзакции

            На данной странице вы можете приобрести 65 000 энергии на 1 час за 5,5 TRX в сети TRC-20
            
            👇 Пожалуйста, выберите кошелек, для которого вы желаете перевести энергию 👇
            """;

    public static final String MSG_TRANSACTION_131K_TEXT = """
            ⚡ Транзакции

            На данной странице вы можете приобрести 131 000 энергии на 1 час за 8 TRX в сети TRC-20
            
            👇 Пожалуйста, выберите кошелек, для которого вы желаете перевести энергию 👇
            """;

    public static final String MSG_TRANSACTION_PROGRESS = "Работаем, пожалуйста, подождите...";

    public static final String MSG_TRANSACTION_PENDING = """
            ⏳ Транзакция в процессе
            Пожалуйста, подождите 5 минут. Если транзакция не завершится, средства будут возвращены на ваш баланс.
            Бот отправит вам уведомление, когда транзакция будет завершена.
            """;

    // NOTIFICATIONS
    public static final String MSG_TRANSACTION_SUCCESS = """
            ✅ Транзакция успешно завершена
            Энергия была переведена на ваш кошелек
            """;

    public static final String MSG_WALLETS = """
            \uD83D\uDC5B Кошельки
            Здесь вы можете управлять кошельками, которые добавили в нашем боте""";

    public static final String MSG_ADD_WALLET = "Отправьте адрес кошелька TRC-20, который вы хотите добавить";
    public static final String MSG_ADD_WALLET_SUCCESS = "✅ Кошелек успешно добавлен";
    public static final String MSG_DELETE_WALLET_SUCCESS = "\uD83D\uDDD1\uFE0F Кошелек успешно удален";


     // ADMIN
    public static final String MSG_ADMIN_MENU = """
            👨‍💻 Админка

            Здесь вы можете управлять группами, пользователями, а также просматривать и изменять их баланс
            """;


    // Menu labels
    public static final String MENU_ADMIN = "👨‍💻 Админка";
    public static final String MENU_TRANSFER_ENERGY_65K = "⚡ 65 000 энергии на 1 час (5.5 TRX)";
    public static final String MENU_TRANSFER_ENERGY_131K = "⚡ 131 000 энергии на 1 час (8 TRX)";
    public static final String MENU_DEPOSIT = "\uD83D\uDCB8 Депозит";
    public static final String MENU_WALLETS = "\uD83D\uDC5B Кошельки";

    // Admin menu labels
    public static final String MENU_ADMIN_MANAGE_GROUPS = "👥 Управление группами";
    public static final String MENU_ADMIN_MANAGE_USERS = "👤 Управление пользователями";


    public static final String TO_MAIN_MENU = "\uD83C\uDFE0 Главное меню";

    // Wallets labels
    public static final String WLT_DELETE_WALLET = "❌";
    public static final String WLT_ADD_WALLET = "➕ Добавить кошелек";


    // Notifications labels
    // TODO: make it label accept params
    public static final String NTFN_ORDER_SUCCESS = """
            ✅ Транзакция успешно завершена
            Энергия была переведена на ваш кошелек
            """;
    public static final String NTFN_ORDER_REFUNDED = """
            ❌ Транзакция была отменена
            Средства были возвращены на ваш баланс
            """;

    public static final String OK = "OK";

    private static DecimalFormat df = new DecimalFormat("# ###.#");
    public static String getDepositMenuText(String depositAddress, Long sunBalance) {
        return """
                💰 Ваш адресс депозита:
                
                `%s`
                
                💰 Баланс:
                
                *%s* TRX
                
                ❗️ Вы можете отправить только TRX сети TRC-20❗️
                
                ❗️ Минимальный депозит - 1 TRX❗️
                
                ⌛️ Среднее время зачисления депозита - 2 минуты."""
                .formatted(
                        depositAddress,
                        df.format(sunBalance / 1_000_000));
    }
}
