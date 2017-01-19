# Unstage several files which might confuse git and which don't neccessarily need to be added to the repo everytime.

git reset HEAD RoddyCore/src/de/dkfz/roddy/Constants.java RoddyCore/buildversion.txt dist/bin/current/Roddy.jar
git checkout -- RoddyCore/src/de/dkfz/roddy/Constants.java RoddyCore/buildversion.txt dist/bin/current/Roddy.jar

