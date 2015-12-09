package de.dkfz.roddy.tools

import de.dkfz.roddy.StringConstants;

/**
 * A class which accepts a String and tries to convert it to a time unit like
 * HHHH:MM, e.g. 180h == 180:00, 4m == 00:04
 * Created by heinold on 08.12.15.
 */
@groovy.transform.CompileStatic
public class TimeUnit {

    private static final Map<String, Integer> mapOfValidTimeUnits = [
            s: 0,
            S: 0,
            m: 1,
            M: 1,
            h: 2,
            H: 2,
            d: 3,
            D: 3,
    ]

    private static final int[] turnaroundValues = [10000, 24, 60, 60]

    private String timeString;

    private String originalString;

    public TimeUnit(String str) {
        // Catch empty strings and resolve to 1 hour.
        if (!str) {
            originalString = timeString = "01:00:00";
            return;
        }

        // Check for too small values. Or char only values
        if (str.size() == 1 && !str.isNumber()) {
            str = "1" + str;
        }

        if (str.isNumber()) { //Else would not work,e.g. 4m would become 4mm
            str = str + "s";
        }

        // Check for malformed string with multiple chars at the end.
        if (mapOfValidTimeUnits.keySet().contains(str[-1])) {
            if (!str[-2].isNumber())
                throw new NumberFormatException("The Time unit string is malformed: $str");
        }

        if (str.contains(".") || !str[-1].isNumber()) { //Check things like 4m 3.5d etc. and convert them to xx:xx:xx which is then parsed again.
            String[] newStrList = ["00", "00", "00", "00"];

            String unit = str[-1];
            String timeStr = str[0..-2];
            if (mapOfValidTimeUnits[unit] == null)
                throw new NumberFormatException("The unit for the Time unit string is not known: $str");

            String[] timeValues = timeStr.split(StringConstants.SPLIT_STOP);

            int unitIndex = mapOfValidTimeUnits[unit]
            int j = 0
            for (int i = unitIndex; i >= 0 && j < timeValues.size(); i--) {
                if (i < unitIndex) {
                    newStrList[i] = ("0." + timeValues[j]).toFloat() * turnaroundValues[-i - 1] as Integer;
                } else {
                    newStrList[i] = timeValues[j];
                }
                j++;
            }
            str = newStrList.reverse().join(":")
        }

        // Check if it is a properly formatted
        if (str.contains(":")) {
            String[] timevalues = ["00", "00", "00", "00"];
            String[] foundTimeValues = str.split(StringConstants.SPLIT_COLON);

            if (foundTimeValues.size() > 4) throw new NumberFormatException("There are too many parts for this Time unit string: $str");
            if (foundTimeValues.findAll { it -> !it.isInteger() }.size() > 0) throw new NumberFormatException("All parts of Time unit strings need to be integers: $str");

            for (int i = 1; i < foundTimeValues.size() + 1; i++) {
                timevalues[-i] = foundTimeValues[-i];
            }
            List<String> listOfCorrectedValues = [];

            // Assemble from back to front
            int addition = 0;
            int j = turnaroundValues.size() - 1;
            for (int i = timevalues.size() - 1; i >= 0; i-- && j--) {
                int number = (addition + timevalues[i].toInteger()) % turnaroundValues[j];
                addition = ((addition + timevalues[i].toInteger()) / turnaroundValues[j]) as Integer;
                listOfCorrectedValues << ("" + number).padLeft(2, "0");
            }

            listOfCorrectedValues = listOfCorrectedValues.reverse()

            while (listOfCorrectedValues[0].toInteger() == 0) {
                listOfCorrectedValues.remove(0);
            }

            // Always append the seconds!
            timeString = listOfCorrectedValues.join(":")
        }
    }

    @Override
    public String toString() {
        return timeString;
    }
}
