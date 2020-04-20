import java.util.ArrayList;

import MDVRP.Node;

public class DepotConfiguration implements Comparable<DepotConfiguration> {
	
	ArrayList<DepotOption> openDepots;
	double costs;
	
	
	public DepotConfiguration() {
		openDepots = new ArrayList<DepotOption>();
	}
	
	
	public DepotConfiguration(DepotOption depot) {
		openDepots = new ArrayList<DepotOption>();
		openDepots.add(depot);
	}
	
	
	public void setCosts(double costs) {
		this.costs = costs;
	}
	
	
	public boolean hasSufficientCapacity(int demand) {
		int sumCapacity = openDepots.stream()
				.mapToInt(DepotOption::getCapacity)
				.sum();	
		return sumCapacity >= demand;
	}
	
	
	public void add(DepotOption depot) {
		openDepots.add(depot);
	}
	
	
	public ArrayList<Node> toMDVRPNodes() {		
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		for (DepotOption depot: openDepots) {
			nodes.add(depot.toMDVRPNode().copy());
		}
		return nodes;
	}
	
	
	public double getOpeningCosts() {
		return openDepots.stream()
				.mapToDouble(DepotOption::getOpeningCost)
				.sum();		
	}

	
	public int compareTo(DepotConfiguration otherConfig) {
		return Double.compare(costs, otherConfig.costs);
	}

}
