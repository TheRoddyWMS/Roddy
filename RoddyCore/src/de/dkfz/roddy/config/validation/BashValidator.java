package de.dkfz.roddy.config.validation;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationValue;

/**
 * This validator check values of type
 *  bashArray
 */
public class BashValidator extends ConfigurationValueValidator {
    public BashValidator(Configuration cfg) {
        super(cfg);
    }

    @Override
    public boolean validate(ConfigurationValue configurationValue) {
          //TODO Handle bash Array validations.
//        for (int i = 1; i < temp.length - 1; i++) {
//
//            //Detect if value is a range { .. }
//            if (temp[i].startsWith(StringConstants.BRACE_LEFT) && temp[i].endsWith(StringConstants.BRACE_RIGHT) && temp[i].contains("..")) {
//                String[] rangeTemp = temp[i].split("..");
//                int start = Integer.parseInt(rangeTemp[1].trim());
//                int end = Integer.parseInt(rangeTemp[2].trim());
//                for (int j = start; j <= end; j++) {
//                    resultStrings.add("" + j);
//                }
//            } else {
//                //Just append the value.
//                resultStrings.add(temp[i]);
//            }
//        }

        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
