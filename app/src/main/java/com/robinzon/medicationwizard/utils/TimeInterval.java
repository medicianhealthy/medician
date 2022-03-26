package com.robinzon.medicationwizard.utils;

public class TimeInterval {

    public static class Hour {
        public static float getFromMilliSeconds(final int millisSeconds) {
            return (float) millisSeconds / 1000F / 60F / 60F;
        }

        public static float getFromSeconds(final int seconds) {
            return (float) seconds / 60F / 60F;
        }

        public static float getFromMinutes(final float minutes) {
            return minutes / 60F;
        }

        public static float getFromHors(final float hours) {
            return hours;
        }

        public static float getFromDays(final float days) {
            return days * 24F;
        }
    }

    public static class Seconds {
        public static float getFromMilliSeconds(final int millisSeconds) {
            return millisSeconds / 1000F;
        }

        public static int geSeconds(final int seconds) {
            return seconds;
        }

        public static int getFromMinutes(final float minutes) {
            return (int) (minutes * 60);
        }

        public static int getFromHors(final float hours) {
            return (int) (hours * 60F * 60F);
        }

        public static int getFromDays(final float days) {
            return (int) (days * 24F * 60F * 60F);
        }
    }

    public static class MilliSeconds {

        public static long getFromMilliSeconds(final long millisSeconds) {
            return millisSeconds;
        }

        public static long getFromSeconds(final float seconds) {
            return (long) (seconds * 1000F);
        }

        public static long getFromMinutes(final float minutes) {
            return (long) (minutes * 60 * 1000L);
        }

        public static long getFromHors(final float hours) {
            return (long) (hours * 60 * 60 * 1000L);
        }

        public static long getFromDays(final byte days) {
            return days * 24 * 60 * 60 * 1000L;

        }
    }

    public static class Minutes {

        public static float getFromMilliSeconds(final long millisSeconds) {
            return millisSeconds / 1000F / 50F;
        }

        public static float getFromSeconds(final float seconds) {
            return seconds / 60F;
        }

        public static float getFromMinutes(final float minutes) {
            return minutes;
        }

        public static float getFromHors(final float hours) {
            return hours * 60F;
        }

        public static float getFromDays(final byte days) {
            return days *24F * 60F;
        }
    }
}
