package com.robinzon.medicationwizard.utils;

public class TimeInterval {

    public static class Hour{
        public static int getFromMilliSeconds(final long milliSeconds){
            return (int) (milliSeconds / 1000 / 60 /60);
        }
    }
    /***************************************************************************************************************************/
    public static class Seconds{
        public static float getFromMilliSeconds(final long millisSeconds){
            return (millisSeconds / 1000F);
        }

        public static int getFromMinutes(final float minutes){
            return (int) (minutes * 60F);
        }

        public static int getFromHors(final float hours){
            return (int) (hours * 60F * 60F);
        }

        public static int getFromDays(final byte days){
            return days * 24 * 60 * 60;
        }
    }
/***************************************************************************************************************************/
    public static class MilliSeconds{

        public static long getFromSeconds(final int seconds){
            return (seconds * 1000L);
        }

        public static long getFromMinutes(final float minutes){
            return (long) (minutes * 60 * 1000L);
        }

        public static long getFromHors(final float hours){
            return (long) (hours * 60 * 60 * 1000L);
        }

        public static long getFromDays(final byte days){
            return days * 24 * 60 * 60 * 1000L;
        }
    }
}
