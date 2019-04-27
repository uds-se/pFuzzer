cd klee/cjson/eval
sh comp_run.sh &
cd ../../ini/eval
sh comp_run.sh &
cd ../../tinyc/eval
sh comp_run.sh &
cd ../../csv/eval
sh comp_run.sh &
cd ../../mjs/eval
sh comp_run.sh &
cd ../../../

cd afl/cjson/eval
sh comp_run.sh cJSON &
cd ../../ini/eval
sh comp_run.sh ini &
cd ../../tinyc/eval
sh comp_run.sh tiny &
cd ../../csv/eval
sh comp_run.sh csvparser &
cd ../../mjs/eval
sh comp_run.sh mjs &
cd ../../..

cd pfuzzer/chains/
python3 coverage.py -p ../samples/cjson/cJSON.c -a false > ../samples/cjson/evallog.txt 2>&1 
python3 coverage.py -p ../samples/ini/ini.c -a false > ../samples/ini/evallog.txt 2>&1 
python3 coverage.py -p ../samples/csv/csvparser.c -a false > ../samples/csv/evallog.txt 2>&1 
python3 coverage.py -p ../samples/tinyc/tiny.c -a false > ../samples/tinyc/evallog.txt 2>&1 
python3 coverage.py -p ../samples/mjs/mjs.c -a false > ../samples/mjs/evallog.txt 2>&1 
