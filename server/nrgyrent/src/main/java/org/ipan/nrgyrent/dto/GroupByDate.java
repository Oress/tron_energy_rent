package org.ipan.nrgyrent.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class GroupByDate<T> {
    private LocalDate date;
    private List<T> items = new ArrayList<>();
}
