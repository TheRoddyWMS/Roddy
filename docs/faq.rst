Frequently Asked Questions
==========================

Indicated packet length ... too large
-------------------------------------

The SSH library we currently use (sshj) does not work if during your login on the submission host (on which the e.g. qsub command is executed). Make
sure during the login no output is generated, e.g. from your `$HOME/.profile`, `$HOME/.bashrc`, etc. files.

When something wents wrong and Roddy returns an error code
----------------------------------------------------------

Roddy can exit with a range of exit codes which are:

.. csv-table:: "Title"
    :Header: "Exit code", "Description"
    :Widths: 5, 20

    "255", "You must call roddy from the right location! This is the folder where roddy.sh resides."
    "254", "SystemExitException throw by GroovyServ. Only occurs when GroovyServ is enabled (not by default!)."
    "253", "Command line was malformed, check your input and correct mistakes."
    "252", "Execution requirements unfulfilled. Seems you are missing some applications, please install them or ask your administrator to do it for you."
    "251", "Startup options could not be parsed. Check your command line."
    "250", "Cannot find requested feature toggle file. Please make sure, that the required file exists."
    "249", "Feature toggle is not known. There are only some available. If you are unsure about this, don't use them, if not necessary."
    "248", "Unknown problem with proxy setup. Check your proxy settings."
    "247", "scratchBaseDir is not defined in your application ini file. Please set it so that it matches your cluster settings."
    "246", "The wrong job manager class is set in your application ini file. Please correct that."
    "245", "Application properties file not found or loadable. Make sure, that the file exists."
    "244", "Could not load the requested analysis. Look for typos or take another one."
    "243", "Severe configuration errors occurred. Take a detailed look into the error messages and your configuration file,"
    "242", "Unhandled exception. That is a bad one. Take a look at any error message and get in contact with us."
    "241", "Unknown SSH host. Change the hostname, it is possibly wrong."
    "240", "SSH setup is not valid. Please follow the instructions and check your application ini file."
    "239", "Fatal error during SSH setup. Please contact us in this case."
    "100", "Someone uses a wrong exit code somewhere in Roddy. Exit codes should be in class ExitReasons (if possible) and must be in the range [1;255]."

In any case, we try to provide you a good explanation about what happened wrong and how you can solve it. If you find
the messages hard to understand, contact us.
