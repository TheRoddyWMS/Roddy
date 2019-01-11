/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.cliclient.clioutput

/**
 * Created by michael on 25.08.14.
 */
class BashFormatter extends ConsoleStringFormatter {
    String formatAll(String target) {
        String res = target
        for (BashColours color in BashColours.values()) {
            res = String.valueOf(27 as Character) + color.format(res)
        }
        res
    }
}
