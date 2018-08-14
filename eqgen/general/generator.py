import string
import random
import math
from itertools import chain, combinations

def generateAFs(coreSize,afSize,selected):
	afs = []
	count = 0
	argNames = list(string.ascii_lowercase)

	args = argNames[:afSize+coreSize] #now we have the afs arguments
	coreArgs = argNames[:coreSize]
	attacks = generateAttacks(args)
	
	for i in selected:
		presel = str(bin(i/(coreSize+1)))[2:][::-1]
		presel = presel.ljust(len(attacks),'0')
	
		attackSet = []
	
		for j in range(len(attacks)):
			if presel[j] == '1':
				attackSet.append(attacks[j])
		
		rest = i%(coreSize+1)
		if rest == 3:
			afs.append((args,attackSet))
		else:
			afs.append(filterArgs((args,attackSet),coreArgs[rest:]))

	return afs

def filterArgs(af,toFilter):
	newArgs = [a for a in af[0] if a not in toFilter]
	newAtts = [(a,b) for (a,b) in af[1] if (a not in toFilter) and (b not in toFilter)]
	
	return (newArgs,newAtts)

def generateAttacks(coreArgs):
	attacks = []    
    
	for a in coreArgs:
		for b in coreArgs:
			attacks.append((a,b))

	return attacks
