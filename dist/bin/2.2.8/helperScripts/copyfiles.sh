#set -xuv
lines=`cat differs.txt | wc -l`
lines=`expr $lines`
for i in `seq 1 $lines`
do
	line=`tail -n +${i} differs.txt | head -n 1`
	cp $line
done
