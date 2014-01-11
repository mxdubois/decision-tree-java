package com.michaelxdubois.decisiontree;

import java.util.Arrays;

protected class Log {

    public static final int ERROR = 1;
    public static final int DEBUG = ERROR * 2;    
    public static final int INFO = DEBUG * 2;
    public static final int FILTERED = INFO * 2;
    public static final int VERBOSE = ERROR + DEBUG + INFO;
    public static final int SILENT = 0;

    public static final int DEBUG_LEVEL = SILENT;
    //public static final int DEBUG_LEVEL = VERBOSE + FILTERED;

    public static final String[] FILTERS = new String[] {};

    public static void d(String tag, String msg) {
        int debug = DEBUG_LEVEL & DEBUG;
        int filtered = DEBUG_LEVEL & FILTERED;
        if(debug == DEBUG) {
           if(filtered == FILTERED) {
               if(!Arrays.asList(FILTERS).contains(tag))
                   return;
           } 
           System.out.println(tag + ": " + msg);
        }
    }

}
