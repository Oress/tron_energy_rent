package org.ipan.nrgyrent.telegram;

public class InlineMenuCallbacks {
    public static final String TO_MAIN_MENU = "action_main_menu";
    public static final String GO_BACK = "action_go_back";
    public static final String DEPOSIT = "deposit";
    public static final String CHANGE_LANGUAGE = "change_language";

    public static final String WALLETS = "wallets";
    public static final String ADD_WALLETS = "add_wallet";
    public static final String DELETE_WALLETS = "delete_wallet";
    public static final String TRANSACTION_65k = "transaction_65k";
    public static final String TRANSACTION_131k = "transaction_131k";
    public static final String CUSTOM_TRANSACTION_AMOUNT = "custom_transaction_amount";

    public static final String SETTINGS = "settings";
    public static final String HISTORY = "history";
    public static final String MANAGE_GROUP = "manage_group";
    public static final String MANAGE_REFERALS = "manage_referals";
    public static final String WITHDRAW_TRX = "withdraw_trx";
    public static final String OPT_SHOW_WALLET_DISABLE = "opt_show_wallet_disable";
    public static final String OPT_SHOW_WALLET_ENABLE = "opt_show_wallet_enable";

    public static final String ADMIN_MENU = "admin_menu";

    public static final String MANAGE_GROUPS = "manage_groups";
    public static final String MANAGE_USERS = "manage_users";
    public static final String MANAGE_ITRX_BALANCE = "manage_itrx_balance";
    public static final String MANAGE_SWEEP_BALANCE = "manage_sweep_balance";
    public static final String MANAGE_WITHDRAW_TRX = "manage_withdraw_trx";
    public static final String MANAGE_TARIFFS = "manage_tarifs";
    public static final String MANAGE_REFERRAL_PROGRAMS = "manage_ref_programs";

    public static final String MANAGE_TARIFFS_PREV_PAGE = "manage_tarifs_search_prev_page";
    public static final String MANAGE_TARIFFS_NEXT_PAGE = "manage_tarifs_search_next_page";
    public static final String MANAGE_TARIFFS_SEARCH_RESET = "manage_tarifs_search_reset";
    public static final String MANAGE_TARIFFS_SEARCH = "manage_tarifs_search";
    public static final String MANAGE_TARIFFS_ADD = "manage_tarifs_add_new";

    public static final String MANAGE_TARIFFS_ACTION_CHANGE_TX1_AMOUNT = "manage_tarifs_action_change_tx1_amount";
    public static final String MANAGE_TARIFFS_ACTION_CHANGE_TX2_AMOUNT = "manage_tarifs_action_change_tx2_amount";
    public static final String MANAGE_TARIFFS_ACTION_RENAME = "manage_tarifs_action_rename";
    public static final String MANAGE_TARIFFS_ACTION_DEACTIVATE = "manage_tarifs_action_deactivate";

    // ref programs
    public static final String MANAGE_REF_PROGRAMS_PREV_PAGE = "manage_ref_programs_search_prev_page";
    public static final String MANAGE_REF_PROGRAMS_NEXT_PAGE = "manage_ref_programs_search_next_page";
    public static final String MANAGE_REF_PROGRAMS_SEARCH_RESET = "manage_ref_programs_search_reset";
    public static final String MANAGE_REF_PROGRAMS_SEARCH = "manage_ref_programs_search";
    public static final String MANAGE_REF_PROGRAMS_ADD = "manage_ref_programs_add_new";

    public static final String MANAGE_REF_PROGRAMS_ACTION_CHANGE_PERCENTAGE = "manage_ref_programs_action_change_percentage";
    public static final String MANAGE_REF_PROGRAMS_ACTION_RENAME = "manage_ref_programs_action_rename";
    public static final String MANAGE_REF_PROGRAMS_ACTION_DEACTIVATE = "manage_ref_programs_action_deactivate";


    public static final String MANAGE_GROUPS_PREV_PAGE = "manage_groups_search_prev_page";
    public static final String MANAGE_GROUPS_NEXT_PAGE = "manage_groups_search_next_page";
    public static final String MANAGE_GROUPS_SEARCH = "manage_groups_search";
    public static final String MANAGE_GROUPS_SEARCH_RESET = "manage_groups_search_reset";
    public static final String MANAGE_GROUPS_ADD = "manage_groups_add_new";

    public static final String MANAGE_USERS_SEARCH = "manage_groups_search";
    public static final String MANAGE_USERS_SEARCH_RESET = "manage_users_search_reset";
    public static final String MANAGE_USERS_PREV_PAGE = "manage_users_search_prev_page";
    public static final String MANAGE_USERS_NEXT_PAGE = "manage_users_search_next_page";

    public static final String MANAGE_GROUPS_ACTION_CHANGE_MANAGER = "manage_groups_action_change_manager";
    public static final String MANAGE_GROUPS_ACTION_ADJUST_BALANCE_MANUALLY = "manage_groups_action_adjust_balance_manually";
    public static final String MANAGE_GROUPS_ACTION_RENAME = "manage_groups_action_rename";
    public static final String MANAGE_GROUPS_ACTION_CHANGE_TARIFF = "manage_groups_action_change_tariff";
    public static final String MANAGE_GROUPS_ACTION_VIEW_USERS = "manage_groups_action_view_users";
    public static final String MANAGE_GROUPS_ACTION_ADD_USERS = "manage_groups_action_add_users";
    public static final String MANAGE_GROUPS_ACTION_REMOVE_USERS = "manage_groups_action_remove_users";
    public static final String MANAGE_GROUPS_ACTION_DEACTIVATE = "manage_groups_action_deactivate";

    public static final String MANAGE_USER_ACTION_DEACTIVATE = "manage_user_action_deactivate";
    public static final String MANAGE_USER_ACTION_ADJUST_BALANCE_MANUALLY = "manage_user_action_adjust_balance_manually";
    public static final String MANAGE_USER_ACTION_CHANGE_TARIFF = "manage_user_action_change_tariff";
    public static final String MANAGE_USER_ACTION_CHANGE_REF_PROGRAM = "manage_user_action_change_ref_program";


    public static final String NTFN_OK = "notification_ok";

    public static final String CONFIRM_YES = "confirm_yes";
    public static final String CONFIRM_NO = "confirm_no";


    private static final String QUICK_TRANSACTION = "/quick_tx/";
    public static String getQuickTxCallback(Long userWalletId) {
        return QUICK_TRANSACTION + userWalletId;
    }

    public static boolean isQuickTxCallback(String data) {
        return data.startsWith(QUICK_TRANSACTION);
    }

    public static Long getWalletIdForQuickTx(String data) {
        Long walletId = null;
        if (data.startsWith(QUICK_TRANSACTION)) {
            String walletIdStr = data.split(QUICK_TRANSACTION)[1];
            walletId = Long.parseLong(walletIdStr);
        }
        return walletId;
    }
}
