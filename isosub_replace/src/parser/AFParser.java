package parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class AFParser {

	private ArrayList<String> patternText;
	private ArrayList<String> replaceText;
	private ArrayList<ArrayList<String>> graphText;
	private ArrayList<String> paths;
	private boolean directory;

	public AFParser(){
		patternText = new ArrayList<String>();
		replaceText = new ArrayList<String>();
		graphText = new ArrayList<ArrayList<String>>();
		paths = new ArrayList<String>();
		this.setDirectory(false);
	}
	
	public void parseAFs(String patternPath, String replacePath, String graphPath) {
		patternText = getLinesFromPath(patternPath);
		replaceText = getLinesFromPath(replacePath);
		
		File graphFile = new File(graphPath);
		if(Files.isDirectory(graphFile.toPath())){
			setDirectory(true);
			File[] fileList = graphFile.listFiles();
			Arrays.sort(fileList);
			for(final File entry: fileList){
				String fp = entry.getPath().toString();
				String ending = fp.substring(fp.length()-4);
				if(!entry.isDirectory() && ending.equals(".apx")){
					graphText.add(getLinesFromPath(fp));
					paths.add(entry.getPath().toString());
				}
			}
		}
		else{
			graphText.add(getLinesFromPath(graphPath));
			paths.add(graphPath);
		}
	}
	
	private ArrayList<String> getLinesFromPath(String path) {
		ArrayList<String> lines = new ArrayList<String>();
		Scanner sc = null;
		
		try {
			sc = new Scanner(new FileReader(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		while(sc.hasNextLine()){
			lines.add(sc.nextLine());
		}
		sc.close();
		
		return lines;
	}

	public ArrayList<String> getPatternText() {
		return patternText;
	}

	public void setPatternText(ArrayList<String> patternText) {
		this.patternText = patternText;
	}

	public ArrayList<String> getReplaceText() {
		return replaceText;
	}

	public void setReplaceText(ArrayList<String> replaceText) {
		this.replaceText = replaceText;
	}

	public ArrayList<ArrayList<String>> getGraphText() {
		return graphText;
	}

	public void setGraphText(ArrayList<ArrayList<String>> graphText) {
		this.graphText = graphText;
	}
	
	public ArrayList<String> getPaths() {
		return paths;
	}

	public void setPaths(ArrayList<String> paths) {
		this.paths = paths;
	}

	public boolean getDirectory() {
		return directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

}
