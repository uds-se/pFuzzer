SUBJECT=$1
rm *.html *.gcda *.gcno *.txt subject subject.cov
gcc ${SUBJECT}.c -o subject -ldl
gcc -fprofile-arcs -ftest-coverage ${SUBJECT}.c -o subject.cov -ldl
python3 eval.py subject > log.txt
