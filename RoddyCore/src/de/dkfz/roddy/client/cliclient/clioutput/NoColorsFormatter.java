package de.dkfz.roddy.client.cliclient.clioutput;

/**
 * Created by michael on 25.08.14.
 */
public class NoColorsFormatter extends ConsoleStringFormatter {

    @Override
    public String formatAll(String str) {
        for (NoColours bc : NoColours.values()) {
            str = bc.format(str);
        }
        return str;
    }
}
