// Update build version and date in Constants.java

if(args.length < 2) {
	println "Please call this script: groovy IncreaseAndSetBuildVersion [version info file] [constants file]"
	System.exit(1);
}

def vFile = new File(args[0])
def srcFile = new File(args[1]);
def lines = vFile.readLines()
def major = lines[0];
def minor = lines[1].toInteger() + 1;
vFile.withWriter {
    out ->
        out.writeLine("" + major);
        out.writeLine("" + minor);
}

// Open String constants in src path and change line with version constant.
def srcLines = srcFile.readLines();
def foundVString = false;
def foundBuildDate = false;
srcFile.withWriter {
    out ->
        for(int i=0;i<srcLines.size();i++) {
            String line = srcLines[i];
            if(line.contains("CURRENT_VERSION_STRING") && !foundVString) {
                line = line.split("[=]")[0] + "= \"${major}.${minor}\";"
                foundVString = true;
            } else if(line.contains("CURRENT_VERSION_BUILD_DATE") && !foundBuildDate) {
                line = line.split("[=]")[0] + "= \"${new Date()}\";"
                foundBuildDate = true;
            }
            out.writeLine(line);
        }
}
