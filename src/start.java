
public class start {
	
	public static void main(String[] args) {
		
		LRPScenarios lrp = new LRPScenarios();
		for (String path: args)		
			lrp.addScenario(path);		
		lrp.solve(new Integer[]{100,10,3,1});
		lrp.writeToFile();
	}

	
}
