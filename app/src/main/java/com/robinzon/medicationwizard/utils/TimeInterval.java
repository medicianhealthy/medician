package com.robinzon.medicationwizard.utils;

public class TimeInterval {

    public static class Hour{
        public static int getFromMilliSeconds(final long milliSeconds){
            return (int) (milliSeconds / 1000 / 60 /60);
        }
    }

    public static class Seconds{
        public static int getFromHors(final float hours){
            return (int) (hours * 60F * 60F);
        }
    }


}
