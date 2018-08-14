import math, random

#option 1, generate, then filter

selected = random.sample(xrange(1000),20)
binaries = []

for i in selected:
	binary = str(bin(i))[2:][::-1]
	binary = binary.ljust(10,'0')
		
	binaries.append(binary)

filtered = []

for b in binaries:
	oneAmount = b.count('1')
	filt = '0000000000'
	
	if oneAmount > 5:
		inds = [pos for pos, char in enumerate(b) if char == '1']
		rnd = random.randint(1,5)
		toKeep = random.sample(xrange(len(inds)),rnd)
		for i in toKeep:
			filt = (filt[:i] + '1' + filt[i+1:])
	else:
		filt = b
	
	filtered.append(filt)
	
print filtered
