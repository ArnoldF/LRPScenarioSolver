
public class start {
	
	public static void main(String[] args) {
		
		LRPScenarios lrp = new LRPScenarios();
		readInputArgs(args, lrp);
		
		//lrp.addScenario("C:\\Users\\FArnold\\Desktop\\VRP instances\\LRP\\Instances_Schneider\\200-10-1e.json");	
		if (lrp.isValidation()) {
			lrp.validate();
			lrp.writeValidation();
		}
		else {
			lrp.solve(new Integer[]{100,10,3,1});
			lrp.writeToFile();
		}		
	}
	
	private static void readInputArgs(String[] args, LRPScenarios lrp) {
		boolean hasMinCapacity = false;
		for (String input: args) {
			if (input.charAt(0) == '-') {
				if (input.contains("v")) {
					lrp.setValidate();
				}
				if (input.contains("m")) {
					hasMinCapacity = true;
				}
				if (input.contains("t")) {
					lrp.settimeLimitPerIteration(Double.parseDouble(input.substring(2)));
				}
			}
			else if (lrp.isValidation() && input.substring(input.lastIndexOf('.') + 1).equals("sol")) {
				lrp.readSolution(input);
			}
			else if (hasMinCapacity && input.matches("-?\\d+")) {
				lrp.setMinCapacity(Integer.parseInt(input));
			}
			else if (input.substring(input.lastIndexOf('.') + 1).equals("json")) {
				lrp.addScenario(input);	
			}
		}
	}

	
}
