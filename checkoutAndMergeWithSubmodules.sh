rm -rf dist/plugins/DefaultPlugin
rm -rf dist/plugins/PluginBase

(git co $1 && git merge $2)

git submodule foreach git fetch --tags
git submodule update --init --recursive
(cd dist/plugins/DefaultPlugin && git co develop)
(cd dist/plugins/PluginBase && git co develop)

