package org.ipan.nrgyrent.telegram.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ManageGroupsLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    public String chooseUsers() {
        return getLocalizedMessage("manage_group.choose_users");
    }

    public String preview(String name, String manager, String date, String tariff, String active, String wallet, String balance) {
        return getLocalizedMessage("manage_group.preview", name, manager, date, tariff, active, wallet, balance);
    }

    public String menuReviewUsers() {
        return getLocalizedMessage("manage_group.preview.actions.review_users");
    }

    public String menuAssignManager() {
        return getLocalizedMessage("manage_group.preview.actions.assign_mananger");
    }

    public String menuChangeBalance() {
        return getLocalizedMessage("manage_group.preview.actions.change_balance");
    }

    public String menuAddUsers() {
        return getLocalizedMessage("manage_group.preview.actions.add_users");
    }

    public String menuRemoveUsers() {
        return getLocalizedMessage("manage_group.preview.actions.remove_users");
    }

    public String menuRename() {
        return getLocalizedMessage("manage_group.preview.actions.rename");
    }

    public String menuChangeTariff() {
        return getLocalizedMessage("manage_group.preview.actions.change_tariff");
    }

    public String menuDeactivate() {
        return getLocalizedMessage("manage_group.preview.actions.deactivate");
    }



    public String deactivateConfirm() {
        return getLocalizedMessage("manage_group.warning.confirm_deactivate");
    }
    public String deactivateSuccess() {
        return getLocalizedMessage("manage_group.result.deactivate.success");
    }

    public String renamePromptLabel() {
        return getLocalizedMessage("manage_group.prompt.new_label");
    }

    public String changeBalancePromptBalance() {
        return getLocalizedMessage("manage_group.prompt.new_balance");
    }

    public String addUsersPromptUsers() {
        return getLocalizedMessage("manage_group.prompt.new_users");
    }

    public String removeUsersPromptUsers() {
        return getLocalizedMessage("manage_group.prompt.remove_users");
    }

    public String renameSuccess() {
        return getLocalizedMessage("manage_group.result.renamed.success");
    }

    public String changeTariffSuccess() {
        return getLocalizedMessage("manage_group.result.tariff_change.success");
    }

    public String renameLabelShort() {
        return getLocalizedMessage("manage_group.warning.name_too_short");
    }

    public String changeBalanceSuccess() {
        return getLocalizedMessage("manage_group.change_tariff.success");
    }

    public String addUsersSuccess() {
        return getLocalizedMessage("manage_group.add_users.success");
    }

    public String removeUsersSuccess() {
        return getLocalizedMessage("manage_group.remove_users.success");
    }

    public String assignManagerSuccess() {
        return getLocalizedMessage("manage_group.assign_mananger.success");
    }

    public String assignManagerPrompt() {
        return getLocalizedMessage("manage_group.assign_mananger.prompt");
    }

    public String assignManagerPromptChooseUser() {
        return getLocalizedMessage("manage_group.assign_mananger.choose_user");
    }

    public String assignManagerPromptChooseManager() {
        return getLocalizedMessage("manage_group.assign_mananger.choose_manager");
    }

    public String removeUsersCantRemoveMngr() {
        return getLocalizedMessage("manage_group.remove_users.cant_remove_manager");
    }

    public String usersNotRegistered(String users) {
        return getLocalizedMessage("manage_group.users_not_registered", users);
    }

    public String usersBelongToAnotherGroup() {
        return getLocalizedMessage("manage_group.users_belong_to_another_group");
    }

    public String usersDisabled() {
        return getLocalizedMessage("manage_group.users_disabled");
    }

    public String notManager() {
        return getLocalizedMessage("manage_group.not_manager");
    }

    public String changeBalanceNotNegative() {
        return getLocalizedMessage("manage_group.balance_negative");
    }

    public String msg() {
        return getLocalizedMessage("manage_group.msg");
    }


    public String createSuccess() {
        return getLocalizedMessage("group_create.success");
    }

    public String createPromptLabel() {
        return getLocalizedMessage("group_create.prompt_label");
    }

    public String createPromptManager() {
        return getLocalizedMessage("group_create.assign_mananger.prompt");
    }

    public String userManagesOtherGroup() {
        return getLocalizedMessage("group_create.user_manager_other_group");
    }

    public String usersList(String users) {
        return getLocalizedMessage("manage_group.users_list", users);
    }

    public String usersListEmpty() {
        return getLocalizedMessage("manage_group.users_list_empty");
    }
}
