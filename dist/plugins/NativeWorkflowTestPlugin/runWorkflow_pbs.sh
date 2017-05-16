source ${CONFIG_FILE}


resa=`qsub -N testScript_calla -l mem=50m -l walltime=00:00:10 testScriptSleep.sh`

