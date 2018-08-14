import os

asp_path = "../asp/" #TODO change if you change folder structure!

def getPath(semantic):
	global asp_path
	path = asp_path + semantic + "_rules"
	return path
	
def selectRecources(semantic):
	if semantic == "stb":
		return [getPath("stb")]
	elif semantic == "adm":
		return [getPath("adm")]
	elif semantic == "prf":
		return [getPath("adm"), getPath("prf")]
	elif semantic == "com":
		return [getPath("com")]
	elif semantic == "grd":
		return [getPath("com"), getPath("grd")]
	else:
		raise ValueError("No valid semantic given! Possible semantics are: stb, adm, prf, com, grd")
		
def getNewName(path,baseName,fileFormat):
	fileList = os.listdir(path)
	
	fileList = [f.replace(fileFormat,"").replace(baseName,"") for f in fileList if baseName in f]
	
	high = 1
	
	while (str(high) in fileList):
			high = high + 1
	
	return baseName + str(high) + fileFormat
