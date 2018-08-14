import sys
import math
import generator
import converter
import random
import cmdbuilder
from subprocess import call

error = "Synopsis: python coregenerate.py <#core_args> <max_#other_args> <#afs_generated> <semantic_name> [-e|-r]\n"
error = error + "Options:\n  -e  Only save problem encoding.\n  -r  Only save encoding and result, don't log."
done = "Done!"

if len(sys.argv) > 6 or len(sys.argv) < 5 :
		print error
		sys.exit()

coreAmount = sys.argv[1] #this is the number of arguments in the core
afSize = sys.argv[2]
afAmount = sys.argv[3]
dlv = sys.argv[4] #which semantics we want to use (stb, adm, prf, com, grd)
enc = False
res = False

if len(sys.argv) == 6:
	if sys.argv[5] == "-e":
		enc = True
	elif sys.argv[5] == "-r":
		res = True

try:
    coreAmount = int(coreAmount)
    afSize = int(afSize)
    afAmount = int(afAmount)
except ValueError:
		print error
		sys.exit()
		
print "Generating AFs. Core size: " + str(coreAmount) + ", outer args: " + str(afSize) + "."

#possible afs 2^number of attacks=powerset, number of attacks=number of args^2, *coreAmount+1 --> filter towards smaller core set
variations = math.pow(2,math.pow(coreAmount+afSize,2))*(coreAmount+1)

#selects afAmount random numbers from numbers 0 - variations-1, xrange faster than range
selected = random.sample(xrange(int(variations)), int(min(afAmount,variations)))

#generator generates all options
complete = generator.generateAFs(coreAmount, afSize, selected)
			
print "Generated " + str(len(complete)) + " AFs."

output = converter.convertList(coreAmount,afSize,selected,complete)

path = "./" #TODO change if folder structure is changed
enc_name = cmdbuilder.getNewName(path,"encoding","")
res_name = "result"
err_name = "error"

asp_encoding = open(path + enc_name, "w")
asp_encoding.write(output)
asp_encoding.close()

print "Converted AFs to solver readable form. Saved " + enc_name + " file."

if enc:
	print done
	sys.exit()

result = open(path + res_name, "w")

#build equivalency check shell command for specified semantics
try:
	shellArgs = ["../dlv", enc_name, cmdbuilder.getPath("basic")]
	shellArgs.extend(cmdbuilder.selectRecources(dlv))
	shellArgs.extend(["-nofinitecheck", "-pfilter=" + dlv + "Equiv"])
except ValueError as semError:
	print error
	print semError
	sys.exit()

#channel shell results into variable
try:
	call(shellArgs, stdout=result)
	result.close()
except ValueError as cmdError:
	print error
	print cmdError
	sys.exit()
	
print "Computed results, saved them in " + res_name + " file."

if res:
	print done
	sys.exit()

#convert output
lines = open(path + res_name,"r").readlines()
try:
	log = converter.convertResultToLog(lines,coreAmount,output,len(selected),enc_name)
except ValueError as logError:
	print "Log could not be created: " + repr(logError) + " No equivalent AFs have been found."
	sys.exit()

#save in .csv file
log_name = cmdbuilder.getNewName(path,"log",".csv")
log_file = open(path + log_name, "w")
log_file.write(log)
log_file.close()

print "Converted results to .csv format, saved them in " + log_name + " file."
print done
