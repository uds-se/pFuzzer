RUNTIME=$1

cd csv
sh run_exp.sh $RUNTIME &
cd ..
cd ini
sh run_exp.sh $RUNTIME &
cd ..
cd cjson
sh run_exp.sh $RUNTIME &
cd ..
cd tinyc
sh run_exp.sh $RUNTIME &
cd ..
cd mjs
sh run_exp.sh $RUNTIME &
cd ..

sleep ${RUNTIME}h

#some safety buffer s.t. all is properly closed and everything had enough time to stop
sleep 15m

cd csv/eval/
sh comp_run.sh csvparser &
cd ../..
cd ini/eval/
sh comp_run.sh ini &
cd ../..
cd cjson/eval/
sh comp_run.sh cJSON &
cd ../..
cd tinyc/eval/
sh comp_run.sh tiny &
cd ../..
cd mjs/eval/
sh comp_run.sh mjs &
cd ../..
