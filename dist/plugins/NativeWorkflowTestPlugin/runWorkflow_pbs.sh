source ${CONFIG_FILE}

set -xuv
first=`qsub -N testScript_calla -l mem=50m -l walltime=00:00:10 testScriptSleep.sh`



piped=`echo "sleep 5" | qsub -N testScript_callb -l mem=20m -l walltime=00:00:10 -W depends=afterok:${first}`
