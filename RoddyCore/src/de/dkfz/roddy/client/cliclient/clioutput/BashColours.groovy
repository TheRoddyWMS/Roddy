package de.dkfz.roddy.client.cliclient.clioutput;

/**
 * A bash colors table with conversion methods.
 */
public enum BashColours {
    RED("31m"),
    GREEN("32m"),
    YELLOW("33m"),
    BLUE("34m"),
    PURPLE("35m"),
    CYAN("36m"),
    WHITE("37m"),

    BGBLACK("40m"),
    BGRED("41m"),
    BGGREEN("42m"),
    BGYELLOW("43m"),
    BGBLUE("44m"),
    BGPURPLE("45m"),
    BGCYAN("46m"),
    BGGRAY("47m"),
    //Fat fonts
    FRED("1;31m"),
    FGREEN("1;32m"),
    FYELLOW("1;33m"),
    FBLUE("1;34m"),
    FPURPLE("1;35m"),
    FCYAN("1;36m"),
    FWHITE("1;37m"),
    CLEAR("0m");

    public final String colourString;
    public final String colourFormatString;
    public final String colourFinalString;

    BashColours(String colourString) {
        this.colourString = colourString
        this.colourFormatString = "#" + name() + "#";
        this.colourFinalString = "\\033[" + colourString;
    }

    public String format(String str) {
        return str.replace(colourFormatString, colourFinalString);
    }
}
