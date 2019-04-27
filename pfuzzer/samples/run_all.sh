RUNTIME=$1

cd cjson/
sh run_exp.sh $RUNTIME cJSON &
cd ../ini
sh run_exp.sh $RUNTIME ini &
cd ../tinyc
sh run_exp.sh $RUNTIME tiny &
cd ../csv
sh run_exp.sh $RUNTIME csvparser &
cd ../mjs
sh run_exp.sh $RUNTIME mjs &
cd ..
