from os import listdir
from os.path import isfile, join
import sys
import subprocess
import time

args = sys.argv

printout = 0

if(len(args) == 4 or len(args) == 5):
	expath = args[1]
	dirpath = args[2]
	sempath = args[3]
	if(len(args) == 5 and args[4] == "--printout"):
		print "test"
		printout = 1
else:
	print "Usage: timing_script.py expath dirpath sempath [--printout]"
	sys.exit(-1)
	
if "dlv" in expath:
	expath = "./" + expath
	filter = "-filter=in"
else:
	filter = "--"
	#TODO give clingo filter file

files = [f for f in sorted(listdir(dirpath)) if isfile(join(dirpath, f))]
files = [f for f in files if ".apx" in f] #filtering out nondesirable files

starttime = time.time()
c = 0

logName = "log-" + str(starttime) + ".log"
logFile = open(logName,"w+")
logFile.write("fileset: " + dirpath + "\n")
logFile.write("semantics file: " + sempath + "\n")
logFile.write("started: " + str(starttime) + "\n")

for f in files:
	fileStart = time.time()
	c = c+1
	cmd = [expath,sempath,dirpath + "/" + f,filter]
	print f + ":" + str(c) + "/" + str(len(files))
	if printout == 0:
		subprocess.call(cmd,stdout=subprocess.PIPE)
	else:
		subprocess.call(cmd)
	
	logFile.write("file: " + f + ", duration: " + str(time.time()-fileStart) + "\n")
	
endtime = time.time()
diff = endtime - starttime

logFile.write("concluded: " + str(endtime) + "\n")
logFile.write("time elapsed: " + str(diff))

print "Wrote results to: " + logName
