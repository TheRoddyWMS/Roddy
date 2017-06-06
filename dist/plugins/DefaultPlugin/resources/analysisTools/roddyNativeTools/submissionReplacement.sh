#!/usr/bin/env bash


export PIPED_SCRIPT=""
set -xuv
lockfile $tmpFileCnt~

cur=`head -n 1 $tmpFileCnt`
cur=`expr $cur + 1`
final=100000$cur
echo $cur > $tmpFileCnt

echo "$final, qsub $*" >> $tmpFileCall

rm -rf $tmpFileCnt~

# useScriptFile needs to be set outside
if [[ $useScriptFile == true ]]; then
  echo "$final"
else
  echo "$final"

  mkdir -p $(dirname ${scriptFilePrefix})
  result=${PIPED_SCRIPT}
  scriptFile=${scriptFilePrefix}${final}
  printf $result > $scriptFile

  cat /dev/stdin | while read line
  do
    printf $"${PIPED_SCRIPT}${line}\n" >> $scriptFile
  done
fi
