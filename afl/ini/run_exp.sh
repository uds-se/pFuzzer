rm -rf findings/
../../programs/afl-2.52b/afl-gcc ini.c -o ini
mkdir findings
export AFL_SKIP_CPUFREQ=True
time timeout -k9 ${1}h ../../programs/afl-2.52b/afl-fuzz -i tests -o findings ./ini > log.txt 2>&1
