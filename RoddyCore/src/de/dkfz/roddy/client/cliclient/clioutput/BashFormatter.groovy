package de.dkfz.roddy.client.cliclient.clioutput;

/**
 * Created by michael on 25.08.14.
 */
public class BashFormatter extends ConsoleStringFormatter {
    public String formatAll(String str) {
        for (BashColours bc in BashColours.values()) {
            str = bc.format(str);
        }
        return str;
    }
}
