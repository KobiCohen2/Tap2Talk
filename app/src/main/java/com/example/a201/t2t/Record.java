package com.example.a201.t2t;

import java.io.File;

/**
 * A class represent record of voice message
 */
public class Record {

    private String location;
    private String userPhone;
    private File record;

    /**
     * Constructor
     * @param location - full path of the record in the phone
     * @param userPhone - userPhone that recorded the voice message
     * @param record - the record as file object
     */
    Record(String location, String userPhone, File record) {
        this.location = location;
        this.userPhone = userPhone;
        this.record = record;
    }

    /**
     * A method to get the location of the record
     * @return full path as string
     */
    String getLocation() {
        return location;
    }

    /**
     *A getter for user phone field
     * @return user phone as string
     */
    String getUserPhone() {
        return userPhone;
    }

    /**
     * A getter for record file
     * @return record as file object
     */
    File getRecord() {
        return record;
    }
}
