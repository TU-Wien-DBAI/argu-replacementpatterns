package replace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import bbgraph.BBMatch;
import bbgraph.MatchInfo;
import graphinfo.GraphInfo;

public class ReplacementCollector {
	
	private ArrayList<MatchInfo> subMatches;
	private ArrayList<Replacer> replacers;
	private GraphInfo patternInfo;
	private GraphInfo replacementInfo;
	private ArrayList<GraphInfo> graphInfos;
	private int sampleSize;
	
	public ReplacementCollector(GraphInfo patternInfo, GraphInfo replacementInfo, ArrayList<GraphInfo> graphInfos){
		subMatches = new ArrayList<MatchInfo>();
		replacers = new ArrayList<Replacer>();
		this.patternInfo = patternInfo;
		this.setReplacementInfo(replacementInfo);
		this.graphInfos = graphInfos;
	}

	public void generateMatches(int numOfFiles, int matchSize, boolean debug) {
		
		if(numOfFiles == 0){
			numOfFiles = graphInfos.size();
		}
		else if(numOfFiles > graphInfos.size()){
			numOfFiles = graphInfos.size();
		}
		
		System.out.println("Computing all " + numOfFiles + " requested matches!");
		
		int c = 0;
		
		for(GraphInfo info: graphInfos){
			c++;
			
			String filePath = info.getOrigFile();
			//should only contain either \ or /
			String fileName = filePath.substring(filePath.lastIndexOf("\\")+1);
			fileName = fileName.substring(fileName.lastIndexOf("/")+1);
			
			System.out.println("Matching " + fileName + " at " + new SimpleDateFormat("HH-mm-ss").format(new Date()));
			
			BBMatch tmpMatch = new BBMatch(patternInfo,info,matchSize,debug);
			subMatches.add(new MatchInfo(tmpMatch.getGraph(),tmpMatch.getQuery(),tmpMatch.getMatched()));
			tmpMatch = null; //pls garbage collection remove unused parts, thx
			System.gc(); //this works, but heap still overflows at ~320 files with 4 GB allocated process memory
			if(c%Math.floor(((double)numOfFiles/10)) == 0){
				System.out.println("Checked " + c + "/" + numOfFiles + " files.");
			}
			if(c>=numOfFiles){
				break;
			}
		}
		
	}

	public void collectReplacements(int sampleSize, boolean debug) {

		System.out.println("Checking replacements!");
		
		this.sampleSize = sampleSize;
		
		for(MatchInfo match : subMatches){
			System.out.print(".");
			Replacer tmprep = new Replacer(match,replacementInfo,sampleSize,debug);
			replacers.add(tmprep);
		}
		System.out.println("");
		
	}


	public void logReplacements(String logPath) {
		System.out.println("Generating log file!");
		
		String logString = "pattern:;semantics:;sample size:\n";
		logString += patternInfo.getOrigFile() + ";" + patternInfo.getSemantics() + ";" + sampleSize + "\n";
		logString += "file:;replaced:;matches:;percentage:;;argrem:;percrem:;attrem:;argadd:;change:\n";
		
		DecimalFormat decformat = new DecimalFormat("0.0000");
		
		for(Replacer r: replacers){
			int replq = r.getReplaced();
			int ssize = r.getSampleSize();
			int remv = r.getRemovedVertices().size();
			int novert = r.getResultGraph().getArgs().size();
			int rema = r.getRemovedEdges().size();
			int adda = r.getAddedEdges().size();
			int noedge = r.getResultGraph().getAtts().size();
			
			logString += r.getMatches().getGraph().getOrigFile() + ";" + replq + ";" + ssize + ";" + decformat.format((double) replq/ssize) +
					";;" + remv + ";" + decformat.format((double) remv/(remv+novert)) + ";" + rema + ";" + adda + ";" + 
					decformat.format((double) (rema-adda)/(noedge+remv-adda)) + "\n";
		}
				
		String dateString = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
		String newPath = logPath+"/results-"+dateString+"/";
		
		File newDir = new File(newPath);
		newDir.mkdirs();
		
		try (PrintWriter out = new PrintWriter(newPath + "log.csv")){
			out.println(logString);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		for(MatchInfo m : subMatches){
			String stringRep = m.getGraphStringRep(m.getGraph());
			Path orig = Paths.get(m.getGraph().getOrigFile());
			try(PrintWriter out = new PrintWriter(newPath + orig.getFileName().toString())){
				out.println(stringRep);
			} catch (FileNotFoundException e){
				e.printStackTrace();
			}
		}
		
		System.out.println("Wrote results to " + logPath+"/results"+dateString+"/");
		
	}
	
	public ArrayList<Replacer> getReplacers() {
		return replacers;
	}

	public void setReplacers(ArrayList<Replacer> replacers) {
		this.replacers = replacers;
	}

	public GraphInfo getReplacementInfo() {
		return replacementInfo;
	}

	public void setReplacementInfo(GraphInfo replacementInfo) {
		this.replacementInfo = replacementInfo;
	}

}
