/*******************************************************************************
 * Copyright (c) 2015 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.framework.configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import dk.alexandra.fresco.framework.MPCException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.ProtocolEvaluator;
import dk.alexandra.fresco.framework.Reporter;
import dk.alexandra.fresco.framework.sce.configuration.ProtocolSuiteConfiguration;
import dk.alexandra.fresco.framework.sce.configuration.SCEConfiguration;
import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.framework.sce.evaluator.SequentialEvaluator;
import dk.alexandra.fresco.framework.sce.resources.storage.InMemoryStorage;
import dk.alexandra.fresco.framework.sce.resources.storage.Storage;
import dk.alexandra.fresco.framework.sce.resources.storage.StorageStrategy;
import dk.alexandra.fresco.framework.sce.resources.storage.StreamedStorage;
import dk.alexandra.fresco.suite.ProtocolSuite;
import dk.alexandra.fresco.suite.bgw.configuration.BgwConfiguration;
import dk.alexandra.fresco.suite.dummy.DummyConfiguration;
import dk.alexandra.fresco.suite.spdz.configuration.SpdzConfiguration;


/**
 * Utility for reading all configuration from command line.
 * 
 * A set of default configurations are used when parameters are not specified at runtime.
 * 
 */
public class CmdLineUtil {

	private final Options options;
	private CommandLine cmd;
	
	private SCEConfiguration sceConf;
	private ProtocolSuiteConfiguration psConf;
	
	public CmdLineUtil() {
		this.options = buildStandardOptions();
	
	}
	
	public SCEConfiguration getSCEConfiguration() {
		return this.sceConf;
	}
	
	public ProtocolSuiteConfiguration getProtocolSuiteConfiguration() {
		return this.psConf;
	}
	
	/**
	 * To be called after parse(). Returns remaining arguments that can be used for the specific application.
	 * 
	 * @return The arguments that are not for SCE and protocol suite.
	 * 
	 */
	public String[] getRemainingArgs() {
		return this.cmd.getArgs();
	}
	
	/**
	 * Adds standard options.
	 * 
	 * TODO: Move standard options to SCE.
	 * 
	 * For instance, options for setting player id and protocol suite.
	 * 
	 */
	private Options buildStandardOptions() {
		Options options = new Options();

		options.addOption(Option.builder("h")
				.desc("Displays this help message")
				.longOpt("help")
				.required(false)
				.hasArg(false)
				.build());
		
		options.addOption(Option.builder("i")
				.desc("The id of this player. Must be a unique positive integer.")
				.longOpt("id")
				.required(true)
				.hasArg()
				 //.type(Integer.class) // Dosn't work for some reason.
				.build());
		
		options.addOption(Option.builder("s")
				.desc("The name of the protocol suite to use. Must be one of these: "
						+ ProtocolSuite.getSupportedProtocolSuites() + ". " 
						+ "The default value is: bgw")
				.longOpt("suite")
				.required(true)
				.hasArg()
				.build());
		
		options.addOption(Option.builder("p")
			.desc("Connection data for a party. Use -p multiple times to specify many players. You must always at least include yourself."
					+ "Must be on the form [id]:[hostname]:[port] or [id]:[hostname]:[port]:[shared key]. "
					+ "id is a unique positive integer for the player, host and port is where to find the player, "
					+ " shared key is an optional string defining a secret key that is shared by you and the other player "
					+ " (the other player must submit the same key for you as you do for him). "
					)
			.longOpt("party")
			.required(true)
			.hasArgs()
			.build());

		
		options.addOption(Option.builder("l")
				.desc("The log level. Can be either OFF, SEVERE, CONFIG, WARNING, INFO, FINE, FINER, FINEST. Default is 'WARNING'")
				.longOpt("log-level")
				.required(false)
				.hasArg()
				.build());
		
		options.addOption(Option.builder("t")
				.desc("The number of threads to use for the SCE. Defaults to " + getDefaultNoOfThreads())
				.longOpt("no-threads")
				.required(false)
				.hasArg(true)
				.build());
		
		options.addOption(Option.builder("vt")
				.desc("The number of threads to use in the VM (evaluator). Defaults to " + getDefaultNoOfThreads())
				.longOpt("no-vm-threads")
				.required(false)
				.hasArg(true)
				.build());
		
		options.addOption(Option.builder("e")
				.desc("The strategy for evaluation. Can be one of: " + Arrays.toString(EvaluationStrategy.values()) + ". Defaults to " + EvaluationStrategy.SEQUENTIAL)
				.longOpt("evaluator")
				.required(false)
				.hasArg(true)
				.build());
		
		options.addOption(Option.builder("b")
				.desc("The maximum number of native protocols kept in memory at any point in time. Defaults to 4096")
				.longOpt("max-batch")
				.required(false)
				.hasArg(true)
				.build());
		
		return options;
	}
	
	
	private int getDefaultNoOfThreads() {
		int n = Runtime.getRuntime().availableProcessors();
		if (n==1) return 1;
		return n-1; // Heuristic that gives best performance: One thread for each worker and one for the 'system'.
	}
	
	private int parseNonzeroInt(String optionId) throws ParseException {
		int res;
		String opStr = this.cmd.getOptionValue(optionId);
		if (opStr == null) throw new ParseException("No value for option: " + optionId);
		try {
			res  = Integer.parseInt(opStr);
			if (res < 0)
				throw new ParseException(optionId + " must be a positive integer");
		} catch (NumberFormatException e) {
			throw new ParseException("Cannot parse '" + this.cmd.getOptionValue(optionId) + "' as an integer");
		}
		return res;
	}

	private void validateStandardOptions() throws ParseException {
		
		int myId;
		Level logLevel;
		
		Object suiteObj = this.cmd.getParsedOptionValue("s");
		if (suiteObj == null) throw new ParseException("Cannot parse '" + this.cmd.getOptionValue("s") + "' as a string");
		
		
		final Map<Integer,Party> parties = new HashMap<Integer,Party>();
		final String suite = (String) suiteObj;
		
		
		if (!ProtocolSuite.getSupportedProtocolSuites().contains(suite.toLowerCase()))
			throw new ParseException("Unknown protocol suite: " + suite);
		
		myId = parseNonzeroInt("i");
		if (myId == 0) throw new ParseException("Player id must be positive, non-zero integer");
		
		for (String pStr : this.cmd.getOptionValues("p")) {
			String[] p = pStr.split(":");
			if (p.length < 3 || p.length > 4)
				throw new ParseException("Could not parse '" + pStr + "' as [id]:[host]:[port] or [id]:[host]:[port]:[shared key]");
			try {
				int id = Integer.parseInt(p[0]);
				InetAddress.getByName(p[1]); // Check that hostname is valid.
				int port = Integer.parseInt(p[2]);
				Party party;
				if (p.length == 3) {
					party = new Party(id, p[1], port);
				} else {
					party = new Party(id, p[1], port, p[3]);
				}
				if (parties.containsKey(id))
					throw new ParseException("Player ids must be unique");
				parties.put(id, party);
			} catch (NumberFormatException | UnknownHostException e) {
				throw new ParseException("Could not parse '" + pStr + "': " + e.getMessage());
			}
		}
		if (!parties.containsKey(myId))
			throw new ParseException("You must at least include yourself in the list of players");
		

		if (this.cmd.hasOption("l")) {
			System.out.println(this.cmd.getOptionValue("l"));
			logLevel = Level.parse(this.cmd.getOptionValue("l"));
		} else {
			logLevel = Level.WARNING;
		}
		Reporter.init(logLevel);
		

		int noOfThreads = this.cmd.hasOption("t") ? parseNonzeroInt("t") : getDefaultNoOfThreads();
		if (noOfThreads > Runtime.getRuntime().availableProcessors())
			Reporter.warn("You are using " + noOfThreads + " but system has only " + Runtime.getRuntime().availableProcessors()
					+ " available processors. This is likely to result in less than optimal performance.");
		
		int vmThreads = this.cmd.hasOption("vt") ? parseNonzeroInt("vt") : getDefaultNoOfThreads();
		if (vmThreads > Runtime.getRuntime().availableProcessors())
			Reporter.warn("You are using " + vmThreads + " but system has only " + Runtime.getRuntime().availableProcessors()
					+ " available processors. This is likely to result in less than optimal performance.");
		
		final ProtocolEvaluator evaluator;
		if (this.cmd.hasOption("e")) {
			try {
			evaluator = EvaluationStrategy.fromString(this.cmd.getOptionValue("e"));
			} catch (ConfigurationException e) {
				throw new ParseException("Invalid evaluation strategy: " + this.cmd.getOptionValue("e"));
			}
		} else {
			evaluator = new SequentialEvaluator();
		}
		
		final Storage storage;
		if(this.cmd.hasOption("store")) {
			try {
				storage = StorageStrategy.fromString(this.cmd.getOptionValue("store"));
			} catch(ConfigurationException e) {
				throw new ParseException("Invalid storage strategy: " + this.cmd.getOptionValue("store"));
			}
		} else {
			storage = new InMemoryStorage();
		}
		
		final int maxBatchSize;
		if(this.cmd.hasOption("b")) {
			try {
				maxBatchSize = Integer.parseInt(this.cmd.getOptionValue("b"));
			} catch(Exception e) {
				throw new ParseException("");
			}
		} else {
			maxBatchSize = 4096;
		}
		
		
		// TODO: Rather: Just log sceConf.toString()
		Reporter.config("Player id          : " + myId);
		Reporter.config("Protocol suite     : " + suite);
		Reporter.config("Players            : " + parties);
		Reporter.config("Log level          : " + logLevel);
		Reporter.config("No of threads      : " + noOfThreads);
		Reporter.config("No of vm threads   : " + vmThreads);
		Reporter.config("Evaluation strategy: " + evaluator);
		Reporter.config("Storage strategy   : " + storage);
		Reporter.config("Maximum batch size : " + maxBatchSize);
		
		this.sceConf = new SCEConfiguration() {

				@Override
				public int getMyId() {
					return myId;
				}
				
				@Override
				public String getProtocolSuiteName() {
					return suite;
				}

				@Override
				public Map<Integer, Party> getParties() {
					return parties;
				}

				@Override
				public Level getLogLevel() {
					return logLevel;
				}

				@Override
				public int getNoOfThreads() {
					return noOfThreads;
				}
				
				@Override
				public int getNoOfVMThreads() {
					return vmThreads;
				}
				
				@Override
				public ProtocolEvaluator getEvaluator() {
					return evaluator;
				}

				@Override
				public Storage getStorage() {
					return storage;
				}

				@Override
				public int getMaxBatchSize() {
					return maxBatchSize;
				}

				@Override
				public StreamedStorage getStreamedStorage() {
					if(storage instanceof StreamedStorage) {
						return (StreamedStorage) storage;
					} else{
						return null;
					}
				}
			};

	}
	
	
	/**
	 * For adding application specific options.
	 * 
	 */
	public void addOption(Option option) {
		// TODO: Rather, application specific options.
		this.options.addOption(option);
	}
	
	

	
	public CommandLine parse(String[] args) {
		//System.out.println("Got args: " + Arrays.toString(args));
		try {
			CommandLineParser parser = new DefaultParser();
			this.cmd = parser.parse(options, args);
			
			if (cmd.hasOption("h")) {
				displayHelp();
				System.exit(0);
			}

			
			validateStandardOptions();

			
			
			
			// TODO: Do this without hardcoding the protocol suite names here.
			String[] remainingArgs = cmd.getArgs();
			switch (this.sceConf.getProtocolSuiteName()) {
			case "bgw":
				this.psConf = BgwConfiguration.fromCmdArgs(this.sceConf, remainingArgs);
				break;
			case "dummy":
				this.psConf = DummyConfiguration.fromCmdArgs(this.sceConf, remainingArgs);
				break;
			case "spdz":
				this.psConf = SpdzConfiguration.fromCmdArgs(this.sceConf, remainingArgs);
				break;
			default:
				throw new MPCException("Unknown protocol suite: " + this.getSCEConfiguration().getProtocolSuiteName());
			}
	

//			
//			if (cmd.hasOption("v")) {
//				log.log(Level.INFO, "Using cli argument -v=" + cmd.getOptionValue("v"));
//				// Whatever you want to do with the setting goes here
//			} else {
//				log.log(Level.SEVERE, "MIssing v option");
//				help();
//			}

			
			
//			// Parse input from cmd line
//			// TODO: We want to run both player 1 and 2 from same place, so we override player id.
//			int myId = Integer.parseInt(args[0]);
//			boolean[] in = null;
//			if (args[1].length() != 16)
//				throw new IllegalArgumentException("bad input hex string or player id");
//			if (myId == 1 || myId == 2) {
//				in = toBoolean(args[1]);
//			}
//			// Do the secure computation using config from property files.
//			CmdLineUtil aes = new CmdLineUtil(myId, in);
//			SCE sce = SCE.getInstance(myId);
//			sce.runApplication(aes);
//			
//			// Print result.
//			System.out.println("The resulting ciphertext is: " + Arrays.toString(aes.result));
			
		} catch (ParseException e) {
			System.out.println("Error while parsing arguments: " + e.getLocalizedMessage());
			System.out.println();
			displayHelp();
			System.exit(-1); // TODO: Consider moving to top level.
			
		}
		return this.cmd;


	}


	

	
	
	public void displayHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("general options are:", this.options);
	}
	

	
}
