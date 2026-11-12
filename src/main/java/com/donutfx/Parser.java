package com.donutfx;

public class Parser {
    /** Parses things like "5M", "$1.5B", "250,000", "10k". */
    public static double parseAmount(String s) {
        s = s.trim().replace("$", "").replace(",", "").toLowerCase();
        double mult = 1;
        if (!s.isEmpty()) {
            char c = s.charAt(s.length() - 1);
            if (c == 'k') mult = 1e3;
            else if (c == 'm') mult = 1e6;
            else if (c == 'b') mult = 1e9;
            else if (c == 't') mult = 1e12;
            if (mult != 1) s = s.substring(0, s.length() - 1);
        }
        try { return Double.parseDouble(s) * mult; } catch (Exception e) { return -1; }
    }

    public static String format(double v) {
        if (v >= 1e12) return trim(v / 1e12) + "T";
        if (v >= 1e9) return trim(v / 1e9) + "B";
        if (v >= 1e6) return trim(v / 1e6) + "M";
        if (v >= 1e3) return trim(v / 1e3) + "K";
        return trim(v);
    }
    private static String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.format("%.1f", d);
    }
}
