package org.ipan.nrgyrent.telegram.state;

public interface UserEdit {
    Long getSelectedUserId();

    UserEdit withSelectedUserId(Long value);
}
