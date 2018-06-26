package com.holenet.cowinfo.item;

import java.io.Serializable;
import java.util.ArrayList;

public class Cow implements Serializable {
    public Integer id;
    public ArrayList<Record> records;
    public String created;
    public Boolean deleted;
    public String sex;
    public String number;
    public String mother_number;
    public String birthday;

    public Cow(String sex, String number, String mother_number, String birthday) {
        this.sex = sex;
        this.number = number;
        this.mother_number = mother_number;
        this.birthday = birthday;
    }

    @Override
    public String toString() {
        return this.number;
    }
}
