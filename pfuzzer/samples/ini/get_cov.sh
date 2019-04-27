SUBJECT=$1
CURRENTDIR=$2
cd ../../chains
python3 coverage.py -p ${CURRENTDIR}/${SUBJECT}.c -a False >> log.txt 2>&1