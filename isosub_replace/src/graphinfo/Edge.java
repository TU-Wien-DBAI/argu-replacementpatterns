package graphinfo;

public class Edge {

	private Vertex a,b;
	private GraphInfo graph;

	public Edge(Vertex a, Vertex b, GraphInfo graph){
		this.a = a;
		this.b = b;
		this.graph = graph;
	}

	public Vertex getA() {
		return a;
	}

	public void setA(Vertex a) {
		this.a = a;
	}

	public Vertex getB() {
		return b;
	}

	public void setB(Vertex b) {
		this.b = b;
	}

	public GraphInfo getGraph() {
		return graph;
	}

	public void setGraph(GraphInfo graph) {
		this.graph = graph;
	}

	@Override
	public boolean equals(Object e){
		if(e instanceof Edge){
			if(((Edge) e).getA() == this.getA() && ((Edge) e).getB() == this.getB()){
				return true;
			}
		}
		return false;
	}

}
