import java.util.ArrayList;
import java.util.HashMap;

import MDVRP.Node;

public class DepotOption {
	
	private int index;
	private double x;
	private double y;
	private double openingCost;
	private int capacity;
	
	
	public DepotOption(int index, double x, double y, double openingCost, int capacity) {
		this.index = index;
		this.x = x;
		this.y = y;
		this.openingCost = openingCost;
		this.capacity = capacity;
	}
	
	
	public double getOpeningCost() {
		return openingCost;
	}
	
	
	public double getX() {
		return x;
	}
	
	
	public double getY() {
		return y;
	}
	
	
	public int getIndex() {
		return index;
	}
	
	
	public int getCapacity() {
		return capacity;
	}
		
	
	public Node toMDVRPNode() {
		Node node = new Node(-1);
		node.x = x;
		node.y = y;
		node.demand = capacity;
		node.demandPerProduct = new int[]{capacity};
		return node;
	}

}
