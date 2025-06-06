package org.ipan.nrgyrent.telegram.state.referral;

public interface RefProgramSearchState {
    Integer getCurrentPage();
    String getQuery();

    RefProgramSearchState withCurrentPage(Integer value);
    RefProgramSearchState withQuery(String value);
}
