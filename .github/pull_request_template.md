<!-- detailed description of the changes and possible context -->

## Check

 * [ ] Stakeholders agree on the validity of the changes, or are not affected by the change
 * [ ] The merge request title concisely describes the changes
 * [ ] Changes are described in the ChangeLog and classified according to [semantic versioning](#SemanticVersioning) into "major", "minor", "patch" levels.
 * [ ] The correct target branch is selected for merging :sweat_smile:
 
This is a pre-release merge request, therefore

 * [ ] All changes planned to be included in the release are merged into the release branch.
 * [ ] All changes since the last version are documented in the change-log. Specifically
   * [ ] Changes included in the release are described.
   * [ ] Deprecation is justified.
   * [ ] The resulting new version number is shown.
 * [ ] Stakeholders agree on the validity of the changes, or are not affected by the change
 * [ ] The version number has been adapted in the code
    1. [ ] `buildinfo.txt` was modified to release number
    2. [ ] `INCREASE_BUILD_VERSION=false roddy.sh compile` was run to update `RoddyCore/src/de/dkfz/roddy/Constants.java`
    3. [ ] Both files were checked in after modification
