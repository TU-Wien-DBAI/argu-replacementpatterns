package graphinfo;

public class Vertex {
	
	private String name;
	private boolean core;
	private boolean prohibitedIn;
	private boolean prohibitedOut;
	
	public Vertex(String name){
		this.name = name;
		this.setCore(false);
		this.setPin(false);
		this.setPout(false);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isCore() {
		return core;
	}

	public void setCore(boolean core) {
		this.core = core;
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof Vertex && ((Vertex) other).getName().equals(name)){
			return true;
		}
		return false;
	}

	public void setPin(boolean b) {
		prohibitedIn = b;
	}
	
	public boolean getPin(){
		return prohibitedIn;
	}
	
	public void setPout(boolean b) {
		prohibitedOut = b;
	}
	
	public boolean getPout(){
		return prohibitedOut;
	}
	
}
