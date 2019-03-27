package edu.temple.mapchat;

import com.google.android.gms.maps.model.LatLng;

public class Partner implements Comparable{

    private LatLng lastKnownPosition;
    private String name;
    private float distance;

    public LatLng getLastKnownPosition() {
        return lastKnownPosition;
    }

    public String getName() {
        return name;
    }

    public void setLastKnownPosition(float latititude, float longitude) {
       this.lastKnownPosition = new LatLng(latititude, longitude);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(Object p) {
        Partner o = (Partner) p;
        if(this.getDistance() < o.getDistance()){
            return -1;
        }
        else if(this.getDistance() == o.getDistance()){
            return 0;
        }
        else {
            return 1;
        }
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getDistance() {
        return distance;
    }
}
