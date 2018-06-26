package com.holenet.cowinfo.item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

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

    public String getKoreanBirthday() {
        if (birthday == null)
            return null;
        String[] birthday = this.birthday.split("-");
        int year = Integer.parseInt(birthday[0]);
        int month = Integer.parseInt(birthday[1]);
        int day = Integer.parseInt(birthday[2]);
        return String.format(Locale.KOREA, "%d년 %d월 %d일", year, month, day);
    }

    @Override
    public String toString() {
        return this.number;
    }
}
