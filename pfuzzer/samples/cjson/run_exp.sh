RUNTIME=$1
SUBJECT=$2
CURRENTDIR=$(pwd)
sh clean.sh
../../install/bin/trace-instr $(pwd)/${SUBJECT}.c ../excluded_functions > log.txt 2>&1
cd ../../chains
time timeout -k9 ${RUNTIME}h python3 chains.py -p ${CURRENTDIR}/${SUBJECT}.c -a False -i False 1> /dev/null 2> error.log &
sleep ${RUNTIME}h
sleep 15m
cd $CURRENTDIR
sh get_cov.sh ${SUBJECT} ${CURRENTDIR}
