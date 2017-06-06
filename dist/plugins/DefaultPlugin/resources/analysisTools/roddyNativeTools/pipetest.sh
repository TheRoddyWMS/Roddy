#!/usr/bin/env bash

set -xuv
echo $#

exit 0

set -xuv
export pipedIn=false
export _flock=/tmp/lockfileForPipetest
export _flockOuter=/tmp/lockfileForPipetestOuter
export locked=false
export tempScript=/tmp/tempScript_$$

rm -f $_flock
rm -f $tempScript

lockfile $_flockOuter
$(set -xuv; locked=false; rm -f $_flockOuter
while read line
do
  [[ $locked == false ]] && lockfile $_flock && locked=true
  echo $"${PIPED_SCRIPT-}${line}\n" >> $tempScript
done < "${1:-/dev/stdin}" ; rm -f $_flock;) & proc=$!

lockfile $_flockOuter
sleep 0.25
lockfile $_flock
[[ ! -e $tempScript ]] && kill $proc 2> /dev/null
rm -f $_flock

cat $tempScript
rm $tempScript
rm -f $_flockOuter