package com.dailycodework.excel2database.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Student model class - created to resolve compilation issues
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    private Long id;
    private String fname;
    private String lname;
    private String htno;
    private String email;
    private String department;
}
