package com.zjut.graduate.Po;

import java.util.Date;

public class User {
    private Long id;
    private String username;
    private String password;
    private String role;
    private Date createdAt;

    public String getPassword() {
        return password;
    }
}

