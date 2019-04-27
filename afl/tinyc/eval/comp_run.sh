SUBJECT=$1
rm *.html *.gcda *.gcno *.txt subject subject.cov
gcc ${SUBJECT}.c -o subject
gcc -fprofile-arcs -ftest-coverage ${SUBJECT}.c -o subject.cov
python3 eval.py subject > log.txt
