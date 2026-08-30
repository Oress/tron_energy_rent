package org.ipan.nrgyrent.telegram;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InlineMenuCallbacks {
    public static final String MANAGE_USER_ACTION_ADJUST_WITHDRAW_LIMIT = "manage_user_action_adjust_withdraw_limit";
    private final static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

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
    public static final String ESTIMATE_TRANSACTION_COST = "estimate_transaction_cost";
    public static final String AUTOTOPUP = "autotopup";

    public static final String SETTINGS = "settings";
    public static final String HISTORY = "history";
    public static final String DEPOSIT_HISTORY = "deposit_history";
    public static final String MANAGE_GROUP = "manage_group";
    public static final String MANAGE_REFERALS = "manage_referals";
    public static final String WITHDRAW_TRX = "withdraw_trx";
    public static final String OPT_SHOW_WALLET_DISABLE = "opt_show_wallet_disable";
    public static final String OPT_SHOW_WALLET_ENABLE = "opt_show_wallet_enable";

    public static final String ADMIN_MENU = "admin_menu";

    public static final String MANAGE_GROUPS = "manage_groups";
    public static final String MANAGE_USERS = "manage_users";
    public static final String MANAGE_ITRX_BALANCE = "manage_itrx_balance";
    public static final String MANAGE_AUTO_ENERGY_PROVIDER = "manage_auto_energy_provider";
    public static final String MANAGE_AUTO_ENERGY_PROVIDER_CHOOSE_ITRX = "auto_energy_provider_choose_itrx";
    public static final String MANAGE_AUTO_ENERGY_PROVIDER_CHOOSE_TRXX = "auto_energy_provider_choose_trxx";
    public static final String MANAGE_ENERGY_PROVIDER = "manage_energy_provider";
    public static final String MANAGE_ENERGY_PROVIDER_CHOOSE_ITRX = "energy_provider_choose_itrx";
    public static final String MANAGE_ENERGY_PROVIDER_CHOOSE_CATFEE = "energy_provider_choose_catfee";
    public static final String MANAGE_ENERGY_PROVIDER_CHOOSE_NETTS = "energy_provider_choose_netts";
    public static final String MANAGE_SWEEP_BALANCE = "manage_sweep_balance";
    public static final String MANAGE_WITHDRAW_TRX = "manage_withdraw_trx";
    public static final String MANAGE_TARIFFS = "manage_tarifs";
    public static final String MANAGE_REFERRAL_PROGRAMS = "manage_ref_programs";
    public static final String MANAGE_AML_PROVIDER = "manage_aml_provider";
    public static final String MANAGE_AML_PROVIDER_CHOOSE_ELLIPTIC = "aml_provider_choose_elliptic";
    public static final String MANAGE_AML_PROVIDER_CHOOSE_BITOK = "aml_provider_choose_bitok";

    public static final String DEPOSIT_PREV_PAGE = "deposit_prev_page";
    public static final String DEPOSIT_NEXT_PAGE = "deposit_next_page";
    public static final String DEPOSIT_SEARCH = "deposit_search";

    public static final String MANAGE_TARIFFS_PREV_PAGE = "manage_tarifs_search_prev_page";
    public static final String MANAGE_TARIFFS_NEXT_PAGE = "manage_tarifs_search_next_page";
    public static final String MANAGE_TARIFFS_SEARCH_RESET = "manage_tarifs_search_reset";
    public static final String MANAGE_TARIFFS_SEARCH = "manage_tarifs_search";
    public static final String MANAGE_TARIFFS_ADD = "manage_tarifs_add_new";

    public static final String MANAGE_TARIFFS_ACTION_CHANGE_TX1_AMOUNT = "manage_tarifs_action_change_tx1_amount";
    public static final String MANAGE_TARIFFS_ACTION_CHANGE_TX2_AMOUNT = "manage_tarifs_action_change_tx2_amount";
    public static final String MANAGE_TARIFFS_ACTION_CHANGE_AML_PRICE = "manage_tarifs_action_change_aml_price";
    public static final String MANAGE_TARIFFS_ACTION_RENAME = "manage_tarifs_action_rename";
    public static final String MANAGE_TARIFFS_ACTION_DEACTIVATE = "manage_tarifs_action_deactivate";

    // ref programs
    public static final String MANAGE_REF_PROGRAMS_PREV_PAGE = "manage_ref_programs_search_prev_page";
    public static final String MANAGE_REF_PROGRAMS_NEXT_PAGE = "manage_ref_programs_search_next_page";
    public static final String MANAGE_REF_PROGRAMS_SEARCH_RESET = "manage_ref_programs_search_reset";
    public static final String MANAGE_REF_PROGRAMS_SEARCH = "manage_ref_programs_search";
    public static final String MANAGE_REF_PROGRAMS_ADD = "manage_ref_programs_add_new";

    public static final String MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX1 = "manage_ref_programs_action_change_base_tx1";
    public static final String MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX2 = "manage_ref_programs_action_change_base_tx2";
    public static final String MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX1_AUTO = "manage_ref_programs_action_change_base_tx1_auto";
    public static final String MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX2_AUTO = "manage_ref_programs_action_change_base_tx2_auto";

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
    public static final String MANAGE_GROUPS_ACTION_ADJUST_WITHDRAW_LIMIT = "manage_groups_action_adjust_withdraw_limit";
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
    public static final String MANAGE_USER_AUTODELEGATION = "manage_user_autodelegation";
    public static final String MANAGE_USER_AUTODELEGATION_PROVIDER_DEFAULT =
            "/admin/user-auto-provider/default";
    public static final String MANAGE_USER_AUTODELEGATION_PROVIDER_ITRX =
            "/admin/user-auto-provider/ITRX";
    public static final String MANAGE_USER_AUTODELEGATION_PROVIDER_TRXX =
            "/admin/user-auto-provider/TRXX";
    private static final String USER_AUTO_SESSION = "/admin/user-auto-session/";
    private static final String USER_AUTO_SWITCH = "/admin/user-auto-switch/";
    private static final String USER_AUTO_SWITCH_CONFIRM = "/admin/user-auto-switch-confirm/";
    private static final String USER_AUTO_DEACTIVATE = "/admin/user-auto-deactivate/";
    private static final String USER_AUTO_DEACTIVATE_CONFIRM = "/admin/user-auto-deactivate-confirm/";


    public static final String AML_CHECK = "aml_check";
    public static final String AML_HISTORY = "aml_history";

    public static final String AUTO_AML = "auto_aml";

    public static final String SETTINGS_AML_PROVIDER = "settings_aml_provider";
    public static final String SETTINGS_AML_PROVIDER_ELLIPTIC = "settings_aml_provider_elliptic";
    public static final String SETTINGS_AML_PROVIDER_BITOK = "settings_aml_provider_bitok";
    public static final String SETTINGS_AML_PROVIDER_BOTH = "settings_aml_provider_both";

    private static final String AML_VIEW_ITEM = "/aml_view/";
    public static String getAmlViewItemCallback(Long id) {
        return AML_VIEW_ITEM + id;
    }
    public static Long getAmlViewItemId(String data) {
        if (data != null && data.startsWith(AML_VIEW_ITEM)) {
            return Long.parseLong(data.substring(AML_VIEW_ITEM.length()));
        }
        return null;
    }

    public static final String NTFN_OK = "notification_ok";

    public static final String CONFIRM_YES = "confirm_yes";
    public static final String CONFIRM_NO = "confirm_no";

    public static String getUserAutoSessionCallback(Long sessionId) {
        return USER_AUTO_SESSION + sessionId;
    }

    public static Long getUserAutoSessionId(String data) {
        if (data != null && data.startsWith(USER_AUTO_SESSION)) {
            return Long.parseLong(data.substring(USER_AUTO_SESSION.length()));
        }
        return null;
    }

    public static String getUserAutoSwitchCallback(Long sessionId, EnergyProviderName provider) {
        return USER_AUTO_SWITCH + sessionId + "/" + provider;
    }

    public static Long getUserAutoSwitchSessionId(String data) {
        if (data != null && data.startsWith(USER_AUTO_SWITCH)) {
            String payload = data.substring(USER_AUTO_SWITCH.length());
            return Long.parseLong(payload.substring(0, payload.indexOf('/')));
        }
        return null;
    }

    public static EnergyProviderName getUserAutoSwitchProvider(String data) {
        if (data != null && data.startsWith(USER_AUTO_SWITCH)) {
            String payload = data.substring(USER_AUTO_SWITCH.length());
            return EnergyProviderName.valueOf(payload.substring(payload.indexOf('/') + 1));
        }
        return null;
    }

    public static String getUserAutoSwitchConfirmCallback(Long sessionId, EnergyProviderName provider) {
        return USER_AUTO_SWITCH_CONFIRM + sessionId + "/" + provider;
    }

    public static Long getUserAutoSwitchConfirmSessionId(String data) {
        if (data != null && data.startsWith(USER_AUTO_SWITCH_CONFIRM)) {
            String payload = data.substring(USER_AUTO_SWITCH_CONFIRM.length());
            return Long.parseLong(payload.substring(0, payload.indexOf('/')));
        }
        return null;
    }

    public static EnergyProviderName getUserAutoSwitchConfirmProvider(String data) {
        if (data != null && data.startsWith(USER_AUTO_SWITCH_CONFIRM)) {
            String payload = data.substring(USER_AUTO_SWITCH_CONFIRM.length());
            return EnergyProviderName.valueOf(payload.substring(payload.indexOf('/') + 1));
        }
        return null;
    }

    public static String getUserAutoDeactivateCallback(Long sessionId) {
        return USER_AUTO_DEACTIVATE + sessionId;
    }

    public static Long getUserAutoDeactivateSessionId(String data) {
        if (data != null && data.startsWith(USER_AUTO_DEACTIVATE)) {
            return Long.parseLong(data.substring(USER_AUTO_DEACTIVATE.length()));
        }
        return null;
    }

    public static String getUserAutoDeactivateConfirmCallback(Long sessionId) {
        return USER_AUTO_DEACTIVATE_CONFIRM + sessionId;
    }

    public static Long getUserAutoDeactivateConfirmSessionId(String data) {
        if (data != null && data.startsWith(USER_AUTO_DEACTIVATE_CONFIRM)) {
            return Long.parseLong(data.substring(USER_AUTO_DEACTIVATE_CONFIRM.length()));
        }
        return null;
    }


    private static final String QUICK_TRANSACTION = "/quick_tx/";
    public static String getQuickTxCallback(Long userWalletId) {
        return QUICK_TRANSACTION + userWalletId;
    }

    public static Long getWalletIdForQuickTx(String data) {
        Long walletId = null;
        if (data.startsWith(QUICK_TRANSACTION)) {
            String walletIdStr = data.split(QUICK_TRANSACTION)[1];
            walletId = Long.parseLong(walletIdStr);
        }
        return walletId;
    }

    private static final String AML_CHECK_WALLET = "/aml_check_wallet/";
    public static String getAmlCheckWalletCallback(Long userWalletId) {
        return AML_CHECK_WALLET + userWalletId;
    }

    public static Long getWalletIdForAmlCheck(String data) {
        if (data != null && data.startsWith(AML_CHECK_WALLET)) {
            return Long.parseLong(data.substring(AML_CHECK_WALLET.length()));
        }
        return null;
    }


    private static final String TOGGLE_REF_PROGRAM_SEBES = "/rp_sebes/";
    public static String createToggleRefProgramSebesCallback(Long refProgramId) {
        return TOGGLE_REF_PROGRAM_SEBES + gson.toJson(new ToggleRefProgramSebesPayload(refProgramId));
    }

    public static ToggleRefProgramSebesPayload getToggleRefProgramSebes(String data) {
        ToggleRefProgramSebesPayload payload = null;
        if (data.startsWith(TOGGLE_REF_PROGRAM_SEBES)) {
            String payloadStr = data.split(TOGGLE_REF_PROGRAM_SEBES)[1];

            try {
                payload = gson.fromJson(payloadStr, ToggleRefProgramSebesPayload.class);
            } catch (Exception e) {
                logger.error("Cannot extract payload for ToggleRefProgramSebesPayload ", e);
            }
        }
        return payload;
    }


    private static final String TOGGLE_AUTO_TOPUP = "/tatpp/";
    public static String createToggleAutoTopupCallback(String address, Long sessionId) {
        ToggleWalletSessionPayload payload;
        if (sessionId != null) {
            payload = new ToggleWalletSessionPayload(sessionId);
        } else {
            payload = new ToggleWalletSessionPayload(address);
        }
        return TOGGLE_AUTO_TOPUP + gson.toJson(payload);
    }

    public static ToggleWalletSessionPayload getToggleWalletSession(String data) {
        ToggleWalletSessionPayload payload = null;
        if (data.startsWith(TOGGLE_AUTO_TOPUP)) {
            String payloadStr = data.split(TOGGLE_AUTO_TOPUP)[1];

            try {
                payload = gson.fromJson(payloadStr, ToggleWalletSessionPayload.class);
            } catch (Exception e) {
                logger.error("Cannot extract payload for ToggleWalletSessionPayload ", e);
            }
        }
        return payload;
    }

    private static final String TOGGLE_AUTO_AML = "/taaml/";
    public static String createToggleAutoAmlCallback(String address, Long sessionId) {
        ToggleWalletSessionPayload payload;
        if (sessionId != null) {
            payload = new ToggleWalletSessionPayload(sessionId);
        } else {
            payload = new ToggleWalletSessionPayload(address);
        }
        return TOGGLE_AUTO_AML + gson.toJson(payload);
    }

    public static ToggleWalletSessionPayload getToggleAutoAmlSession(String data) {
        ToggleWalletSessionPayload payload = null;
        if (data.startsWith(TOGGLE_AUTO_AML)) {
            String payloadStr = data.split(TOGGLE_AUTO_AML)[1];

            try {
                payload = gson.fromJson(payloadStr, ToggleWalletSessionPayload.class);
            } catch (Exception e) {
                logger.error("Cannot extract payload for ToggleAutoAmlSession ", e);
            }
        }
        return payload;
    }

    @Getter
    public static class ToggleWalletSessionPayload {
        private String address;
        private Long sessionId;

        public ToggleWalletSessionPayload(String address) {
            this.address = address;
        }

        public ToggleWalletSessionPayload(Long sessionId) {
            this.sessionId = sessionId;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ToggleRefProgramSebesPayload {
        private Long refProgramId;
    }
}
