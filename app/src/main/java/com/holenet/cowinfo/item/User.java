package com.holenet.cowinfo.item;

import java.io.Serializable;

public class User implements Serializable {
    public String username;
    public String password;
    public String auth_token;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
