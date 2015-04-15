/**
 * Created by michael on 05.02.15.
 *
 * Adds a version tag to the changelist section of a readme file
 *
 */

import java.nio.file.Files
import java.nio.file.Paths

def readmePath = Paths.get(args[0])
def readmeCopyPath = Paths.get(args[0] + "~")
def buildfilePath = Paths.get(args[1])

def lines = readmePath.readLines()
def versionEntries = buildfilePath.readLines()
def version = versionEntries.join(".")
int indexOfChangelist = 0;
for (int i = 0; i < lines.size(); i++) {
    String line = lines[i];
    if(line.trim().startsWith("== Changelist")) {
        indexOfChangelist = i;
        break;
    }
}
indexOfChangelist;
//println(indexOfChangelist)
//println(version)

lines.add(indexOfChangelist + 1, "")
lines.add(indexOfChangelist + 2, "* Version update to " + version)

if(Files.exists(readmeCopyPath)) Files.delete(readmeCopyPath)
Files.copy(readmePath, readmeCopyPath);
new File(args[0]).write(lines.join("\n"));