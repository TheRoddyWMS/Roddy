<!-- detailed description of the changes and possible context -->

## Check

 * [ ] Stakeholders agree on the validity of the changes, or are not affected by the change
 * [ ] The merge request title concisely describes the changes
 * [ ] Changes are described in the ChangeLog and classified according to [semantic versioning](#SemanticVersioning) into "major", "minor", "patch" levels.
 * [ ] The correct target branch is selected for merging :sweat_smile:
 * [ ] If a release is planned, Roddy's release procedure was followed 
     1. [ ] `buildinfo.txt` was modified to release number
     2. [ ] `INCREASE_BUILD_VERSION=false roddy.sh compile` was run to update `RoddyCore/src/de/dkfz/roddy/Constants.java`
     3. [ ] Both files were checked in after modification
