package bbgraph;

import java.util.AbstractMap;
import java.util.ArrayList;

import graphinfo.Edge;
import graphinfo.GraphInfo;
import graphinfo.Vertex;

public class MatchInfo {

	private GraphInfo graph;
	private GraphInfo query;
	private ArrayList<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> matched;
	
	public MatchInfo(GraphInfo graph, GraphInfo query,
			ArrayList<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> matched) {
		this.setGraph(graph);
		this.setQuery(query);
		this.setMatched(matched);
	}

	public GraphInfo getGraph() {
		return graph;
	}

	public void setGraph(GraphInfo graph) {
		this.graph = graph;
	}

	public GraphInfo getQuery() {
		return query;
	}

	public void setQuery(GraphInfo query) {
		this.query = query;
	}

	public ArrayList<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> getMatched() {
		return matched;
	}

	public void setMatched(ArrayList<AbstractMap.SimpleEntry<ArrayList<AbstractMap.SimpleEntry<Vertex, Vertex>>, ArrayList<AbstractMap.SimpleEntry<Edge, Edge>>>> matched) {
		this.matched = matched;
	}

	public String getGraphStringRep(GraphInfo g) {
		GraphInfo tmp;
		String rep = "";
		
		if(g.equals(graph)){
			tmp=graph;
		}
		else if(g.equals(query)){
			tmp=query;
		}
		else{
			return rep;
		}
		
		for(Vertex v:tmp.getArgs()){
			rep += "arg("+v.getName()+").\n";
		}
		for(Edge e:tmp.getAtts()){
			/*if(e.getA().getName().equals("a117") || e.getB().getName().equals("a219")) {
				System.out.println("into file: " + e.getA().getName() + ", " + e.getB().getName());
			}*/
			
			rep += "att("+e.getA().getName()+","+e.getB().getName()+").\n";
		}
		
		return rep;
	}

}
