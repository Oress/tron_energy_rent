package org.ipan.nrgyrent.dashboard;

import java.util.List;

/**
 * Generic page envelope returned by all dashboard report endpoints.
 * Matches the Page schemas in the Angular dashboard's OpenAPI contract
 * (content/page/size/totalElements).
 */
public record PageDto<T>(List<T> content, int page, int size, long totalElements) {

    public static <T> PageDto<T> empty(int page, int size) {
        return new PageDto<>(List.of(), page, size, 0);
    }
}
