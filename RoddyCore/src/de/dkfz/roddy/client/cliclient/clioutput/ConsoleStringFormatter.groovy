/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.cliclient.clioutput

import groovy.transform.CompileStatic;

/**
 * Created by michael on 25.08.14.
 */
@CompileStatic
public abstract class ConsoleStringFormatter {

    private static ConsoleStringFormatter _formatter = null

    static ConsoleStringFormatter getFormatter() {
        if (_formatter == null) {
            //Decide which formatter can be used.
            def env = System.getenv()
            boolean isLinux = env.get("OSTYPE") == "linux";
            boolean isBash = env.get("SHELL")?.contains("bash");

        if (isLinux && isBash) { //Simple check if we are using linux and a color code aware console
            if (System.console() != null)
                return new BashFormatter();
        }
            _formatter = new NoColorsFormatter();
        }
        return _formatter;
    }

    public abstract String formatAll(String str);
}
