package org.ipan.nrgyrent.telegram.state;

public interface GroupSearchState {
    Integer getCurrentPage();
    String getQuery();

    GroupSearchState withCurrentPage(Integer value);
    GroupSearchState withQuery(String value);
}
