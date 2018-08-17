package isosub;

import java.util.ArrayList;
import java.util.HashMap;

import bbgraph.BBMatch;
import bbgraph.MatchInfo;
import graphinfo.GraphInfo;
import parser.AFParser;
import replace.ReplacementCollector;
import replace.Replacer;

public class IsoSub {

	static String patternPath, inputPath, replacementPath, logPath;
	static Integer startFileIndex, endFileIndex;

	public static void main(String args[]){

		/* copy example as argument string:
		 * "src/patterns/stbpt" "C:/Users/Patrick/Desktop/DI/instances/StableGen" "src/patterns/stbrep" "C:/Users/Patrick/Desktop/DI/stats" -s 250 -e 500
		 */

		if(args.length >= 4){
			parseCommandLine(args);
		}

		//paths if no arguments present, for testing mostly
		if(patternPath == null || inputPath == null || replacementPath == null || logPath == null){
			patternPath = "src/patterns/stbpat";
			inputPath = "C:/Users/Patrick/Desktop/DI/instances/StableGen";
			//inputPath = "C:/Users/Patrick/Desktop/DI/instances/StableGen/stb_371_20.apx";
			replacementPath = "src/patterns/stbrep";
			logPath = "C:/Users/Patrick/Desktop/DI/stats";
		}

		System.out.println("Reading input files!");
		AFParser parser = new AFParser();
		//use either graphPath for one specific file, or dirPath for a folder
		parser.parseAFs(patternPath,replacementPath,inputPath);

		GraphInfo patternInfo = new GraphInfo(parser.getPatternText(),null,2,false,patternPath);
		GraphInfo replacementInfo = new GraphInfo(parser.getReplaceText(),patternInfo.getArgBuffer(),3,false,replacementPath);
		ArrayList<GraphInfo> graphInfos = new ArrayList<GraphInfo>();

		if(startFileIndex == null){
			startFileIndex = 0;
		}
		if(endFileIndex == null){
			endFileIndex = 500;
		}

		if(startFileIndex < 0){
			startFileIndex = 0;
		}
		if(endFileIndex > parser.getGraphText().size()){
			endFileIndex = parser.getGraphText().size();
		}
		
		for(int i = startFileIndex;i<endFileIndex;i++){
			graphInfos.add(new GraphInfo(parser.getGraphText().get(i),null,1,false,parser.getPaths().get(i)));
		}

		System.out.println("Parsing files done!");

		System.out.println("Starting search for isomorphic subgraphs!");
		
		if(!parser.getDirectory()){
			System.out.println("Checking single graph!");
			parser = null; //don't need parser anymore, relevant data has been parsed
			
			BBMatch bbmatches = new BBMatch(patternInfo,graphInfos.get(0),50000,false); //int - samplesize, 0 - all
			MatchInfo info = new MatchInfo(bbmatches.getGraph(),bbmatches.getQuery(),bbmatches.getMatched());
			//bbmatches.printMatched();
			System.out.println("Found " + bbmatches.getMatched().size() + " instances of isomorphism!");

			System.out.println("Starting to replace fitting matches!");
			Replacer replacer = new Replacer(info,replacementInfo,50000,false); //int - samplesize, 0 - all
			replacer.printResult(false,true);
		}
		else{
			ReplacementCollector collector = new ReplacementCollector(patternInfo,replacementInfo,graphInfos);
			collector.generateMatches(0,50000,false);//numOfFiles (0-all),sampleSize,debug
			collector.collectReplacements(50000,false);
			collector.logReplacements(logPath);
		}
	}

	private static void parseCommandLine(String[] args) {
		HashMap<String,Integer> options = new HashMap<String,Integer>();
		ArrayList<String> argList = new ArrayList<String>();

		String usage = "usage: patternPath inputPath replacementPath logPath [-s startIndex -e endIndex]";
		String curropt = "";
		boolean prevopt = false;

		for(String s: args){ //check whether this seems to be a correct usage, save if yes
			if(!prevopt){
				if(s.charAt(0) == '-'){
					options.put(s, null);
					curropt = s;
					prevopt = true;
				}
				else{
					argList.add(s);
				}
			}
			else{
				if(s.charAt(0) == '-'){
					System.out.println(usage);
					System.exit(0);
				}
				else{
					try{
						options.put(curropt, Integer.parseInt(s));
						prevopt = false;
					} catch (NumberFormatException e){
						System.out.println(usage);
						System.exit(0);
					}
				}
			}
		}

		if(argList.size() == 4){
			patternPath = argList.get(0);
			inputPath = argList.get(1);
			replacementPath = argList.get(2);
			logPath = argList.get(3);
		}

		if(options.get("-s") == null){
			startFileIndex = 0;
		}
		else{
			startFileIndex = options.get("-s");
		}
		//startFileIndex = options.getOrDefault("-s",0);
		
		if(options.get("-e") == null){
			endFileIndex = 0;
		}
		else{
			endFileIndex = options.get("-e");
		}
		//endFileIndex = options.getOrDefault("-e", 0);

		/*System.out.println(patternPath + ", " + inputPath + ", " + replacementPath + ", " + logPath + ", " + startFileIndex
				+ ", " + endFileIndex);*/
	}

}
