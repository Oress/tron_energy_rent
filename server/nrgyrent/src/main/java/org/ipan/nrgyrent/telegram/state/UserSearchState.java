package org.ipan.nrgyrent.telegram.state;

public interface UserSearchState {
    Integer getCurrentPage();
    String getQuery();

    UserSearchState withCurrentPage(Integer value);
    UserSearchState withQuery(String value);
}
