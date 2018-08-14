import string

def convertDomain(amount):
	domainString = ""

	for arg in list(string.ascii_lowercase)[:amount]:
		domainString += "domain(" + arg + ").\n"

	return domainString

def convertCore(amount):
	coreString = "core(["

	for arg in list(string.ascii_lowercase)[:amount]:
		coreString += arg + ","

	coreString = coreString[:-1] + "])."

	return coreString

def convertArguments(argString):
	argumentsString = "["

	for arg in argString:
		argumentsString += arg + ","

	if (len(argString) > 0):
		argumentsString = argumentsString[:-1]
		
	argumentsString += "]"

	return argumentsString

def convertFrameworkStrings(argumentsString,number):
	frameworkStrings = "arguments(af" + str(number) + "," + argumentsString + ")."

	return frameworkStrings

def convertAttackSets(attSet,number):
	attacksString = "attacks(af" + str(number) + ",[" + convertAttacks(attSet) + "])."

	return attacksString

def convertAttacks(attSet):
	retSet = ""

	if len(attSet) == 0:
		return "[]"

	for att in attSet:
		retSet += "[" + att[0] + "," + att[1] + "],"

	retSet = retSet[:-1]

	return retSet
	
def convertList(coreAmount,afSize,selection,complete):
	domainStrings = convertDomain(coreAmount+afSize)
	coreString = convertCore(coreAmount)

	afStrings = []

	for i in range(len(complete)):
		argumentString = convertArguments(complete[i][0])
		frameworkString = convertFrameworkStrings(argumentString,i)
		attackString = convertAttackSets(complete[i][1],i)
	
		afStrings.append(frameworkString + "\n" + attackString)
	
	output = domainStrings + coreString + "\n"

	for a in afStrings:
		output = output + a + "\n"
		
	return output
	
def convertResultToLog(stringList,coreAmount,facts,afno,enc):
	if len(stringList) != 3:
		raise ValueError ("Invalid amount of answer sets!")
	
	log = ""
	
	results = stringList[2] #this is the one and only answer set (filtered)
	results = results[1:len(results)-2] #removes {,}
	factName = results[:results.find('(')] #gets name of filtered fact
	results = results.replace(factName,'') #cuts occurences of fact name from string
	
	log = factName + ":\nproblem instance:;" + enc
	log = log + "\ncore:;" + convertCore(coreAmount)[6:-3].replace(',','') + "\n"
	log = log + "af1;arguments1;attacks1;af2;arguments2;attacks2\n"
	
	factList = results[1:-1].split('), (')
	
	#remove here unused data from fact encoding
	encoding = removeUnusedData(facts)
	
	#saves all the arg/att data for af only once, doesn't need to be searched again
	dataList = buildDataList(encoding,afno)
	
	#convert af args and attacks into .csv readable form
	for fact in factList:
		delim = fact.index(',')
		af1 = fact[:delim]
		af1no = int(af1[2:])
		af2 = fact[delim+1:]
		af2no = int(af2[2:])
		
		#for efficiency only ever convert an af once, then reuse if needed more often

		log = log + dataList[af1no] + ";"
		
		log = log + dataList[af2no] + "\n"
			
	return log
	
def removeUnusedData(encoding):
	enclist = encoding.split(".\n")
	
	encoding = [ x for x in enclist if not("domain" in x) and not("core" in x) ]
	
	return encoding

def buildDataList(facts,afno):
	dataList = []

	for i in range(0,afno):
		dataList.append("af"+str(i)+";"+cutData(facts,"af"+str(i)))
		
	return dataList

def cutData(encoding,af):
	indices = [i for i, elem in enumerate(encoding) if af+"," in elem]
	afenc = []
	
	for i in indices:
		afenc.append(encoding[i])
		
	del encoding[indices[0]:indices[1]+1]
	
	for line in afenc:
		if "arguments" in line:
			offset = len(af) + 11
			args = line[offset+1:]
			args = args[:-2].replace(",","")
		elif "attacks" in line:
			offset = len(af) + 9
			atts = line[offset+1:]
			atts = atts[:-2].replace(",","")
			
	return args + ";" + atts
	
