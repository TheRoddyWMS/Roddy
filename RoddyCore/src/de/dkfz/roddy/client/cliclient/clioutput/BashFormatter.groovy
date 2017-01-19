/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.cliclient.clioutput;

/**
 * Created by michael on 25.08.14.
 */
public class BashFormatter extends ConsoleStringFormatter {
    public String formatAll(String str) {
        for (BashColours bc in BashColours.values()) {
            str = "" + (char)27 + bc.format(str);
        }
        return str;
    }
}
