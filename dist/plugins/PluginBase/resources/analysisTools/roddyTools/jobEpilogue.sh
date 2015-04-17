#!/bin/bash

# This script is called after the execution of a (cluster) job. It tries to get the final status of a job (error, finished...)
# and writes this to a logfile in the execution directory.

# Call the post epilogue script in a separate process so the other script can figure out the state of this process!
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "Epilogue working in $DIR"
ssh $2@tbi-pbs1 "sh ${DIR}/jobPostEpilogueScript.sh $1 $2" &