package com.holenet.cowinfo.item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

public class Cow implements Serializable {
    public Integer id;
    public ArrayList<Record> records;
    public String created;
    public Boolean deleted = false;
    public String sex;
    public String number;
    public String mother_number;
    public Integer mother_id;
    public String birthday;
    public String summary;

    public Cow(Integer id) {
        this.id = id;
        records = new ArrayList<>();
    }

    public Cow(Integer id, ArrayList<Record> records, String created, Boolean deleted, String sex, String number, String mother_number, Integer mother_id, String birthday, String summary) {
        this.id = id;
        this.records = records;
        this.created = created;
        this.deleted = deleted;
        this.sex = sex;
        this.number = number;
        this.mother_number = mother_number;
        this.mother_id = mother_id;
        this.birthday = birthday;
        this.summary = summary;
    }

    public Cow(String sex, String number, String mother_number, String birthday) {
        this.deleted = false;
        this.sex = sex;
        this.number = number;
        this.mother_number = mother_number;
        this.birthday = birthday;
    }

    public Cow(Cow cow, String sex, String number, String mother_number, String birthday) {
        this.id = cow.id;
        this.records = cow.records;
        this.created = cow.created;
        this.deleted = cow.deleted;
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

    public Cow copy() {
        return new Cow(id, records, created, deleted, sex, number, mother_number, mother_id, birthday, summary);
    }

    @Override
    public String toString() {
        return this.number;
    }
}
