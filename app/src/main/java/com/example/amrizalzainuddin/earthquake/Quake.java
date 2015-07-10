package com.example.amrizalzainuddin.earthquake;

import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by amrizal.zainuddin on 9/7/2015.
 */
public class Quake {
    private Date date;
    private String details;
    private Location location;
    private double magnitude;
    private String link;

    public Location getLocation() {
        return location;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public String getLink() {
        return link;
    }

    public String getDetails() {
        return details;
    }

    public Date getDate() {
        return date;
    }

    public Quake(Date _d, String _det, Location _loc, double _mag, String _link) {
        this.date = _d;
        this.details = _det;
        this.location = _loc;
        this.magnitude = _mag;
        this.link = _link;
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String dateString = sdf.format(date);
        return dateString + ": " + magnitude + " " + details;
    }
}
