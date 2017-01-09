/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Provides basic fields and methods for classes which represent a jobState of i.e. data sets which for a given point in time.
 */
public class InfoObject {
    private static final String TIMESTAMP_FORMAT = "yyMMdd_HHmm";
    private static final String TIMESTAMP_FORMAT_LONG = "yyMMdd_HHmmssSS";
    /**
     * A timestamp which can be updated
     */
    private Date timeStamp;

    public InfoObject() {
        this.timeStamp = new Date();
    }

    public static String formatTimestampReadable(Date timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd / HH:mm");
        return sdf.format(timestamp);
    }

    public static String formatTimestamp(Date timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT_LONG);

            return sdf.format(timestamp);
        } catch (Exception ex) {
            return "" + timestamp.getTime();
        }
    }

    public static Date parseTimestampString(String ts) {
        try {
            if(ts.length() == TIMESTAMP_FORMAT.length())
                return new SimpleDateFormat(TIMESTAMP_FORMAT).parse(ts);
            else
                return new SimpleDateFormat(TIMESTAMP_FORMAT_LONG).parse(ts);
        } catch (Exception ex) {
            return new Date();
        }
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void updateTimeStamp(Date date) {
        if (date == null)
            updateTimeStamp();
        else
            this.timeStamp = date;
    }

    public void updateTimeStamp() {
        this.timeStamp = new Date();
    }

    public int getAgeInSeconds() {
        long age = (int)(((new Date().getTime()) - timeStamp.getTime()) / 1000);
//        System.out.println(age);
        return age > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)age;
    }

    @Override
    public String toString() {
        return formatTimestamp(timeStamp);
    }
}
