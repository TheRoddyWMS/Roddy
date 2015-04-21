diff -r . ../RoddyStable | grep "diff -r" | grep -v buildversion | grep -v .idea | cut -b 8-200 > differs.txt
