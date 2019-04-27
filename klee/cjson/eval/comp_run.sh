SUBJECT=cJSON
rm -rf valid_inputs.txt *.html *.gcda *.gcno subject *.cov log.txt
gcc ${SUBJECT}.c -o subject -ldl
# gcc -fprofile-arcs -ftest-coverage ${SUBJECT}.c -o subject.cov
python3 eval.py subject > log.txt 2>&1
echo "##############Coverage###########" >> log.txt
python3 coverage.py -p ${SUBJECT}.c -a False >> log.txt 2>&1
