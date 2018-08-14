package replace;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import bbgraph.MatchInfo;
import graphinfo.Edge;
import graphinfo.GraphInfo;
import graphinfo.Vertex;
import graphinfo.GraphInfo.Sem;

public class Replacer {

	private MatchInfo matches;
	private GraphInfo replacement;
	private GraphInfo resultGraph;
	private boolean debug;
	private ArrayList<Vertex> removedVertices;
	private ArrayList<Edge> removedEdges;
	private ArrayList<Edge> addedEdges;
	private int replaced;
	private int sampleSize; //sample size, if 0 no sampling, but take complete list
	private List<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> sample;
	private List<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex,Vertex>>,ArrayList<AbstractMap.SimpleEntry<Edge,Edge>>>> replacedMatches;

	public Replacer(MatchInfo matches, GraphInfo replacement, int sampleSize, boolean debug) {
		this.matches = matches;
		this.replacement = replacement;
		this.setResultGraph(new GraphInfo());
		this.debug = debug;
		this.removedVertices = new ArrayList<Vertex>();
		this.removedEdges = new ArrayList<Edge>();
		this.addedEdges = new ArrayList<Edge>();
		this.sampleSize = sampleSize;
		this.setReplaced(0);
		resultGraph.setArgs(matches.getGraph().getArgs());
		resultGraph.setAtts(matches.getGraph().getAtts());
		resultGraph.mapAttacks();
		sampleMatches();
		checkMatches();
	}

	private void sampleMatches() {
		sample = matches.getMatched();

		if(sampleSize > 0 && sample.size() > 0){
			Collections.shuffle(sample);
			if(!sample.isEmpty()){
				sample = sample.subList(0, Math.min(sampleSize-1,sample.size()));
			}
		}

		sampleSize = sample.size();

		//printSamples();

	}

	private void checkMatches() {
		replacedMatches = new ArrayList<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex,Vertex>>,ArrayList<AbstractMap.SimpleEntry<Edge,Edge>>>>();

		if(replacable()){
			printDebugMessage("replacement is compatible");
			for(AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m : sample){
				switch(replacement.getSemantics()){
				case stb:
					if(notStableMatch(m) == null){ // we don't want insufficiently matched matches
						replaceMatch(m,replacedMatches);
					}
					break;
				default:
					if(notGeneralMatch(m) == null){
						replaceMatch(m,replacedMatches);
					}
					break;
				}
			}
		}
		else{
			printDebugMessage("The replacement graph does not fit the pattern!");
			return;
		}

		printDebugMessage("Some (" + (sample.size()-replaced) + ") unsuitable matches were not applied!");

		printDebugMessage("");
	}

	private AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> notStableMatch(AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m) {
		for(AbstractMap.SimpleEntry<Vertex, Vertex> vm : m.getKey()){
			printDebugMessage("check match: " + vm.getKey().getName() + ", " + vm.getValue().getName());
			if(vm.getKey().isCore() && !checkAttackStable(vm, m)){ //if we find an incoming attack that is not matched
				printDebugMessage("stb check failed: " + vm.getKey().getName() + ", " + vm.getValue().getName());
				return m;
			}
		}
		return null;
	}

	private AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> notGeneralMatch(AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m) {
		for(AbstractMap.SimpleEntry<Vertex, Vertex> vm : m.getKey()){
			printDebugMessage("check match: " + vm.getKey().getName() + ", " + vm.getValue().getName());
			if(vm.getKey().isCore() && !checkAttackGeneral(vm, m)){ //if we find an incoming attack that is not matched
				printDebugMessage("general check failed: " + vm.getKey().getName() + ", " + vm.getValue().getName());
				return m;
			}
		}
		return null;
	}

	private boolean checkAttackStable(AbstractMap.SimpleEntry<Vertex, Vertex> vm, AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m) {
		ArrayList<Vertex> resultAttackers = resultGraph.getAttackers(vm.getValue());		
		ArrayList<Vertex> patternAttackers = matches.getQuery().getAttackers(vm.getKey());

		if(!checkInsideAttackers(vm,m,patternAttackers,resultAttackers)){//if is attacked by illegal core args
			return false;
		}

		if(!checkOutsideAttackers(vm,m,patternAttackers,resultAttackers,Sem.stb)){//if is attacked by illegal outside args
			return false;
		}

		return true;
	}

	private boolean checkAttackGeneral(AbstractMap.SimpleEntry<Vertex, Vertex> vm,
			AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m) {
		ArrayList<Vertex> resultAttackers = resultGraph.getAttackers(vm.getValue());
		ArrayList<Vertex> patternAttackers = matches.getQuery().getAttackers(vm.getKey());

		if(!checkInsideAttackers(vm,m,patternAttackers,resultAttackers)){
			return false;
		}

		if(!checkOutsideAttackers(vm,m,patternAttackers,resultAttackers,null)){
			return false;
		}

		if(vm.getKey().getPout() && !checkAttacksOnOutside(vm, m)){ //if outside can't be attacked, but is
			return false;
		}

		return true;
	}


	private boolean checkAttacksOnOutside(AbstractMap.SimpleEntry<Vertex, Vertex> vm, AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m) {
		// check whether some argument in core attacks an argument it is not supposed to
		// i.e. is the number of attacks correct (internal exact match is already checked, right?)

		ArrayList<Vertex> resultAttacks = resultGraph.getAttacked(vm.getValue());
		ArrayList<Vertex> patternAttacks = matches.getQuery().getAttacked(vm.getKey());

		if(resultAttacks == null && patternAttacks == null){ //equal --> correct
			return true;
		}
		else if(resultAttacks == null || patternAttacks == null){ //attacks where there should be none
			return false;
		}
		else if(resultAttacks.size() < patternAttacks.size()){ //does not match all attacks!
			return false;
		}
		else if(checkAlreadyAttacked(vm,resultAttacks,patternAttacks,m)){
			return false;
		}
		else{
			return true;
		}
	}

	private boolean checkAlreadyAttacked(AbstractMap.SimpleEntry<Vertex, Vertex> vm, ArrayList<Vertex> resultAttacks, ArrayList<Vertex> patternAttacks, AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m2) {

		printDebugMessage("for " + vm.getKey().getName() + ", " + vm.getValue().getName() + ":");

		for(Vertex v : resultAttacks){ // all outside argument vmv attacks are contained in resultAttacks
			Vertex m = findMatchedVertex(v,m2.getKey(),2);
			ArrayList<Vertex> coreAttackers = new ArrayList<Vertex>();
			ArrayList<Vertex> matchedCoreAttackers = new ArrayList<Vertex>();

			if(m != null && !m.isCore()){ //those that do not match core arguments
				//checked for how many core attackers they have
				ArrayList<Vertex> attackedAttackers = matches.getQuery().getAttackers(m);

				if(attackedAttackers != null && !attackedAttackers.isEmpty()){
					for(Vertex c : attackedAttackers){
						if(c.isCore()){
							coreAttackers.add(c);
						}
					}
				}

				//System.out.print("ca: ");
				//printVertexSet(coreAttackers);

				//if in result attackers matched to core arguments, this is illegal

				for(Vertex fromall: resultGraph.getAttackers(v)){
					Vertex fromm = findMatchedVertex(fromall,m2.getKey(),2);
					if(fromm != null && fromm.isCore()){
						matchedCoreAttackers.add(fromall);
					}
				}

				//System.out.print("ma: ");
				//printVertexSet(matchedCoreAttackers);

				if(coreAttackers.size() != matchedCoreAttackers.size()){
					printDebugMessage("failed!");
					return true;
				}
			}

		}

		return false;
	}

	/**
	 * checks whether there are illegal core on core attacks
	 * @param matchToCheck the vertex match that is checked
	 * @param m all matches
	 * @param resultAttackers attacks on match value in graph
	 * @param patternAttackers attacks on match key in pattern
	 * @return if everything is in order (i.e. there are no
	 * 		wrong core on core attacks or missing attacks)
	 */
	private boolean checkInsideAttackers(AbstractMap.SimpleEntry<Vertex, Vertex> matchToCheck, AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m,
			ArrayList<Vertex> patternAttackers, ArrayList<Vertex> resultAttackers) {

		/*boolean special = false;
		if(matchToCheck.getValue().getName().equals("a165")) {
			System.out.println("Special check: " + matchToCheck.getKey().getName() + ", a165");
			special = true;
		}*/

		printDebugMessage("checking inside attackers: " + matchToCheck.getValue().getName());	
		if(matchToCheck.getKey().isCore()){ //if there is an attacker from inside, then it must be exactly matched!			
			for(Vertex patarg: patternAttackers) {
				if(patarg.isCore()) { //this method checks only for core on core attacks
					Vertex graphVertex = findMatchedVertex(patarg,m.getKey(),1);
					if(!resultAttackers.contains(graphVertex)) { //this does not attack our value
						return false; //attack is missing
					}
				}
			}
			for(Vertex patarg: matches.getQuery().getArgs()) {
				if(patarg.isCore()) { //looks at all the core arguments in pattern
					if(!patternAttackers.contains(patarg)) { //check all core arguments not attacking key
						Vertex graphVertex = findMatchedVertex(patarg,m.getKey(),1);
						/*if(special && graphVertex.getName().equals("a166")) {
							System.out.println(patarg.getName() + " is matched to " + "a166");
							for(Vertex v: resultAttackers) {
								System.out.println(v.getName());
							}
						}*/
						if(resultAttackers.contains(graphVertex)) {
							/*if(special) {
								System.out.println("correct behaviour");
							}*/
							//this is an attack from a core matched argument onto a core matched argument that don't have a patern attack relation
							return false; 
						}
					}
				}
			}
		}

		return true;
	}

	private boolean checkOutsideAttackers(AbstractMap.SimpleEntry<Vertex, Vertex> vm, AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m,
			ArrayList<Vertex> patternAttackers,ArrayList<Vertex> resultAttackers, Sem semantic) {

		if(!checkMultipleOutsideAttacks(vm,patternAttackers,m)){
			return false;
		}
		else if(vm.getKey().getPin()){
			if(!checkNoOutsideAttacks(vm,resultAttackers,patternAttackers,m)){
				return false;
			}
		}

		return true;
	}

	private boolean checkMultipleOutsideAttacks(AbstractMap.SimpleEntry<Vertex, Vertex> vm, ArrayList<Vertex> patternAttackers, AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m) {
		boolean hasOut = false;

		if(patternAttackers != null){ //if there is an attacker from outside, then there can be more in result!
			printDebugMessage(vm.getKey().getName() + " has " + patternAttackers.size() + " atts to be matched!");
			for(Vertex v: patternAttackers){
				if(!hasOut){
					printDebugMessage("checking outside attacker: " + v.getName());					
					for(AbstractMap.SimpleEntry<Vertex, Vertex> mp: m.getKey()){
						if(mp.getKey().equals(v)){
							printDebugMessage("Found outside attacker match for " + v.getName() + ": " + mp.getValue().getName() );
							hasOut = true;
							break;
						}
					}
					if(!hasOut){
						return false;
					}
				}
			}
		}

		return true;
	}

	private boolean checkNoOutsideAttacks(AbstractMap.SimpleEntry<Vertex, Vertex> vm, ArrayList<Vertex> resultAttackers, ArrayList<Vertex> patternAttackers, AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m){
		//if there is no attacker from outside, there can't one to be matched!
		int resoutatt = resultAttackers.size();
		int patoutatt = patternAttackers.size();

		for(Vertex v: resultAttackers){
			if(matchIsCore(v,m.getKey())){
				resoutatt--;
			}
		}

		for(Vertex v: patternAttackers){
			if(v.isCore()){
				patoutatt--;
			}
		}

		if(patoutatt == 0 && resoutatt != 0){
			printDebugMessage("There are no attackers onto " + vm.getKey().getName() + " from outside, but " + resoutatt + " in the result!");
			return false;
		}

		return true;
	}

	private boolean matchIsCore(Vertex v, ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> arrayList) {

		for(AbstractMap.SimpleEntry<Vertex, Vertex> vm : arrayList){
			if(vm.getValue().equals(v) && vm.getKey().isCore()){
				return true;
			}
		}

		return false;
	}

	private void replaceMatch(AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m, List<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> rm) {
		if(stillMatches(m)){
			setReplaced(getReplaced() + 1);
			rm.add(m);
			replacePattern(m);
		}
	}

	private boolean replacable() { // meaning each replacement vertex must exist in pattern
		for(Vertex c : replacement.getArgs()){
			if(c.isCore() && !matches.getQuery().hasVertex(c,2)){
				return false;
			}
			else if(!c.isCore() && !matches.getQuery().hasVertex(c,1)){
				return false;
			}
		}
		return true;
	}

	private boolean stillMatches(AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> currentMatch) {
		ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> vm = currentMatch.getKey();
		ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> em = currentMatch.getValue();

		for(AbstractMap.SimpleEntry<Vertex, Vertex> vp : vm){
			if(!resultGraph.getArgs().contains(vp.getValue())){
				printDebugMessage("The argument " + vp.getValue().getName() + " has been previously removed, match invalid!");
				return false;
			}
		}

		for(AbstractMap.SimpleEntry<Edge, Edge> ep: em){
			if(!resultGraph.getAtts().contains(ep.getValue())){
				printDebugMessage("The attack (" + ep.getValue().getA().getName() + "," + ep.getValue().getB().getName() +
						") has been previously removed, match invalid!");
				return false;
			}
		}

		return true;
	}

	private void replacePattern(AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> currentMatch) {
		ArrayList<Edge> newEdges = new ArrayList<Edge>();
		ArrayList<Edge> oldEdges = new ArrayList<Edge>();

		for(AbstractMap.SimpleEntry<Vertex, Vertex> vm : currentMatch.getKey()){ //for each vertex replace
			if(toRetain(vm)){
				for(Vertex v : replacement.getArgs()){
					if(v.getName().equals(vm.getKey().getName())){
						for(Edge e : replacement.getAtts()){ //add all edges in replacement
							Edge toAdd = edgeLookup(e,currentMatch);
							if(!resultGraph.getAtts().contains(toAdd)) { //except if we already have them
								newEdges.add(toAdd);
							}
						}
					}
				}
			}
			else{
				//just delete it!
				deleteVertex(resultGraph,vm);
			}
		}

		//System.out.println("want to add");
		newEdges = checkSimilarVertices(newEdges,currentMatch);
		addNewEdges(resultGraph,newEdges);

		for(AbstractMap.SimpleEntry<Edge, Edge> em: currentMatch.getValue()){
			if(replacement.getArgs().contains(em.getKey().getA()) && replacement.getArgs().contains(em.getKey().getB())){
				if(!replacement.getAtts().contains(em.getKey())){
					oldEdges.add(em.getValue());
				}
			} //else it has been removed with vertex anyway
		}

		//System.out.println("want to remove");
		oldEdges = checkSimilarVertices(oldEdges,currentMatch);
		removeOldEdges(resultGraph,oldEdges);

	}

	private ArrayList<Edge> checkSimilarVertices(ArrayList<Edge> alteredEdges, SimpleEntry<ArrayList<SimpleEntry<Vertex, Vertex>>, ArrayList<SimpleEntry<Edge, Edge>>> currentMatch) {
		//remove/add edges similar to the ones already removed/added between core and non-core args

		ArrayList<Edge> alsoAlter = new ArrayList<Edge>();
		
		if(alteredEdges.isEmpty()) {
			return alteredEdges;
		}

		boolean acore, bcore;
		Vertex a,b;

		for (Edge e: alteredEdges) {
			a = findMatchedVertex(e.getA(),currentMatch.getKey(), 2);
			b = findMatchedVertex(e.getB(),currentMatch.getKey(), 2);

			acore = a.isCore();
			bcore = b.isCore();

			if(!((acore && bcore) || (!acore && !bcore))) { //i.e. one core, one not
				if(acore) { //core attacks non-core
					//TODO check correctness
					//look for similar vertices in db graph
					ArrayList<Vertex> alsoAttackers = resultGraph.getAttackerMap().get(e.getB()); //these also attack our non-core arg

					alsoAttackers = findCoreMatches(alsoAttackers, currentMatch.getKey()); //these core attack attack our arg

					ArrayList<Vertex> attackedCollection = new ArrayList<Vertex>(); //collect all args attacked by those core matches

					attackedCollection = getAttackIntersection(alsoAttackers,2);

					for(Vertex v : attackedCollection) {
						if(v != e.getB()) {
							alsoAlter.add(new Edge(e.getA(),v,resultGraph));
						}
					}
				}
				else if(bcore) { // non-core attacks core
					//look for similar vertices in db graph
					//System.out.println("Checking interaction " + e.getA().getName() + ", " + e.getB().getName() + " matched to " + a.getName() + ", " + b.getName());

					ArrayList<Vertex> alsoAttacked = resultGraph.getAttackMap().get(e.getA()); //these are attacked by our attacker

					alsoAttacked = findCoreMatches(alsoAttacked, currentMatch.getKey()); //these core are attacked by our attacker

					ArrayList<Vertex> attackedCollection = new ArrayList<Vertex>(); //collect all args that also attack all the same as the attacker

					attackedCollection = getAttackIntersection(alsoAttacked,1);

					for(Vertex v : attackedCollection) {
						if(v != e.getA()) {
							//System.out.println("also: " + v.getName() + ", " + e.getB().getName());
							alsoAlter.add(new Edge(v,e.getB(),resultGraph));
						}
					}
				}

			}
		}

		alteredEdges.addAll(alsoAlter);
		
		return alteredEdges;
	}

	/**
	 * finds all arguments that have the same relations to some set of arguments
	 * @param baseSet set of arguments that are related to a specific argument
	 * @param type related: 1 - they are attacked, 2 - they are attacking
	 * @return all other arguments that are related in the same way to all arguments in the base set
	 */
	private ArrayList<Vertex> getAttackIntersection(ArrayList<Vertex> baseSet, int type) {
		ArrayList<Vertex> intersection = new ArrayList<Vertex>();
		ArrayList<Vertex> tmp;

		if(baseSet != null && !baseSet.isEmpty()) {
			if(type == 1) { //fill with values from first element
				intersection = resultGraph.getAttackerMap().get(baseSet.get(0));
			}
			else if(type == 2) {
				intersection = resultGraph.getAttackMap().get(baseSet.get(0));
			}
			for(Vertex v: baseSet) {
				tmp = new ArrayList<Vertex>();
				if(type == 1) { //get new values
					tmp = resultGraph.getAttackerMap().get(v);
				}
				else if(type == 2) {
					tmp = resultGraph.getAttackMap().get(v);
				}

				if(intersection != null && !intersection.isEmpty()) { //if there is nothing we are done, otw filter out
					intersection.retainAll(tmp);
				}
				else {
					return new ArrayList<Vertex>();
				}

			}
		}

		return intersection;
	}

	private ArrayList<Vertex> findCoreMatches(ArrayList<Vertex> vertexList, ArrayList<SimpleEntry<Vertex, Vertex>> vmatches) {
		ArrayList<Vertex> nonCore = new ArrayList<Vertex>();

		if(vertexList != null) {
			for(Vertex v: vertexList) {
				Vertex tmp = findMatchedVertex(v,vmatches,2);
				if(tmp == null || !tmp.isCore()) {
					nonCore.add(v);
				}
			}
		}

		if(vertexList != null) {
			vertexList.removeAll(nonCore);
		}

		return vertexList;
	}

	@SuppressWarnings({ "unlikely-arg-type", "unused" })
	private boolean edgeNotMatched(ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> match, Edge e) {
		for(AbstractMap.SimpleEntry<Edge, Edge> em: match){
			if(em.getValue().equals(match)){
				return true;
			}
		}
		return false;
	}

	private void addNewEdges(GraphInfo rg, ArrayList<Edge> ne) {
		for(Edge edge : ne){
			if(!rg.getAtts().contains(edge)){
				addedEdges.add(edge);
				rg.getAtts().add(edge);
				rg.addMapping(rg.getAttackerMap(),edge.getB(),edge.getA());
				rg.addMapping(rg.getAttackMap(),edge.getA(),edge.getB());
			}
		}
	}

	private void removeOldEdges(GraphInfo graph, ArrayList<Edge> oldE) {
		for(Edge edge: oldE){
			if(graph.getAtts().contains(edge)){
				removedEdges.add(edge);
				graph.getAtts().remove(edge);
				graph.getAttackers(edge.getB()).remove(edge.getA());
				graph.getAttacked(edge.getA()).remove(edge.getB());
			}
		}
	}

	private Edge edgeLookup(Edge e, AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> m) {
		ArrayList<AbstractMap.SimpleEntry<Edge, Edge>> value = m.getValue();

		for(AbstractMap.SimpleEntry<Edge, Edge> em: value){
			if(em.getKey().getA().getName().equals(e.getA().getName()) && em.getKey().getB().getName().equals(e.getB().getName())){
				return em.getValue();
			}
		}

		return new Edge(findMatchedVertex(e.getA(),m.getKey(),1),findMatchedVertex(e.getB(),m.getKey(),1),resultGraph);
	}

	/**
	 * finds the vertex that is matched to the given vertex
	 * @param v a vertex which's match is to be found
	 * @param matches all vertex matches
	 * @param side whether a graph or a pattern vertex is wanted
	 * 		1 - give graph vertex, 2 - give pattern vertex
	 * @return gives matched vertex of given type
	 */
	private Vertex findMatchedVertex(Vertex v, ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>> matches, int side) {
		for(AbstractMap.SimpleEntry<Vertex, Vertex> pairing: matches){
			if(side == 1 && pairing.getKey().getName().equals(v.getName())){
				return pairing.getValue();
			}
			else if(side == 2 && pairing.getValue().getName().equals(v.getName())){
				return pairing.getKey();
			}
		}

		return null;
	}

	/**
	 * deletes vertex from result as well as all incident edges
	 * @param graph 
	 * @param vm value says which vertex needs removing
	 * @param resultGraph result to be manipulated
	 */
	private void deleteVertex(GraphInfo graph, AbstractMap.SimpleEntry<Vertex, Vertex> vm) {
		Vertex rem = vm.getValue();
		removedVertices.add(rem);

		if(removedVertices.size()%1000==0){
			System.out.println(removedVertices.size());
		}

		resultGraph.getArgs().remove(rem);

		ArrayList<Edge> toRemove = new ArrayList<Edge>();

		for(Edge e : resultGraph.getAtts()){
			if(e.getA() == rem || e.getB() == rem){
				removedEdges.add(e);
				toRemove.add(e);
			}
		}

		//edge removal from hashmaps
		removeOldEdges(graph,toRemove); //removes dependencies
		resultGraph.getAttackerMap().remove(rem); //removes direct entries of rem
		resultGraph.getAttackMap().remove(rem); //removes direct entries of rem

		//direct attack removal
		resultGraph.getAtts().removeAll(toRemove);
	}

	private boolean toRetain(AbstractMap.SimpleEntry<Vertex, Vertex> vm) {
		if(replacement.vertexLookup(vm.getKey().getName()) != null){
			return true;
		}
		return false;
	}

	public MatchInfo getMatches() {
		return matches;
	}

	public void setMatches(MatchInfo matches) {
		this.matches = matches;
	}

	public GraphInfo getResultGraph() {
		return resultGraph;
	}

	public void setResultGraph(GraphInfo resultGraph) {
		this.resultGraph = resultGraph;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void printDebugMessage(String message){
		if(debug){
			System.out.println(message);
		}
	}

	public ArrayList<Vertex> getRemovedVertices() {
		return removedVertices;
	}

	public void setRemovedVertices(ArrayList<Vertex> removedVertices) {
		this.removedVertices = removedVertices;
	}

	public ArrayList<Edge> getRemovedEdges() {
		return removedEdges;
	}

	public void setRemovedEdges(ArrayList<Edge> removedEdges) {
		this.removedEdges = removedEdges;
	}

	public ArrayList<Edge> getAddedEdges() {
		return addedEdges;
	}

	public void setAddedEdges(ArrayList<Edge> addedEdges) {
		this.addedEdges = addedEdges;
	}

	public void printResult(boolean printGraph, boolean printReplaced) {
		System.out.println("The graph had " + replaced + " matches successfully applied!");

		System.out.println("It had " + removedVertices.size() + " vertices and " + removedEdges.size() + " edges removed.");
		System.out.println("There also were " + addedEdges.size() + " edges added!");

		if(printGraph){
			System.out.println("Resulting graph: ");

			resultGraph.printGraph();
		}
		if(printReplaced){
			printSamples(2);
		}
	}

	public int getReplaced() {
		return replaced;
	}

	public void setReplaced(int replaced) {
		this.replaced = replaced;
	}

	public void printVertexSet(ArrayList<Vertex> list){
		String printString = "";

		for(Vertex v: list){
			printString += v.getName() + ", ";
		}

		System.out.println(printString.substring(0, Math.max(0, printString.length() - 2)));
	}

	public int getSampleSize() {
		return sampleSize;
	}

	public void setSampleSize(int sampleSize) {
		this.sampleSize = sampleSize;
	}

	public List<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> getSample() {
		return sample;
	}

	public void setSample(List<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> sample) {
		this.sample = sample;
	}

	//type: 1 - samples, 2 - replaced
	private void printSamples(int type) {
		List<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> matchesToPrint;

		if(type == 1){
			matchesToPrint = sample;
		}
		else{
			matchesToPrint = replacedMatches;
		}

		for(AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>> instance: matchesToPrint){
			System.out.println("Printing sample: ");
			for(AbstractMap.SimpleEntry<Vertex, Vertex> verts: instance.getKey()){
				System.out.println(verts.getKey().getName() + " matched to " + verts.getValue().getName());
				System.out.println(verts.getValue().getName() + " attacks:");

				List<Vertex> atts = resultGraph.getAttacked(verts.getValue());

				if(atts != null && !atts.isEmpty()){
					for(Vertex v: atts){
						System.out.print(v.getName() + " ");
					}
				}

				System.out.println("");
				System.out.println(verts.getValue().getName() + " is attacked by:");

				List<Vertex> atkd = resultGraph.getAttackers(verts.getValue());

				if(atkd != null && !atkd.isEmpty()){
					for(Vertex v: atkd){
						System.out.print(v.getName() + " ");
					}
				}
				System.out.println("");
			}
			System.out.println("\n");
		}

	}

}
