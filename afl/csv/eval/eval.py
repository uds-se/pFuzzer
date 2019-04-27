import os
import sys
import subprocess
from io import BytesIO
from random import shuffle
import metric

max_exec = 1000000000
PUT = "subject"
METRIC_PUT = "csvparser.c"

counter = 0
files = list()
filesfolders = ["../findings/queue/", "../findings/hangs/"]
for filesfolder in filesfolders:
    for f in os.listdir(filesfolder):
        if f.startswith("id:"):
            starttime = os.path.getctime(os.path.join(filesfolder, f))
            files.append((starttime, os.path.join(filesfolder, f)))
files = sorted(files, key=lambda x: x[0])
starttime = files[0][0]
endtime = 0
current_out = ""
shuffle(files)
valid_files = set()
valid_inputs = list()
with open("valid_inputs.txt", "w") as output_file:
    for f in files:
        file = f[1]
        # print(f)
        print("File %d of %d" % (counter, len(files)))
        counter += 1
        print(str(file))
        input = ""
        with open(file, encoding="latin-1") as inputfile:
            try:
                input = inputfile.read()
            except UnicodeDecodeError:
                print("No valid Unicode in file %s" % file)
                input = "File has no valid unicode encoding: %s" % file
        print(repr(input))
        # print("Before Cleaning: %s" % repr(input))
        # input = input.replace("\0", "")
        # print("After cleaning: %s" % repr(input))
        try:
            ps = subprocess.Popen(('cat', str(file)), stdout=subprocess.PIPE)
            output = subprocess.call('./%s' % PUT, timeout=10, stdin=ps.stdout)
        except subprocess.TimeoutExpired:
            print("Timed out.")
            output = 0
        if output == 0:
            valid_inputs.append(input)
            gentime = f[0] - starttime
            output_file.write("Generation Time: %d\n" % gentime)
            output_file.write("File: %s\n" % str(file))
            output_file.write(repr(input) + "\n\n")
            valid_files.add(file)

        if counter == max_exec:
            break

cov_info = None
valid_cov_increase_counter = 0
for file in valid_files:
    try:
        ps = subprocess.Popen(('cat', str(file)), stdout=subprocess.PIPE)
        output = subprocess.call('./%s.cov' % PUT, timeout=10, stdin=ps.stdout)
    except subprocess.TimeoutExpired:
        print("Timed out.")
        output = 0
    tmp_cov_info = subprocess.check_output(["gcov", "-c", "-b", METRIC_PUT[:-2]], encoding="utf-8")
    tmp_cov_info = "\n" + "".join([line for line in tmp_cov_info.split("\n") if line.startswith("Taken")])
    if tmp_cov_info != cov_info:
        valid_cov_increase_counter += 1
        cov_info = tmp_cov_info
print("\nValid inputs that increase coverage: %d\n" % valid_cov_increase_counter)

os.system("gcovr -r . --branches --html --html-details -o coverage.html")
for key, val in metric.count_regex(METRIC_PUT, valid_inputs).items():
    if val > 0:
        print("\n\t%s: %s" % (key, val))

# print(current_out)
# print(endtime - starttime)
