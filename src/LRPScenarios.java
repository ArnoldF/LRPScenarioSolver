import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.log4j.Logger;
import MDVRP.MDVRPModel;
import MDVRP.Node;


public class LRPScenarios {
	
	static final int RUNTIME = 60000;
	
	private ArrayList<MDVRPModel> customerScenarios;
	private ArrayList<Integer> demandPerScenario;
	private List<DepotOption> possibleDepots;
	private int numPossibleDepots;
	private ArrayList<DepotConfiguration> remainingConfigurations;
	private static Logger logger = Logger.getLogger(LRPScenarios.class);
	
	
	public LRPScenarios() {
		
		customerScenarios = new ArrayList<MDVRPModel>();
		demandPerScenario = new ArrayList<Integer>();
	}
	
	
	public void addScenario(String path) {
		
		try (FileReader reader = new FileReader(path)) {
			JSONParser parser = new JSONParser();
			JSONObject jsonObject = (JSONObject) parser.parse(reader);
			
			MDVRPModel mdvrp = new MDVRPModel(jsonObject);		
			//ASSUMPTION: ALL SCENARIOS HAVE THE SAME DEPOTS
			if (possibleDepots == null || possibleDepots.isEmpty())
				loadPossibleDepots(jsonObject);
			numPossibleDepots = possibleDepots.size();
			
			int sumDemand = Arrays.stream(mdvrp.nodes)
				.mapToInt(n -> n.demand)
				.sum();
			
			customerScenarios.add(mdvrp);
			demandPerScenario.add(sumDemand);
			logger.info("Scenario at " + path + " has been added." );
				
		} catch (IOException e) {
			logger.error("The scenario " + path + " could not be found. Check path.");
	    } catch (ParseException e) {
	    	logger.error("The scenario did not match the LRP format.");
	    }	
	}
	
		
	private void loadPossibleDepots(JSONObject jsonObj) throws ParseException {
		
		possibleDepots = new ArrayList<DepotOption>();
		JSONArray depotInfo = (JSONArray) jsonObj.get("depots");     
	    Iterator<Object> iterator = depotInfo.iterator();
	    int index = 0;
	    while (iterator.hasNext()) {
	    	JSONObject nextEntry = (JSONObject) iterator.next();
	    	int capacity = ((Long) nextEntry.get("capacity")).intValue();
	    	double cost = ((Long) nextEntry.get("costs")).doubleValue();
	    	double x = ((Long) nextEntry.get("x")).doubleValue();
	    	double y = ((Long) nextEntry.get("y")).doubleValue();
	    	possibleDepots.add(new DepotOption(index, x, y, cost, capacity));
	    	index++;
	    }
	}
	
	
	public void solve(Integer[] filterStages) {
		
		try {
			int upperBoundOpenDepot = 0;
			for (int scenarioID = 0; scenarioID < customerScenarios.size(); scenarioID++){
				upperBoundOpenDepot = Math.max(upperBoundOpenDepot, computeUpperBoundOpenDepots(scenarioID));
			}
			logger.info("Upper bound of open depots has been computed as " +upperBoundOpenDepot + ".");
			
			generateAllDepotConfigurations(upperBoundOpenDepot);
			logger.info("All relevant depot configurations have been build.");
			
			applyFilters(filterStages);
			logger.info("Optimation completed.");
		}
		catch (Exception e) {
			logger.error("An error occured during optimisation.");
		}
	}
	
	
	private int computeUpperBoundOpenDepots(int scenarioID) {
		
		MDVRPModel mdvrp = customerScenarios.get(scenarioID);
		
		List<Integer> sortedCapacity = possibleDepots.stream()
				.map(d -> d.getCapacity())
				.sorted(Comparator.reverseOrder())
				.collect(Collectors.toList());
				
		//determine minimal number of open depots, given limited depot capacity
		int sumCapacity = 0;
		int minOpenDepots = 0;
		while (minOpenDepots < numPossibleDepots && sumCapacity < demandPerScenario.get(scenarioID)){
			sumCapacity += sortedCapacity.get(minOpenDepots);
			minOpenDepots++;
		}
		
		//determine most central depot
		double shortestDistance = Double.MAX_VALUE;
		DepotOption centralDepot = null;
		for (DepotOption d: possibleDepots) {
			double sumDistance = 0;
			for (Node n: mdvrp.nodes) {
				sumDistance += (float) Math.sqrt( Math.pow( n.x - d.getX() , 2) + Math.pow( n.y - d.getY() , 2) );				
			}
			if (sumDistance < shortestDistance) {
				centralDepot = d;
				shortestDistance = sumDistance;
			}
		}
		
		//estimate routing costs from central depot
		mdvrp.setDepots(new DepotConfiguration(centralDepot).toMDVRPNodes());
		mdvrp.depotInventory[0][0] = Integer.MAX_VALUE;
		double r1 = mdvrp.constructStartingSolution();
		
		List<Double> sortedOpeningCosts = possibleDepots.stream()
				.map(d -> d.getOpeningCost())
				.sorted(Comparator.reverseOrder())
				.collect(Collectors.toList());	
		int minNumRoutes = (int)Math.ceil((double)demandPerScenario.get(scenarioID)/mdvrp.capacityLimitVehicle);
		
		int upperBound = 2;
		double costReduction = r1 * (reduction(upperBound, minNumRoutes) - reduction(upperBound-1, minNumRoutes));
		while(upperBound < numPossibleDepots && costReduction > sortedOpeningCosts.get(upperBound-1)) {
			upperBound++;
			costReduction = r1 * (reduction(upperBound, minNumRoutes) - reduction(upperBound-1, minNumRoutes));
		}
		
		return Math.max(upperBound, minOpenDepots);
	}
	
	
	private double reduction(int M, int minNumRoutes) {
		
		double val = Math.pow(Math.log((double)M), 0.58) * 0.27 * Math.pow(minNumRoutes / 10.0, 1.0 / 3);
		return val;
	}
	
	
	private void generateAllDepotConfigurations(int upperBoundOpenDepots) {
		
		remainingConfigurations = new ArrayList<DepotConfiguration>();
		for (int numOpenDepots = 1; numOpenDepots <= upperBoundOpenDepots; numOpenDepots++) {
			int[] current_depot_option = new int[numOpenDepots];
			for (int i=0; i<numOpenDepots; i++)
				current_depot_option[i] = i;
			while (true) {			
				DepotConfiguration currentDepotConfig = new DepotConfiguration();
				for (int i=0; i<numOpenDepots; i++) {
					currentDepotConfig.add(possibleDepots.get(current_depot_option[i]));	
				}
				if (evaluateDepotConfiguration(currentDepotConfig, 0)){//TODO pass threshold as parameter to abort CW early
					remainingConfigurations.add(currentDepotConfig);
				}
	
				int posToChange = numOpenDepots - 1;
				while ( posToChange >= 0 && current_depot_option[posToChange] == numPossibleDepots - (numOpenDepots - posToChange))
					posToChange--;
				if (posToChange == -1)
					break;
				current_depot_option[ posToChange ]++;
				int resetValue = current_depot_option[ posToChange ];
				for (int i=posToChange+1; i<numOpenDepots; i++)
					current_depot_option[ i ] = resetValue + i - posToChange;				
			}
		}
		Collections.sort(remainingConfigurations);
		logger.info("Best objective value: " + remainingConfigurations.get(0).costs);
	}		
	
	
	private boolean evaluateDepotConfiguration(DepotConfiguration depotConfig, int maxIterations) {
		
		for (Integer demand : demandPerScenario) {		
			if (!depotConfig.hasSufficientCapacity(demand))
				return false;
		}
		
		ArrayList<SolverThread> threads = new ArrayList<SolverThread>();
		for (MDVRPModel mdvrp : customerScenarios) {
			SolverThread thread = new SolverThread(mdvrp, depotConfig.toMDVRPNodes(), maxIterations);
			threads.add(thread);
		}
		
		boolean isRunning = true;
		for (SolverThread thread : threads) {
			thread.start();
		}		
		
		while (isRunning) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//validate whether all threads are finished
			isRunning = false;
			for (Thread thread : threads) {
				if (thread.isAlive()){
					isRunning = true;
					break;
				}				
			}
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		double routingCost = 0;
		for (MDVRPModel mdvrp : customerScenarios) {
			routingCost += mdvrp.computeSolutionCosts();
		}
		depotConfig.setCosts(routingCost + depotConfig.getOpeningCosts());
		return true;
	}
	
	
	private void applyFilters(Integer[] filterStages) {
		
		for (int numCap: filterStages) {
			reduceRemainingOptions(numCap);
			double timeLimit = RUNTIME / (100 * filterStages.length * numCap);
			int maxIterations = (int)Math.ceil(timeLimit * 10);
			if (numCap == 1)
				maxIterations = 3000;
			evaluateDepotConfigurations(maxIterations);
			logger.info("The best " + numCap + " configurations have been evaluated. Best objective value: " + remainingConfigurations.get(0).costs);
		}		
	}
	

	private void reduceRemainingOptions(int maxOptions)	{
		
		Collections.sort(remainingConfigurations);
		while (remainingConfigurations.size() > maxOptions) {
			remainingConfigurations.remove(remainingConfigurations.size()-1);
		}
	}


	private void evaluateDepotConfigurations(int maxIterations) {
		
		for (DepotConfiguration config: remainingConfigurations) {		
			if (!evaluateDepotConfiguration(config, maxIterations))
				logger.error("Depot Configuration does not sufficient capacity in optimisation stage");
			//TODO checkINventoryConstraints();
		}
	}
		
	
	public void writeToFile() {
		
		JSONObject output = new JSONObject();
		output.put("Objective", remainingConfigurations.get(0).costs);
		output.put("NumOpenDepots", remainingConfigurations.get(0).openDepots.size());

	    JSONArray openDepots = new JSONArray();
	    for (DepotOption depot : remainingConfigurations.get(0).openDepots) {
	    	openDepots.add(depot.getIndex());
	    }
	    output.put("OpenDepots", openDepots);
	    
	    Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH_mm_ss");

	    try (FileWriter file = new FileWriter("." + File.separator +"output" + File.separator + "Result_"+sdf.format(cal.getTime())+".json")) {
	        file.write(output.toJSONString());
	    } catch (IOException e) {
	    	logger.error("Output File could not be written. Check whether the output folder exists.");
	    }
	}
	
	
	class SolverThread extends Thread {
		
		MDVRPModel mdvrp;
		ArrayList<Node> depotConfiguration;
		int maxIterations;

		public SolverThread(MDVRPModel mdvrp, ArrayList<Node> depotConfiguration, int maxIterations) {
			this.mdvrp = mdvrp;
			this.depotConfiguration = depotConfiguration;
			this.maxIterations = maxIterations;
		}
		
        @Override
        public void run() {
            try {
            	mdvrp.setDepots(depotConfiguration);
            	mdvrp.constructStartingSolution();
            	mdvrp.checkSolution();
        		if (maxIterations > 0) {
        			 mdvrp.optimizeRoutes(maxIterations, Double.MAX_VALUE, false);	            			 
        		}
            }catch (Exception ex){
                ex.printStackTrace();}
        }
	}
    


}
