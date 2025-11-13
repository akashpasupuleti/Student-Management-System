package com.dailycodework.excel2database.domain;

import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Subject {
    private int sno;
    private String htno;
    private String subcode;
    private String subname;
    private Integer internals;
    private String grade;
    private Double credit;
}
