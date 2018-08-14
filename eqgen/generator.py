import random
import math
import string
from itertools import chain, combinations

def generateSelectors(afSize,afAmount,offset):
	#possible afs 2^number of attacks=powerset, number of attacks=number of args^2
	variations = math.pow(2,math.pow(afSize,2))
	selfVars = math.pow(2,afSize) #was afAmount instead of afSize
	stdVars = variations - selfVars
	
	#gets the outer args' names
	args = list(string.ascii_lowercase)[offset:afSize+offset]
	
	#selects afAmount random numbers from numbers 0 - variations-1, xrange faster than range
	selfSel = random.sample(xrange(int(selfVars)), int(min(selfVars, afAmount, stdVars)))
	stdSel = random.sample(xrange(int(stdVars)), int(min(afAmount,stdVars)))
	
	return [selfSel, stdSel, args]

def generateAFs(afSize,selectedSelf,selectedStd,args,isCore,mult):
	afs = []
	count = 0
	
	if(mult is -1):
		mult = 1.5

	attacks = generateAttacks(args,args)
	splitAttacks = separateAttacks(attacks) #[self,std]
	orderedAttacks = splitAttacks[0]+splitAttacks[1]
	
	for i in range(0,len(selectedSelf)): #len(selectedSelf)=len(selectedStd)	
		binself = binarize(selectedSelf[i],len(splitAttacks[0]))
		binself = reduceSelfAttacks(binself,isCore)
		
		binstd = binarize(selectedStd[i],len(splitAttacks[1]))
		binrep = binself+binstd
		
		presel = reduceAttacks(binrep,int(len(args)*mult))

		attackSet = []

		for j in range(len(orderedAttacks)):
			if presel[j] == '1':
				attackSet.append(orderedAttacks[j])
	
		if isCore:
			restArgs = reduceArgs(args,random.randint(0,afSize))
			toAdd = filterArgs((args,attackSet),restArgs)
		else:
			toAdd = (args,attackSet)
	
		afs.append(toAdd)

	return afs

def separateAttacks(attacks):
	slf = []
	std = []
	
	for (a,b) in attacks:
		if a == b:
			slf.append((a,b))
		else:
			std.append((a,b))
	
	return [slf,std]

def generateConnections(outer,inner,conn_amount):
	attacks = generateAttacks(outer[0],inner[0])
	attacks.extend(generateAttacks(inner[0], outer[0]))
	
	conn_variations = math.pow(2,len(inner[0])*len(outer[0])*2)
	selected = random.sample(xrange(int(conn_variations)),int(min(conn_amount,conn_variations)))
	
	connections = []
	
	#print "c: " + str((len(inner[0])*len(outer[0])))
	
	for i in selected:
		presel = reduceAttacks(binarize(i,len(attacks)),int((len(inner[0])*len(outer[0]))/3*2))
		
		attackSet = []
		
		for j in range(len(attacks)):
			if presel[j] == '1':
				attackSet.append(attacks[j])
				
		connections.append(connectParts(outer,inner,attackSet))
		
	return connections

def binarize(inNumber,binLength):
	binary = str(bin(inNumber))[2:][::-1]
	binary = binary.ljust(binLength,'0')
	
	return binary
	
def reduceArgs(inArgs,amount):
	toKeep = [ inArgs[i] for i in random.sample(xrange(len(inArgs)),amount) ]
	return toKeep
	
def reduceAttacks(inNumber,maxOne):
	oneAmount = inNumber.count('1')

	if oneAmount > maxOne:
		inds = [pos for pos, char in enumerate(inNumber) if char == '1']
		rnd = random.randint(0,maxOne)
		toKeep = random.sample(xrange(len(inds)),rnd) #indices in inds
		
		for i in range(len(toKeep)):
			toKeep[i] = inds[toKeep[i]]
		
		nList = list(inNumber)
		for i in range(len(nList)):
			if nList[i] == '1' and not (i in toKeep):
				nList[i] = '0'
		inNumber = "".join(nList)
		
	return inNumber
	
def reduceSelfAttacks(inNumber,isCore):
	if not isCore:
		return inNumber.replace('1','0')

	indices = [i for i, x in enumerate(inNumber) if x == '1']
	choices = []
	
	selfNum = random.randint(0,2) #two self attacks allowed, but not needed
	
	if indices:
		for i in range(selfNum):
			choices.append(random.choice(indices))
	else:
		return inNumber
	
	numberList = list(inNumber)
	
	for i in range(0,len(numberList)):
		if not (i in choices):
			numberList[i] = '0'
	
	return "".join(numberList)

def connectParts(outer,inner,attackSet):
	args = []
	args.extend(outer[0])
	args.extend(inner[0])
	
	atts = []
	atts.extend(outer[1])
	atts.extend(inner[1])
	atts.extend(attackSet)

	return (args,atts)

def filterArgs(af,toKeep):
	newArgs = [a for a in af[0] if a in toKeep]
	newAtts = [(a,b) for (a,b) in af[1] if (a in toKeep) and (b in toKeep)]
	
	return (newArgs,newAtts)

def generateAttacks(argSetA,argSetB): # if A=B, gen attacks, if A!=B, gen connattacks
	attacks = []    
    
	for a in argSetA:
		for b in argSetB:
			attacks.append((a,b))

	return attacks
