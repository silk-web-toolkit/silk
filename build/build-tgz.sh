#!/bin/sh
#
# build-tgz.sh Builds the silk-web-toolkit tgz binary package.
# Used by the custom OSX tgz based install script.
# Must be run from the current directory as paths are relative.
#
# Ross McDonald <ross@bheap.co.uk>
#

if [ $1 != "" ]
then
    # Assumes that lein uberjar has been run and uberjar exists in target directory
    mkdir -p ../target/tgz
    cp ../target/*standalone* ../target/tgz/silk.jar
    cp tgz/silk ../target/tgz
    cd ../target/tgz
    tar cvzf silk-$1.tgz * -C ../target/tgz
else
    echo "ERROR: VERSION variable not defined"
fi
