package net.voldrich.googleanalytics;

import java.time.LocalDate;
import java.util.Date;

public class BaseDailyStats {
    private long id;

    private LocalDate day;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDate getDay() {
        return day;
    }

    public void setDay(LocalDate day) {
        this.day = day;
    }

    public void populateCalculatedFields() {
        // do nothing
    }
}
