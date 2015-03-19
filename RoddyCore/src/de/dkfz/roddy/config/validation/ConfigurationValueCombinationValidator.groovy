package de.dkfz.roddy.config.validation

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import static de.dkfz.roddy.StringConstants.*

/**
 */
@groovy.transform.CompileStatic
public class ConfigurationValueCombinationValidator extends ConfigurationValidator {

    ConfigurationValueCombinationValidator(Configuration cfg) {
        super(cfg)
    }

    @Override
    public boolean validate() {
        def listOfAllValues = configuration.getConfigurationValues().getListOfAllValues().findAll { String it -> it.startsWith("cfgValidationRule") };
        Map<String, List<String>> rulesByGroup = [:];
        for (String id in listOfAllValues) {
            String[] split = id.split(SPLIT_UNDERSCORE);
            String group = split[1];
            String secondaryID = split[2];
            rulesByGroup.get(group, []) << id;
        }

        //Check group wise. At least one value in the group has to valid. If this is the case, the group is valid.
        for (groupID in rulesByGroup.keySet()) {
            final List<String> idsInGroup = rulesByGroup[groupID];
            boolean oneWasValid = false;
            for (String id in idsInGroup) {
                final ConfigurationValue cv = configuration.getConfigurationValues().get(id);
                final String[] splittedValues = cv.getValue().toString().split(SPLIT_SEMICOLON);
                List<List<String[]>> variants = [];
                for (int i = 0; i < splittedValues.length; i++) {
                    String[] _sv = splittedValues[i].split(SPLIT_EQUALS);
                    String[] values = _sv[1].split(SPLIT_SLASH);
                    variants << [];
                    for (String val in values) {
                        String[] t = new String[2];
                        t[0] = _sv[0];
                        t[1] = val;
                        variants[i].add(t);
                    }
                }

                oneWasValid = check(configuration, variants, 0);
                if (oneWasValid) break;

//                addErrorToList(new ValidationError(configuration, "Could not apply at least one configuration value combination!", "A combination of values in the configuration does not fit.", null));

            }
            if (!oneWasValid) {

                addErrorToList(new ValidationError(configuration, "Could not apply at least one configuration value combination ${groupID}!", "A combination of values in the configuration does not fit.", null));

                configuration.addValidationError(new ConfigurationValidationError.ConfigurationValueCombinationMismatch(configuration, groupID));
            }
        }
    }

    boolean check(Configuration configuration, List<List<String[]>> variants, int indexInVariants) {
        for (int i = 0; i < variants[indexInVariants].size(); i++) {
//            System.out.println("Checking " + variants[indexInVariants][i]);
            String var = variants[indexInVariants][i][0];
            String val = variants[indexInVariants][i][1];
            ConfigurationValue _var = configuration.getConfigurationValues().get(var);
            ConfigurationValue _val = configuration.getConfigurationValues().get(val);
            boolean current = _var.toString().equals(_val.toString())

            if (!current)
                continue;

            System.out.println(String.format("%${indexInVariants*2+6}s: %s", "Match", _var.toString()));

            if (indexInVariants + 1 < variants.size()) {
                if (check(configuration, variants, indexInVariants + 1))
                    return true;
            } else
                return true;
        }

        return false;
    }
}
