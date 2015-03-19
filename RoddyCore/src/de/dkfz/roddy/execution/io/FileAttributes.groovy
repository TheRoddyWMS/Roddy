package de.dkfz.roddy.execution.io

/**
 */
@groovy.transform.CompileStatic
public class FileAttributes {
    public final String userID;
    public final String groupID;

    private boolean userCanRead;
    private boolean userCanWrite;
    private boolean userCanExecute;
    private boolean groupCanRead;
    private boolean groupCanWrite;
    private boolean groupCanExecute;
    private boolean othersCanRead;
    private boolean othersCanWrite;
    private boolean othersCanExecute;


    FileAttributes(String userID, String groupID) {
        this.userID = userID
        this.groupID = groupID
    }

    public void setPermissions(boolean userCanRead, boolean userCanWrite, boolean userCanExecute, boolean groupCanRead, boolean groupCanWrite, boolean groupCanExecute, boolean othersCanRead, boolean othersCanWrite, boolean othersCanExecute) {
        this.userCanRead = userCanRead
        this.userCanWrite = userCanWrite
        this.userCanExecute = userCanExecute
        this.groupCanRead = groupCanRead
        this.groupCanWrite = groupCanWrite
        this.groupCanExecute = groupCanExecute
        this.othersCanRead = othersCanRead
        this.othersCanWrite = othersCanWrite
        this.othersCanExecute = othersCanExecute
    }

    boolean getUserCanRead() {
        return userCanRead
    }

    boolean getUserCanWrite() {
        return userCanWrite
    }

    boolean getUserCanExecute() {
        return userCanExecute
    }

    boolean getGroupCanRead() {
        return groupCanRead
    }

    boolean getGroupCanWrite() {
        return groupCanWrite
    }

    boolean getGroupCanExecute() {
        return groupCanExecute
    }

    boolean getOthersCanRead() {
        return othersCanRead
    }

    boolean getOthersCanWrite() {
        return othersCanWrite
    }

    boolean getOthersCanExecute() {
        return othersCanExecute
    }
}
