package de.dkfz.roddy.tools

import org.jetbrains.annotations.NotNull


class Utils {

    static @NotNull String stackTrace() {
        StringBuilder sb = new StringBuilder()
        Thread.currentThread().getStackTrace().drop(1).each {
            sb.append(it.toString()).append("\n")
        }
        return sb.toString()
    }

}
