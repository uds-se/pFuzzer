RUNTIME=$1
SUBJECT=tiny
rm -rf klee-*
clang -c -g -emit-llvm ${SUBJECT}.c -o ${SUBJECT}.bc
time timeout -k2 ${RUNTIME}h klee --optimize --only-output-states-covering-new --libc=uclibc  -posix-runtime ${SUBJECT}.bc -sym-stdin 200 > log.txt 2>&1 &
