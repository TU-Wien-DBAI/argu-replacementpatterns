# argu-replacementpatterns
A project on replacement patterns for abstract argumentation

Included are four tools:

Main contributions:

asp:
dlv encodings
using dlv load the basic_rules file, a file containing facts describing the af and the *_rules file where * is the desired semantics name

isosub_replace:
replaces isomorphic subgraphs fitting a provided equivalence pattern

use the IsoSub.class file in the bin/isosub directory

Synopsis: java IsoSub <pattern_path> <data_path> <replacement_path> <stats_path> [-s <int>] [-e <int>]
	pattern_path - path to the pattern file
	data_path - path to either a directory of/or single database graph file(s)
	replacement_path - path to replacement graph file
	stats_path - path to directory where graphs after replacement and a summary result file are stored
	
	-s <int>  Give starting file index within data_path (Standard=0)
	-e <int> Give last file (index+1) to be analysed within data_path (Standard=500)

Helper tools:

eqgen:
generates small AFs according to some criteria - a helper tool for "asp"

Synopsis: python coregenerate.py <#afs_generated> <#core_size> <#outer_size>  <#core_variations> <#connection_variations> --sem NAME [-e|-r] [--mult <int>] [--rest <int>]

	afs_generated - Amount of non-core Argumentation Frameworks to generate.
	core_size - Maximum number of core arguments in each AF.
	outer_size - Amount of arguments, that are not in the core.
	core_variations - Amount of cores that are combined with each set of non-core arguments.
	connection_variations - Amount of different combination of connecting attacks between core and non-core arguments is generated.
	
	-e  Only save problem encoding.
	-r  Only save encoding and result, don't log.
	--sem NAME  Give the semantics name to be checked.
	--mult <int>  There are at most int*#args attacks.
	--rest <int>  Restrict log: int=1 - no equal AFs, int=2 - no A(F1)=A(F2).
	
testing:
computes extensions for a given semantics for a directory of files
then logs computation times to logfile in location of script

Synopsis: python timing_script.py <expath> <dirpath> <sempath> [--printout]

	expath - path to dlv executable or just write clingo to use clingo
	dirpath - path to files to compute extensions for
	sempath - path to file of ASPARTIX semantics encoding
	
	--printout  Set flag if answer sets are to be printed to terminal
