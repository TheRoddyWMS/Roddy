/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools;

import java.io.Console;
import java.util.List;
import java.util.Scanner;

import static de.dkfz.roddy.StringConstants.EMPTY;

/**
 * A wrapper class for the command line scanner class.
 * It allows reading strings and boolean values from the command line.
 */
public class ScannerWrapper {

    /**
     * The scanner object
     */
    private Scanner sc = new Scanner(System.in);

    public boolean getBooleanYN(String query, boolean def) {
        String defString = def ?  " [Y/n] " : " [y/N] ";
        System.out.print(query + defString);
        String keyLine = sc.nextLine();
        if(keyLine == null || keyLine.trim().equals(EMPTY)) {
            return def;
        }
        return keyLine != null && keyLine.toLowerCase().startsWith("y");
    }

    public boolean getBooleanYN() {
        String keyLine = sc.nextLine();
        return keyLine != null && keyLine.toLowerCase().startsWith("y");
    }

    public String getString(String query, String def) {
        def = def == null ? EMPTY : def;
        System.out.print(query + String.format(" [%s] ", def));
        String nl = getString();
        if(nl.equals(EMPTY))
            return def;
        return nl;
    }

    public String getString() {
        String nl = sc.nextLine();
        return nl != null ? nl : EMPTY;
    }

    public String getPasswordString() {
        Console console = System.console();
        String nl;
        if (console != null)
            nl = new String(console.readPassword());
        else
            nl = sc.nextLine();
        return nl != null ? nl : "";
    }


    /**
     * Convenience method for getChoice... where selectedValue defaults to null
     */
    public int getChoice(String query, List<String> options, int defaultValue) {
        return getChoice(query, options, defaultValue, null);
    }

    /**
     * Returns an integer in the range 1 <= choice <= options.length.
     * Returns defaultValue (or the selectedValue) if no correct value was entered.
     * Shows query, followed by a list of options identified by integers. The selected option
     * has a prefix " * "
     *
     * @param defaultValue
     * @param query
     * @param options
     * @param selectedValue
     * @return
     */
    public int getChoice(String query, List<String> options, int defaultValue, String selectedValue) {
        int choice = 0;
        while (choice < 1 || choice > options.size()) {
            //Preselect the default value.
            choice = defaultValue;
            //If selectedValue is not null, try to find it.
            if (selectedValue != null) {
                int optCnt = 1;
                for (String option : options) {
                    if (option.equals(selectedValue)) {
                        choice = optCnt;
                    }
                    optCnt++;
                }
            }
            System.out.println(query);
            int optCnt = 1;
            for (String option : options) {
                String selStr = "  ";
                if (optCnt == choice)
                    selStr = " *";
                String numberStr = String.format("[%d]", optCnt);
                System.out.println(String.format("%s%-4s : %s", selStr, numberStr, option));
                optCnt++;
            }

            System.out.print("Please select a value in the list [" + choice + "]: ");
            String value = getString().toString();
            if (value.length() == 0) {
//                choice = defaultValue;              //Choice is already set.
            } else {
                try {
                    choice = Integer.parseInt(value);
                } catch (Exception ex) {

                }
            }
        }
        return choice;
    }

    /**
     * Convenience method for getChoiceAsObject where selectedValue defaults to null
     */
    public <T> T getChoiceAsObject(String query, List<String> options, int defaultValue, List<T> objectsByOption) {
        return getChoiceAsObject(query, options, defaultValue, objectsByOption, null);
    }

    /**
     * Uses getChoice and returns an object identified by the returned value of getChoice
     *
     * @param query
     * @param options
     * @param defaultValue
     * @param selectedValue
     * @return
     */
    public <T> T getChoiceAsObject(String query, List<String> options, int defaultValue, List<T> objectsByOption, String selectedValue) {
        if (objectsByOption.size() != options.size())
            throw new RuntimeException("options must have the same size as objectsByOption");
        int choice = getChoice(query, options, defaultValue, selectedValue);
        return objectsByOption.get(choice - 1);
    }
}