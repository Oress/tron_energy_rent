package org.ipan.nrgyrent.telegram;

public class StaticLabels {
    // COMMON
    public static final String MSG_MAIN_MENU_TEXT = """
            ⚡ Приветствуем в нашем сервисе ⚡

            Выберите действие, нажав кнопку ниже, время аренды - 1 час
            """;

    // NOTIFICATIONS
    public static final String MSG_WALLETS = """
            \uD83D\uDC5B Кошельки
            Здесь вы можете управлять кошельками, которые добавили в нашем боте""";

    // Menu labels
    public static final String MENU_MANAGE_GROUP = "👥 Управление группой";
    public static final String WITHDRAW_TRX = "💰 Вывод TRX";
    public static final String MENU_HISTORY = "📜 История транзакций";
    public static final String MENU_ADMIN = "👨‍💻 Админка";
    public static final String MENU_TRANSFER_ENERGY_65K = "⚡ 1 тр на кош с USDT (5.5 TRX)";
    public static final String MENU_TRANSFER_ENERGY_131K = "⚡ 1 тр на кош без USDT или биржу (8.6 TRX)";
    public static final String MENU_DEPOSIT = "\uD83D\uDCB8 Депозит TRX";
    public static final String MENU_WALLETS = "\uD83D\uDC5B Кошельки";

    public static final String TO_MAIN_MENU = "\uD83C\uDFE0 Главное меню";
    public static final String GO_BACK = "🔙 Назад";

    // Wallets labels
    public static final String WLT_DELETE_WALLET = "❌";
    public static final String WLT_ADD_WALLET = "➕ Добавить кошелек";


    // Notifications labels
    public static final String NTFN_BALANCE_TOPUP = """
            ✅ Баланс успешно пополнен
            TRX были добавлены на ваш кошелек
            """;

    public static final String NTFN_ORDER_SUCCESS = """
            ✅ Транзакция успешно завершена
            Энергия была переведена на ваш кошелек
            """;
    public static final String NTFN_ORDER_REFUNDED = """
            ❌ Транзакция была отменена
            Средства были возвращены на ваш баланс
            """;
    public static final String NTFN_WITHDRWAL_SUCCESS = """
            ✅ Вывод средств успешно завершен
            Средства были переведены на ваш кошелек
            """;
    public static final String NTFN_WITHDRWAL_FAIL = "❌ Вывод средств не удался";

    public static final String OK = "OK";


}
