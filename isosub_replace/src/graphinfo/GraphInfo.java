package graphinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

public class GraphInfo {

	private ArrayList<Vertex> args;
	private ArrayList<Edge> atts;
	private ArrayList<String> pout;
	private ArrayList<String> pin;
	private HashMap<String,Vertex> argBuffer;
	private HashMap<Vertex,ArrayList<Vertex>> attackMap, attackerMap;
	private int type; //1 - graph, 2 - pattern, 3 - replacement
	public enum Sem {stb,adm,prf,com,grd};
	private Sem semantics;
	private boolean randomize; //randomizes complete argument list for random starting point
	private String origFile;

	public GraphInfo(){}

	public GraphInfo(ArrayList<String> input, HashMap<String,Vertex> existingArgs, int type, boolean randomize, String origFile){
		args = new ArrayList<Vertex>();
		atts = new ArrayList<Edge>();
		setPout(new ArrayList<String>());
		setPin(new ArrayList<String>());
		argBuffer = new HashMap<String,Vertex>();
		this.type = type;
		this.setRandomize(randomize);
		this.setOrigFile(origFile);

		readInput(input,existingArgs);

		input = null;
		
		mapAttacks();
		//printMaps();
		//printGraph();

		if(randomize){
			Collections.shuffle(args);
		}
	}

	private void readInput(ArrayList<String> input, HashMap<String, Vertex> existingArgs) {
		for(String i: input){
			if(i.length() > 0 && i.charAt(0) == '%'){ // we don't care about comments
				continue;
			}

			if(i.contains("arg")){
				addVertex(i.substring(4, i.length()-2),existingArgs);
			}
			else if(i.contains("att")){
				String[] split = i.substring(4, i.length()-2).split(",");
				addVertex(split[0],existingArgs);
				addVertex(split[1],existingArgs);
				atts.add(new Edge(vertexLookup(split[0]),vertexLookup(split[1]),this));
			}
			else if(i.contains("pin")){ //prohibited from being attacked by non-core arguments
				pin.add(i.substring(4,i.length()-2));
			}
			else if(i.contains("pout")){ //prohibited from attacking non-core arguments
				pout.add(i.substring(4,i.length()-2));
			}
			else if(type == 2 || type == 3){
				if(i.contains("core")){
					String cname = i.substring(5, i.length()-2);
					addVertex(cname,existingArgs);
					vertexLookup(cname).setCore(true);
				}
				else if(type == 3 && i.contains("sem")){
					setSemantics(i.substring(4,i.length()-2));
				}
			}
		}

		setProhibited();
	}

	private void setProhibited() {
		for(String name : pin){
			Vertex arg = vertexLookup(name);
			if(arg != null){
				arg.setPin(true);
			}
		}
		for(String name : pout){
			Vertex arg = vertexLookup(name);
			if(arg != null){
				arg.setPout(true);
			}
		}
	}

	private void setSemantics(String sem) {
		switch(sem){
		case "stb":
			semantics = Sem.stb;
			break;
		case "adm":
			semantics = Sem.adm;
			break;
		case "prf":
			semantics = Sem.prf;
			break;
		case "com":
			semantics = Sem.com;
			break;
		case "grd":
			semantics = Sem.grd;
			break;
		default:
			semantics = null;
		}
	}

	private void addVertex(String name, HashMap<String, Vertex> existingArgs){		
		if(vertexLookup(name) == null){
			Vertex n = new Vertex(name);
			if(existingArgs == null){
				addArgument(name,n);
			}
			else{
				Vertex v = existingArgs.get(name);
				if(v != null){
					addArgument(name,v);
				}
				else{
					addArgument(name,n);
				}
			}
		}
	}

	private void addArgument(String name, Vertex n) {
		args.add(n);
		argBuffer.put(name, n);
	}

	public Vertex vertexLookup(String name) {
		return argBuffer.get(name);
	}

	public void mapAttacks() {
		setAttackMap(new HashMap<Vertex,ArrayList<Vertex>>());
		setAttackerMap(new HashMap<Vertex,ArrayList<Vertex>>());

		for(Edge att: atts){
			addMapping(attackMap,att.getA(),att.getB());
			addMapping(attackerMap,att.getB(),att.getA());
		}
	}

	public void addMapping(HashMap<Vertex, ArrayList<Vertex>> map, Vertex a, Vertex b) {
		if(map.containsKey(a)){
			map.get(a).add(b);
		}
		else{
			map.put(a, new ArrayList<Vertex>());
			map.get(a).add(b);
		}
	}

	public ArrayList<Vertex> getAttacked(Vertex v){
		ArrayList<Vertex> atd = getAttackMap().get(v);

		if(atd==null){
			return new ArrayList<Vertex>();
		}
		else{
			return atd;
		}
	}

	public ArrayList<Vertex> getAttackers(Vertex v){
		ArrayList<Vertex> atk = getAttackerMap().get(v);

		if(atk==null){
			return new ArrayList<Vertex>();
		}
		else{
			return atk;
		}
	}

	public ArrayList<Vertex> getArgs() {
		return args;
	}

	public void setArgs(ArrayList<Vertex> args) {
		this.args = args;
	}

	public ArrayList<Edge> getAtts() {
		return atts;
	}

	public void setAtts(ArrayList<Edge> atts) {
		this.atts = atts;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Sem getSemantics() {
		return semantics;
	}

	public void setSemantics(Sem semantics) {
		this.semantics = semantics;
	}

	public boolean hasVertex(Vertex s, int i) {
		for(Vertex v : args){
			if(v.getName().equals(s.getName())){
				if(i == 2 && !v.isCore()){
					return false;
				}
				else{
					return true;
				}
			}
		}
		return false;
	}

	public void printGraph() {
		System.out.println("arguments:");

		for(Vertex v : args){
			System.out.print(v.getName());
			String s = "";
			if(v.getPin()){
				s += "-";
			}
			if(v.getPout()){
				s += "+";
			}
			if(s.length() != 0){
				s = "(" + s;
				s = s + ")";
			}
			System.out.println(s);
		}

		System.out.println("attacks:");

		for(Edge e : atts){
			System.out.println("(" + e.getA().getName() + "," + e.getB().getName() + ")");
		}

	}
	
	public void printMaps() {
		for (Entry<Vertex, ArrayList<Vertex>> e : attackMap.entrySet()) {
			System.out.print(e.getKey().getName() + " attacks: ");
			for(Vertex v: e.getValue()) {
				System.out.print(v.getName() + ", ");
			}
			System.out.println("");
		}
		System.out.println("");
		for (Entry<Vertex, ArrayList<Vertex>> e : attackerMap.entrySet()) {
			System.out.print(e.getKey().getName() + " is attacked by: ");
			for(Vertex v: e.getValue()) {
				System.out.print(v.getName() + ", ");
			}
			System.out.println("");
		}
		System.out.println("");
	}

	public HashMap<Vertex,ArrayList<Vertex>> getAttackMap() {
		return attackMap;
	}

	public void setAttackMap(HashMap<Vertex,ArrayList<Vertex>> attackMap) {
		this.attackMap = attackMap;
	}

	public HashMap<Vertex,ArrayList<Vertex>> getAttackerMap() {
		return attackerMap;
	}

	public void setAttackerMap(HashMap<Vertex,ArrayList<Vertex>> attackerMap) {
		this.attackerMap = attackerMap;
	}

	public HashMap<String, Vertex> getArgBuffer() {
		return argBuffer;
	}

	public void setArgBuffer(HashMap<String, Vertex> argBuffer) {
		this.argBuffer = argBuffer;
	}

	public boolean isRandomize() {
		return randomize;
	}

	public void setRandomize(boolean randomize) {
		this.randomize = randomize;
	}

	public ArrayList<String> getPout() {
		return pout;
	}

	public void setPout(ArrayList<String> arrayList) {
		this.pout = arrayList;
	}

	public ArrayList<String> getPin() {
		return pin;
	}

	public void setPin(ArrayList<String> arrayList) {
		this.pin = arrayList;
	}

	public String getOrigFile() {
		return origFile;
	}

	public void setOrigFile(String origFile) {
		this.origFile = origFile;
	}
}
