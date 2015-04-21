#!/bin/bash
set -xv

cd dist/

rm "dist/lib/groovy-all*.jar"
rm -rf "runtime"
rm -rf "runtimeDevel"

basepath="runtime/"
javaArchive=`ls jre*`
groovyArchive=`ls groovy-binary*`

if [ "$1" == "devel" ] 
then
	basepath="runtimeDevel/"
	javaArchive=`ls jdk*`
	groovyArchive=`ls groovy-sdk*`
fi

mkdir $basepath

unzip $groovyArchive -d $basepath
tar xvfz $javaArchive -C $basepath 

groovyJar=`find -name "groovy-all*.jar" | grep -v "indy"`
cp $groovyJar "lib/groovy-all.jar"

runtimePath=${PWD}/${basepath}

if [ "$1" == "devel" ]
then	
	ln -s ${runtimePath}jdk*/jre ${runtimePath}jre
	ln -s ${runtimePath}jdk* ${runtimePath}jdk
	ln -s ${runtimePath}groovy* ${runtimePath}groovy
	exit 0
fi

ln -s ${runtimePath}jre* ${runtimePath}jre
ln -s ${runtimePath}groovy* ${runtimePath}groovy

