package me.desht.pneumaticcraft.common.event;

import java.util.Calendar;

public class DateEventHandler {
    public static boolean isEvent() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        if (month == Calendar.APRIL && day == 17) { //MineMaarten's birthday
            return true;
        } else if (month == Calendar.DECEMBER && day == 31) { //New Years eve
            return true;
        } else if (month == Calendar.JUNE && day == 9) { //PneumaticCraft's birthday
            return true;
        } else {
            return month == Calendar.FEBRUARY && day == 19;
        }
    }

    public static boolean isIronManEvent() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        // Iron Man (1) premiere | Iron Man 2 premiere | Iron Man 3 premiere | Avengers premiere
        return month == Calendar.APRIL && (day == 14 || day == 26 || day == 18 || day == 11);
    }
}
