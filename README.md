# ProgressiveFiltering

Progressive Filtering is an algorithm to solve instances of the Location Routing Problem. The algorithm is described in the paper "A progressive heuristic for the location-routing problem and variants" by Florian Arnold and Kenneth Sörensen (Computers & Operations Research, 2021).


## Usage

ProgressiveFiltering can be turned into a runnable .jar and started via the command line. The following arguments can be provided:

path/instance.json	The path to the instance to be solved. The instance format has to correspond to the the example in the input folder
-m	the maximal number of configurations to be considered in the first stage, e.g., "-m1000" would limit the number of configurations to 1000. Default value is 20.000
-t	the number of threads to be used, e.g., "-t4" to use four threads. Default value is 1.
-f	the definition of the filter stages seperated by a commata, e.g., "-f100,5,1" would signify that three filtering stages are applied after which 100, 5 and 1 configurations are left over. Default value is 100,10,3,1


## Requirements

All dependencies are provided in the libs folder as .jar files.
json-simple 	to read and write json files
log4j 		for logging
MDVRPSolver 	to solve MDVRP instances


## Authors

Progressive Filtering has been implemented by Florian Arnold during his time as PhD student at the Unversity of Antwerp. The algorithmic ideas have been developed together with Kenneth Sörensen.


## Contributing

We have provided the sorce code to allow students and researchers to adapt the code to their need. We welcome a short note in case the source code is actively used or changed, however, this is not mandantory.


## License

The software is for academic use only
