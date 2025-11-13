package com.dailycodework.excel2database.domain;

import lombok.Data;

@Data
public class Teacher {
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String password;
    private String role;
}
