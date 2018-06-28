package com.holenet.cowinfo.item;

import java.io.Serializable;
import java.util.Locale;

public class Record implements Serializable {
    public Integer id;
    public String created;
    public String content;
    public String etc;
    public String day;
    public Integer cow;

    public Record(String content, String etc, String day, Integer cow) {
        this.content = content;
        this.etc = etc;
        this.day = day;
        this.cow = cow;
    }

    public String getKoreanDay() {
        String[] days = this.day.split("-");
        int year = Integer.parseInt(days[0]);
        int month = Integer.parseInt(days[1]);
        int day = Integer.parseInt(days[2]);
        return String.format(Locale.KOREA, "%d년 %d월 %d일", year, month, day);
    }
}
