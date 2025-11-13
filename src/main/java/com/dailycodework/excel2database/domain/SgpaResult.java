package com.dailycodework.excel2database.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SgpaResult {
    private Long id;
    private String htno; 
    private Double sem_1_1;
    private Double sem_1_2;
    private Double sem_2_1;
    private Double sem_2_2;
    private Double sem_3_1;
    private Double sem_3_2;
    private Double sem_4_1;
    private Double sem_4_2;
}
