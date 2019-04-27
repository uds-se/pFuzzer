import os
import sys
import subprocess
from io import BytesIO
from random import shuffle

max_exec = 1000000
PUT = sys.argv[1]

counter = 0
files = list()
for f in os.listdir("../klee-out-0"):
    if str(f).endswith(".ktest"):
        starttime = os.path.getctime(os.path.join("../klee-out-0", f))
        files.append((starttime, f))
files = sorted(files, key=lambda x: x[0])
starttime = files[0][0]
endtime = 0
current_out = ""
shuffle(files)
with open("valid_inputs.txt", "w") as output_file:
    for f in files:
        file = f[1]
        # print(f)
        if not str(file).endswith(".ktest"):
            continue
        print("\nFile %d of %d\n" % (counter, len(files)))
        counter += 1
        print("%s\n" % str(file))
        command = "ktest-tool --trim-zeros ../klee-out-0/" + file
        klee_out = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE).stdout.read()
        # print(klee_out)
        try:
            klee_out = klee_out.decode("utf-8")
            if "0: data: \'" in klee_out:
                klee_out = klee_out.split("data: \'")[1].split("\'\nobject")[0]
            elif "0: data: \"" in klee_out:
                klee_out = klee_out.split("data: \"")[1].split("\"\nobject")[0]
            else:
                raise ValueError("Cannot be parsed: %s" % str(f))
        except Exception as e:
            # sometimes the file is empty, then no test was created? we will ignore those cases
            print(e)
            continue
        # print(klee_out)
        input = bytes(klee_out.split("\\x00")[0], "ascii").decode("unicode-escape")
        print(repr(input))
        # input = input.replace("\n", "").replace("\\\"", "\"").replace("\"", "\\\"")
        ps = subprocess.Popen(('printf', '%s', input), stdout=subprocess.PIPE)
        try:
            output = subprocess.call(('./%s' % PUT), timeout=100, stdin=ps.stdout)
        except subprocess.TimeoutExpired:
            print("Timed out.")
            output = 0
        if output == 0:
            gentime = f[0] - starttime
            output_file.write("Generation Time: %d\n" % gentime)
            output_file.write(str(file)+"\n")
            output_file.write(repr(input) + "\n\n")

        if counter == max_exec:
            break
print(current_out)
print(endtime - starttime)
