import sys
import math
import generator
import converter
import random
import cmdbuilder
import argparse
from subprocess import call

error = "Synopsis: python coregenerate.py <#afs_generated> <#core_size> <#outer_size>  <#coreVariations> <#connectionVariations> --sem NAME [-e|-r]\n"
error = error + "Options:\n  -e  Only save problem encoding.\n  -r  Only save encoding and result, don't log."
done = "Done!"

parser = argparse.ArgumentParser("Equivalence checking for randomly generated Argumentation Frameworks.")
parser.add_argument('afAmount', metavar='A', type=int, nargs=1, help="Amount of non-core Argumentation Frameworks to generate.")
parser.add_argument('coreSize', metavar='C', type=int, nargs=1, help="Maximum number of core arguments in each AF.")
parser.add_argument('afSize', metavar='O', type=int, nargs=1, help="Amount of arguments, that are not in the core.")
parser.add_argument('coreVars', metavar='R', type=int, nargs=1, help="Amount of cores that are combined with each set of non-core arguments.")
parser.add_argument('connVars', metavar='N', type=int, nargs=1, help="Amount of different combination of connecting attacks between core and non-core arguments is generated.")
parser.add_argument('--sem', dest='dlv', required=True, type=str, nargs=1, help="Semantic that should be checked.")
parser.add_argument('--mult', dest='mult', required=False, type=float, nargs=1, help="There are at most mult*#args attacks.")
parser.add_argument('--rest', dest='rest', required=False, type=int, nargs=1, help="Restrict log: 1 - no equal AFs, 2 - no A(F1)=A(F2).")
group = parser.add_mutually_exclusive_group(required=False)
group.add_argument('-e', action="store_true", help="AFs should merely be generated, but not checked.")
group.add_argument('-r', action="store_true", help="AFs are checked but not logged.")

parsed = vars(parser.parse_args())

afAmount = parsed['afAmount'][0]
coreSize = parsed['coreSize'][0]
afSize = parsed['afSize'][0]
coreVars = parsed['coreVars'][0]
connVars = parsed['connVars'][0]
dlv = parsed['dlv'][0]
enc = parsed['e']
res = parsed['r']
mult = -1
rest = 0

if(parsed['mult']):
	mult = parsed['mult'][0]
	
if(parsed['rest']):
	rest = parsed['rest'][0]
		
print "Generating AFs. Core size: " + str(coreSize) + ", outer args: " + str(afSize) + "."

selection = generator.generateSelectors(afSize,afAmount,0)

#generator generates all options for outer afs
outer = generator.generateAFs(afSize, selection[0], selection[1], selection[2], False, mult)	
	
print "Generated " + str(len(outer)) + " outer AFs."

selection = generator.generateSelectors(coreSize,coreVars,afSize)

#generator generates all options for outer afs #was coreVars instead of coreSize
inner = generator.generateAFs(coreSize, selection[0], selection[1], selection[2], True, mult)

print "Generated " + str(len(inner)) + " core AFs."

complete = []

for o in outer:
	for i in inner:
		complete.extend(generator.generateConnections(o,i,connVars))

if len(outer) == 0:
	complete = inner

print "Connected configurations into " + str(len(complete)) + " AFs."

output = converter.convertList(coreSize,afSize,complete)

path = "./" #TODO change if folder structure is changed
enc_name = cmdbuilder.getNewName(path,"encoding","")
res_name = cmdbuilder.getNewName(path,"result","")
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
	log = converter.convertResultToLog(lines,afSize,coreSize,output,len(complete),enc_name,rest)
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
