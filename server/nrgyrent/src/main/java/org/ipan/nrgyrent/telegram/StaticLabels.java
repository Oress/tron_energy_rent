package org.ipan.nrgyrent.telegram;

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

    // NOTIFICATIONS
    public static final String MSG_WALLETS = """
            \uD83D\uDC5B Кошельки
            Здесь вы можете управлять кошельками, которые добавили в нашем боте""";

    // Menu labels
    public static final String MENU_ADMIN = "👨‍💻 Админка";
    public static final String MENU_TRANSFER_ENERGY_65K = "⚡ 65 000 энергии на 1 час (5.5 TRX)";
    public static final String MENU_TRANSFER_ENERGY_131K = "⚡ 131 000 энергии на 1 час (8 TRX)";
    public static final String MENU_DEPOSIT = "\uD83D\uDCB8 Депозит";
    public static final String MENU_WALLETS = "\uD83D\uDC5B Кошельки";

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


}
