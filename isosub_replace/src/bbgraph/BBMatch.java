package bbgraph;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import graphinfo.GraphInfo;
import graphinfo.Edge;
import graphinfo.Vertex;

public class BBMatch {

	private GraphInfo query;
	private GraphInfo graph;
	//		graphm	instm	list_of_instance_node_matches	list_of_instance_edge_matches
	private ArrayList<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex,Vertex>>,ArrayList<AbstractMap.SimpleEntry<Edge,Edge>>>> matched;
	enum Dir {in,out};
	private ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> vmatch;
	private ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> ematch;
	private LinkedList<AbstractMap.SimpleEntry<Vertex, Vertex>> stack;
	private boolean debug;
	private int sampleSize;
	private boolean terminate;

	public BBMatch(GraphInfo query, GraphInfo graph, int sampleSize, boolean debug){
		this.query = query;
		this.graph = graph;
		this.setDebug(debug);
		this.setSampleSize(sampleSize);
		terminate = false;
		getMatching();
	}

	private void getMatching() {
		matched = new ArrayList<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex,Vertex>>,ArrayList<AbstractMap.SimpleEntry<Edge,Edge>>>>();

		Vertex ustart = query.getArgs().get(0);

		ArrayList<Vertex> candidates = new ArrayList<Vertex>();

		for(Vertex u2: graph.getArgs()){
			if(mnp(ustart,u2)){
				candidates.add(u2);
			}
		}

		for(Vertex u2: candidates){
			if(candidates.indexOf(u2) > 0 && candidates.indexOf(u2)%100==0){
				debugPrint(candidates.indexOf(u2) + "/" + candidates.size());
			}
			
			vmatch = new ArrayList<AbstractMap.SimpleEntry<Vertex,Vertex>>();
			ematch = new ArrayList<AbstractMap.SimpleEntry<Edge,Edge>>();
			stack = new LinkedList<AbstractMap.SimpleEntry<Vertex,Vertex>>();

			stack.addFirst(new AbstractMap.SimpleEntry<Vertex,Vertex>(ustart,u2));
			printStack(stack);
			vmatch.add(new AbstractMap.SimpleEntry<Vertex,Vertex>(ustart,u2));

			search(vmatch,ematch,stack);
			if(terminate){
				return;
			}
		}
	}

	private boolean mnp(Vertex u1, Vertex u2) {
		int u1indeg, u1outdeg, u2indeg, u2outdeg;

		u1indeg = getDeg(u1,query.getAtts(),Dir.in);
		u1outdeg = getDeg(u1,query.getAtts(),Dir.out);
		u2indeg = getDeg(u2,graph.getAtts(),Dir.in);
		u2outdeg = getDeg(u2,graph.getAtts(),Dir.out);

		if(u1indeg <= u2indeg && u1outdeg <= u2outdeg){
			return true;
		}

		return false;
	}

	private int getDeg(Vertex ustart, ArrayList<Edge> edgeList, Dir dir) {
		int c = 0;

		for(Edge att : edgeList){
			if(dir == Dir.in && att.getB().equals(ustart)){
				c++;
			}
			else if(dir == Dir.out && att.getA().equals(ustart)){
				c++;
			}
		}

		return c;
	}

	private void search(ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> currvmatch, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> currematch, LinkedList<AbstractMap.SimpleEntry<Vertex, Vertex>> currstack) {
		if(!currstack.isEmpty()){
			AbstractMap.SimpleEntry<Vertex, Vertex> pair = currstack.removeFirst();
			//debugPrint("consumed: " + pair.getKey().getName() + "," + pair.getValue().getName());
			printStack(currstack);

			ArrayList<Edge> unmatchedAdjacent = getUnmatchedAdjacent(pair,currematch);
			if(!unmatchedAdjacent.isEmpty()){
				//debugPrint("more unmatched to branch");
				branchNodes(pair,unmatchedAdjacent,currvmatch,currematch,currstack);
			}
			else{
				//debugPrint("no more unmatched: search other");
				search(currvmatch,currematch,currstack);
				if(terminate){
					return;
				}
			}
		}
		else if(!terminate){
			matched.add(new AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>(copy(currvmatch),copy(currematch)));
			//debugPrint("added result: " + matched.size());

			if(sampleSize > 0 && matched.size() >= sampleSize){
				debugPrint("Stopping extraction of matches, reached sampleSize (" + matched.size() + " matches obtained!)");
				terminate = true;
				return;
			}
			else if(matched.size()%1000==0){
				//debugPrint(""+matched.size());
			}
		}
	}


	private ArrayList<Edge> getUnmatchedAdjacent(AbstractMap.SimpleEntry<Vertex, Vertex> pair, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> currematch) {
		ArrayList<Edge> unmatched = new ArrayList<Edge>();
		ArrayList<Edge> adjacent = getAdjacent(pair.getKey(),query.getAtts());

		//debugPrint("looking for unadj for: " + pair.getKey().getName() + ", " + pair.getValue().getName());

		for(Edge att : adjacent){
			//debugPrint("is (" + att.getA().getName() + ", " + att.getB().getName() + ") unadj?"); 
			if(!isEMatched(att,currematch,1)){
				unmatched.add(att);
			}
		}
		
		adjacent = null;

		return unmatched;
	}

	private ArrayList<Edge> getAdjacent(Vertex vertex, ArrayList<Edge> edgeList) {
		ArrayList<Edge> adjacent = new ArrayList<Edge>();

		for(Edge att : edgeList){
			if(att.getA().equals(vertex) || att.getB().equals(vertex)){
				adjacent.add(att);
			}
		}

		return adjacent;
	}

	private void branchNodes(AbstractMap.SimpleEntry<Vertex, Vertex> pair, ArrayList<Edge> unmatchedAdjacent, 
			ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> currvmatch, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> currematch, LinkedList<AbstractMap.SimpleEntry<Vertex, Vertex>> currstack) {

		HashMap<Integer,AbstractMap.SimpleEntry<Edge,ArrayList<Edge>>> candidates = new HashMap<Integer,AbstractMap.SimpleEntry<Edge,ArrayList<Edge>>>();

		//debugPrint("\nunmatched for: " + pair.getKey().getName() + "," + pair.getValue().getName()); 
		for(Edge r1: unmatchedAdjacent){
			//debugPrint(r1.getA().getName() + "," + r1.getB().getName());
			ArrayList<Edge> potential = getPotentialMatches(pair,r1);
			//debugPrint("potential matches: ");
			if(debug){
				/*for(Edge p : potential){
					debugPrint("(" + p.getA().getName() + "," + p.getB().getName() + ")"); 
				}*/
			}
			candidates.put(unmatchedAdjacent.indexOf(r1), new AbstractMap.SimpleEntry<Edge, ArrayList<Edge>>(r1,potential));
		}

		int k = unmatchedAdjacent.size();

		matchRelationship(0,k,candidates,pair,currvmatch,currematch,currstack);
	}

	private ArrayList<Edge> getPotentialMatches(AbstractMap.SimpleEntry<Vertex, Vertex> pair, Edge r1) {
		ArrayList<Edge> potMat = new ArrayList<Edge>();
		ArrayList<Edge> adjacent = getAdjacent(pair.getValue(),graph.getAtts());

		for(Edge r2: adjacent){
			if(mrp(pair,r1,r2)){
				potMat.add(r2);
			}
		}
		
		adjacent = null;

		return potMat;
	}

	//checks direction of edge, not label, since this does not exist here
	private boolean mrp(AbstractMap.SimpleEntry<Vertex, Vertex> pair, Edge r1, Edge r2) {
		if(r1.getA().equals(pair.getKey()) && r2.getA().equals(pair.getValue())){
			return true;
		}
		else if(r1.getB().equals(pair.getKey()) && r2.getB().equals(pair.getValue())){
			return true;
		}
		else{
			return false;
		}
	}

	private void matchRelationship(int i, int k, HashMap<Integer, AbstractMap.SimpleEntry<Edge, ArrayList<Edge>>> candidates,
			AbstractMap.SimpleEntry<Vertex, Vertex> current, ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> currvmatch, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> currematch,
			LinkedList<AbstractMap.SimpleEntry<Vertex, Vertex>> currstack) {

		Edge r1;

		try{
			r1 = candidates.get(i).getKey();
		} catch (NullPointerException n){
			return;
		}

		//debugPrint("# match candidates for " + r1.getA().getName() + "," +
		//r1.getB().getName() + ": " + candidates.get(i).getValue().size());

		for(Edge r2: candidates.get(i).getValue()){
			//debugPrint("check with: " + r2.getA().getName() + "," + r2.getB().getName());
			if(!isEMatched(r2,currematch,2)){
				AbstractMap.SimpleEntry<Edge, Edge> p1 = new AbstractMap.SimpleEntry<Edge, Edge>(r1,r2);
				AbstractMap.SimpleEntry<Vertex, Vertex> p2 = new AbstractMap.SimpleEntry<Vertex,Vertex>(current.getKey(),current.getValue());

				LinkedList<AbstractMap.SimpleEntry<Vertex, Vertex>> customtmpstack = copy(currstack);
				ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> customtmpvmatch = copy(currvmatch);

				if(check(new AbstractMap.SimpleEntry<AbstractMap.SimpleEntry<Edge,Edge>,AbstractMap.SimpleEntry<Vertex, Vertex>>(p1,p2),currvmatch,currstack)){
					currematch.add(p1);

					//int id = new Random().nextInt();
					//debugPrint("added match id " + id);
					if(debug){
						printVMatch(currvmatch);
						printEMatch(currematch);
						printStack(currstack);
					}

					LinkedList<AbstractMap.SimpleEntry<Vertex, Vertex>> tmpstack = copy(currstack);
					ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> tmpvmatch = copy(currvmatch);

					if(i<k-1){
						//debugPrint("iterate!");
						matchRelationship(i+1,k,candidates,current,currvmatch,currematch,currstack);
					}
					else{
						//debugPrint("search next!");
						search(currvmatch,currematch,currstack);
						if(terminate){
							return;
						}
					}

					currematch.remove(p1);
					currstack = copy(tmpstack);
					currvmatch = copy(tmpvmatch);
					
					//freeing references to be garbage collected
					tmpstack = null;
					tmpvmatch = null;
					
					//debugPrint("backtrack to " + id);
					if(debug){
						printVMatch(currvmatch);
						printEMatch(currematch);
						printStack(currstack);
					}
				}

				currstack = copy(customtmpstack);
				currvmatch = copy(customtmpvmatch);
				
				customtmpstack = null;
				customtmpvmatch = null;
			}
		}

	}

	//							r1						r2							u1		u2
	private boolean check(AbstractMap.SimpleEntry<AbstractMap.SimpleEntry<Edge, Edge>, AbstractMap.SimpleEntry<Vertex, Vertex>> pair, ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> currvmatch,
			LinkedList<AbstractMap.SimpleEntry<Vertex, Vertex>> currstack) {
		Vertex v1 = extractOther(pair.getKey().getKey(),pair.getValue().getKey());
		Vertex v2 = extractOther(pair.getKey().getValue(),pair.getValue().getValue());
		AbstractMap.SimpleEntry<Vertex,Vertex> checkPair = new AbstractMap.SimpleEntry<Vertex,Vertex>(v1,v2);

		//debugPrint("u: " + pair.getValue().getKey().getName() + ", u': " + pair.getValue().getValue().getName());
		//debugPrint("v1: " + v1.getName() + ", v2: " + v2.getName());

		Vertex oth;
		if((oth = otherMatchExists(checkPair,1,currvmatch)) != null){
			//debugPrint("Found another v1 for v1," + v2.getName() + ": " + oth.getName());
			return false;
		}
		else if((oth = otherMatchExists(checkPair,2,currvmatch)) != null){
			//debugPrint("Found another v2 for " + v1.getName() + ",v2: " + oth.getName());
			printVMatch(currvmatch);
			return false;
		}
		else if(!isVMatched(checkPair, currvmatch)){
			if(mnp(v1,v2)){
				//debugPrint("No VMatch yet, and mnp(" + v1.getName() + "," + v2.getName() + ") holds.");
				currstack.addFirst(checkPair);
				printStack(currstack);
				currvmatch.add(checkPair);
				//debugPrint("added vertex match: " + checkPair.getKey().getName() + ", " + checkPair.getValue().getName());
			}
			else{
				//debugPrint("Vertex not fitting.");
				return false;
			}
		}
		else{
			//debugPrint("Vertex " + v2.getName() + " already matched!");
		}

		return true;
	}

	private boolean isVMatched(AbstractMap.SimpleEntry<Vertex, Vertex> pair, ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> currvmatch) {
		for(AbstractMap.SimpleEntry<Vertex, Vertex> o : currvmatch){
			if(pair.getKey().equals(o.getKey()) && pair.getValue().equals(o.getValue())){
				return true;
			}
		}

		return false;
	}

	//type 1 - looking for pattern edge match, 2 - graph edge match
	private boolean isEMatched(Edge att, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> currematch, int type){
		for(AbstractMap.SimpleEntry<Edge,Edge> m : currematch){
			if(type == 1 && m.getKey().getA().equals(att.getA()) && m.getKey().getB().equals(att.getB())){
				//debugPrint("Edge is already matched!");
				return true;
			}
			else if(type == 2 && m.getValue().getA().equals(att.getA()) && m.getValue().getB().equals(att.getB())){
				//debugPrint("Edge is already matched!");
				return true;
			}
		}

		return false;
	}

	//1 - look for other key, 2 - look for other value
	private Vertex otherMatchExists(AbstractMap.SimpleEntry<Vertex,Vertex> pair, int i, ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> currvmatch) {
		for(AbstractMap.SimpleEntry<Vertex, Vertex> m : currvmatch){
			if(i == 1 && m.getValue().equals(pair.getValue()) && !m.getKey().equals(pair.getKey())){
				return m.getKey();
			}
			else if(i == 2 && m.getKey().equals(pair.getKey()) && !m.getValue().equals(pair.getValue())){
				return m.getValue();
			}
		}
		return null;
	}

	private Vertex extractOther(Edge edge, Vertex vertex) {
		if(edge.getA().equals(vertex)){
			return edge.getB();
		}
		else{
			return edge.getA();
		}
	}

	public GraphInfo getQuery() {
		return query;
	}

	public void setQuery(GraphInfo query) {
		this.query = query;
	}

	public GraphInfo getData() {
		return graph;
	}

	public void setData(GraphInfo data) {
		this.graph = data;
	}

	public GraphInfo getGraph() {
		return graph;
	}

	public void setGraph(GraphInfo graph) {
		this.graph = graph;
	}

	public ArrayList<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> getMatched() {
		return matched;
	}

	public void setMatched(ArrayList<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> matched) {
		this.matched = matched;
	}

	public ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> getVmatch() {
		return vmatch;
	}

	public void setVmatch(ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> vmatch) {
		this.vmatch = vmatch;
	}

	public ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> getEmatch() {
		return ematch;
	}

	public void setEmatch(ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> ematch) {
		this.ematch = ematch;
	}

	public LinkedList<AbstractMap.SimpleEntry<Vertex, Vertex>> getStack() {
		return stack;
	}

	public void setStack(LinkedList<AbstractMap.SimpleEntry<Vertex, Vertex>> stack) {
		this.stack = stack;
	}


	private <T> ArrayList<T> copy(ArrayList<T> original) {
		ArrayList<T> copy = new ArrayList<T>();

		for(T t: original){
			copy.add(t);
		}

		return copy;
	}

	private <T> LinkedList<T> copy(LinkedList<T> original) {
		LinkedList<T> copy = new LinkedList<T>();

		for(T t: original){
			copy.add(t);
		}

		return copy;
	}

	public void printMatched(){
		int i = 1;

		System.out.println("Found " + matched.size() + " matches for this pattern/graph combination:\n");

		for(AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> match : matched){
			System.out.println("Match " + i + " (pattern -> graph):");
			i++;
			System.out.println("Argument matches:");
			for(AbstractMap.SimpleEntry<Vertex, Vertex> v : match.getKey()){
				System.out.println(v.getKey().getName() + " matches " + v.getValue().getName());
			}
			System.out.println("Attack matches:");
			for(AbstractMap.SimpleEntry<Edge, Edge> e : match.getValue()){
				System.out.println("(" + e.getKey().getA().getName() + "," + e.getKey().getB().getName() + ") matches ("
						+ e.getValue().getA().getName() + "," + e.getValue().getB().getName() + ")");
			}

			System.out.println("");
		}

		if(i==1){
			System.out.println("No matches found!");
		}

	}

	private void printVMatch(ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> vm) {
		if(debug){
			//debugPrint("VMatch:");
			for(AbstractMap.SimpleEntry<Vertex, Vertex> match : vm){
				//debugPrint(match.getKey().getName() + " matches " + match.getValue().getName());
			}
		}
	}

	private void printEMatch(ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> em) {
		if(debug){
			//debugPrint("EMatch:");
			for(AbstractMap.SimpleEntry<Edge, Edge> match : em){
				//debugPrint("(" + match.getKey().getA().getName() + "," + match.getKey().getB().getName() + ") matches (" 
				//+ match.getValue().getA().getName() + "," + match.getValue().getB().getName() + ")");
			}
		}
	}

	private void printStack(LinkedList<AbstractMap.SimpleEntry<Vertex, Vertex>> st){
		if(debug){
			//debugPrint("Stack:");
			for(AbstractMap.SimpleEntry<Vertex, Vertex> e : st){
				//debugPrint("|_" + e.getKey().getName() + "," + e.getValue().getName() + "_|");
			}
		}
	}

	private void printSingleMatch(AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> match){
		//debugPrint("Argument matches:");
		for(AbstractMap.SimpleEntry<Vertex, Vertex> v : match.getKey()){
			//debugPrint(v.getKey().getName() + " matches " + v.getValue().getName());
		}
		//debugPrint("Attack matches:");
		for(AbstractMap.SimpleEntry<Edge, Edge> e : match.getValue()){
			//debugPrint("(" + e.getKey().getA().getName() + "," + e.getKey().getB().getName() + ") matches ("
			//+ e.getValue().getA().getName() + "," + e.getValue().getB().getName() + ")");
		}

		//debugPrint("");
	}

	private void debugPrint(String message){
		if(debug){
			System.out.println(message);
		}
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getSampleSize() {
		return sampleSize;
	}

	public void setSampleSize(int sampleSize) {
		this.sampleSize = sampleSize;
	}

}
