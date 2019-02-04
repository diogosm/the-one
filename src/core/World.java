/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import input.EventQueue;
import input.ExternalEvent;
import input.ScheduledUpdatesQueue;

import java.util.*;

import org.apache.commons.collections15.Transformer;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import core.Graph_Algos;


/**
 * World contains all the nodes and is responsible for updating their
 * location and connections.
 */
public class World {
	/** name space of optimization settings ({@value})*/
	public static final String OPTIMIZATION_SETTINGS_NS = "Optimization";

	/**
	 * Should the order of node updates be different (random) within every
	 * update step -setting id ({@value}). Boolean (true/false) variable.
	 * Default is @link {@link #DEF_RANDOMIZE_UPDATES}.
	 */
	public static final String RANDOMIZE_UPDATES_S = "randomizeUpdateOrder";
	/** should the update order of nodes be randomized -setting's default value
	 * ({@value}) */
	public static final boolean DEF_RANDOMIZE_UPDATES = true;

	/**
	 * Should the connectivity simulation be stopped after one round
	 * -setting id ({@value}). Boolean (true/false) variable.
	 */
	public static final String SIMULATE_CON_ONCE_S = "simulateConnectionsOnce";

	private int sizeX;
	private int sizeY;
	private List<EventQueue> eventQueues;
	private double updateInterval;
	private SimClock simClock;
	private double nextQueueEventTime;
	private EventQueue nextEventQueue;
	/** list of nodes; nodes are indexed by their network address */
	private List<DTNHost> hosts;
	private boolean simulateConnections;
	/** nodes in the order they should be updated (if the order should be
	 * randomized; null value means that the order should not be randomized) */
	private ArrayList<DTNHost> updateOrder;
	/** is cancellation of simulation requested from UI */
	private boolean isCancelled;
	private List<UpdateListener> updateListeners;
	/** Queue of scheduled update requests */
	private ScheduledUpdatesQueue scheduledUpdates;
	private boolean simulateConOnce;

	public static Graph_Algos GA1;
	public static LinkedList<String> Distinct_Vertex;
	public static LinkedList<String> Source_Vertex;
	public static LinkedList<String> Target_Vertex;
	public static LinkedList<Double> Edge_Weight;
	public static HashMap<String, ArrayList<String> > mappedEdges;
	public static boolean grafo[][];
	public static int numNodes;
	//lista de LCC pra cada node
	public static ArrayList<Double> LCC;

	/**
	 * Constructor.
	 */
	public World(List<DTNHost> hosts, int sizeX, int sizeY,
			double updateInterval, List<UpdateListener> updateListeners,
			boolean simulateConnections, List<EventQueue> eventQueues) {
		this.hosts = hosts;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.updateInterval = updateInterval;
		this.updateListeners = updateListeners;
		this.simulateConnections = simulateConnections;
		this.eventQueues = eventQueues;

		this.simClock = SimClock.getInstance();
		this.scheduledUpdates = new ScheduledUpdatesQueue();
		this.isCancelled = false;

		setNextEventQueue();
		initSettings();

		GA1 = new Graph_Algos();
		Distinct_Vertex = new LinkedList<String>();
		Source_Vertex = new LinkedList<String>();
		Target_Vertex = new LinkedList<String>();
		Edge_Weight = new LinkedList<Double>();
		mappedEdges = new HashMap<String, ArrayList<String>>();

		for(DTNHost host : this.hosts){
			Distinct_Vertex.add(host.toString());
		}

		numNodes = this.hosts.size();
		grafo = new boolean[this.hosts.size()][this.hosts.size()];
		for(int i = 0;i<this.hosts.size();i++)
			for(int j = 0;j<this.hosts.size();j++)
				grafo[i][j] = false;

		LCC = new ArrayList<Double>();
		for(int i = 0;i<this.hosts.size();i++) LCC.add(0.0);
	}

	/**
	 * Initializes settings fields that can be configured using Settings class
	 */
	private void initSettings() {
		Settings s = new Settings(OPTIMIZATION_SETTINGS_NS);
		boolean randomizeUpdates = DEF_RANDOMIZE_UPDATES;

		if (s.contains(RANDOMIZE_UPDATES_S)) {
			randomizeUpdates = s.getBoolean(RANDOMIZE_UPDATES_S);
		}
		simulateConOnce = s.getBoolean(SIMULATE_CON_ONCE_S, false);

		if(randomizeUpdates) {
			// creates the update order array that can be shuffled
			this.updateOrder = new ArrayList<DTNHost>(this.hosts);
		}
		else { // null pointer means "don't randomize"
			this.updateOrder = null;
		}
	}

	/**
	 * Moves hosts in the world for the time given time initialize host
	 * positions properly. SimClock must be set to <CODE>-time</CODE> before
	 * calling this method.
	 * @param time The total time (seconds) to move
	 */
	public void warmupMovementModel(double time) {
		if (time <= 0) {
			return;
		}

		while(SimClock.getTime() < -updateInterval) {
			moveHosts(updateInterval);
			simClock.advance(updateInterval);
		}

		double finalStep = -SimClock.getTime();

		moveHosts(finalStep);
		simClock.setTime(0);
	}

	/**
	 * Goes through all event Queues and sets the
	 * event queue that has the next event.
	 */
	public void setNextEventQueue() {
		EventQueue nextQueue = scheduledUpdates;
		double earliest = nextQueue.nextEventsTime();

		/* find the queue that has the next event */
		for (EventQueue eq : eventQueues) {
			if (eq.nextEventsTime() < earliest){
				nextQueue = eq;
				earliest = eq.nextEventsTime();
			}
		}

		this.nextEventQueue = nextQueue;
		this.nextQueueEventTime = earliest;
	}

	/**
	 * Update (move, connect, disconnect etc.) all hosts in the world.
	 * Runs all external events that are due between the time when
	 * this method is called and after one update interval.
	 */
	public void update () {
		double runUntil = SimClock.getTime() + this.updateInterval;

		setNextEventQueue();

		/* process all events that are due until next interval update */
		while (this.nextQueueEventTime <= runUntil) {
			simClock.setTime(this.nextQueueEventTime);
			ExternalEvent ee = this.nextEventQueue.nextEvent();
			ee.processEvent(this);
			updateHosts(); // update all hosts after every event
			setNextEventQueue();
		}

		moveHosts(this.updateInterval);
		simClock.setTime(runUntil);

		updateHosts();

		/* inform all update listeners */
		for (UpdateListener ul : this.updateListeners) {
			ul.updated(this.hosts);
		}
	}

	/**
	 * Updates all hosts (calls update for every one of them). If update
	 * order randomizing is on (updateOrder array is defined), the calls
	 * are made in random order.
	 */
	private void updateHosts() {
		if (this.updateOrder == null) { // randomizing is off
			for (int i=0, n = hosts.size();i < n; i++) {
				if (this.isCancelled) {
					break;
				}
				hosts.get(i).update(simulateConnections);
			}
		}
		else { // update order randomizing is on
			assert this.updateOrder.size() == this.hosts.size() :
				"Nrof hosts has changed unexpectedly";
			Random rng = new Random(SimClock.getIntTime());
			Collections.shuffle(this.updateOrder, rng);
			for (int i=0, n = hosts.size();i < n; i++) {
				if (this.isCancelled) {
					break;
				}
				this.updateOrder.get(i).update(simulateConnections);
			}
		}

		if (simulateConOnce && simulateConnections) {
			simulateConnections = false;
		}
	}

	/**
	 * Moves all hosts in the world for a given amount of time
	 * @param timeIncrement The time how long all nodes should move
	 */
	private void moveHosts(double timeIncrement) {
		for (int i=0,n = hosts.size(); i<n; i++) {
			DTNHost host = hosts.get(i);
			host.move(timeIncrement);
		}
	}

	/**
	 * Asynchronously cancels the currently running simulation
	 */
	public void cancelSim() {
		this.isCancelled = true;
	}

	/**
	 * Returns the hosts in a list
	 * @return the hosts in a list
	 */
	public List<DTNHost> getHosts() {
		return this.hosts;
	}

	/**
	 * Returns the x-size (width) of the world
	 * @return the x-size (width) of the world
	 */
	public int getSizeX() {
		return this.sizeX;
	}

	/**
	 * Returns the y-size (height) of the world
	 * @return the y-size (height) of the world
	 */
	public int getSizeY() {
		return this.sizeY;
	}

	/**
	 * Returns a node from the world by its address
	 * @param address The address of the node
	 * @return The requested node or null if it wasn't found
	 */
	public DTNHost getNodeByAddress(int address) {
		if (address < 0 || address >= hosts.size()) {
			throw new SimError("No host for address " + address + ". Address " +
					"range of 0-" + (hosts.size()-1) + " is valid");
		}

		DTNHost node = this.hosts.get(address);
		assert node.getAddress() == address : "Node indexing failed. " +
			"Node " + node + " in index " + address;

		return node;
	}

	/**
	 * Schedules an update request to all nodes to happen at the specified
	 * simulation time.
	 * @param simTime The time of the update
	 */
	public void scheduleUpdate(double simTime) {
		scheduledUpdates.addUpdate(simTime);
	}

	public static void addGrafo(DTNHost host, DTNHost otherHost) {
		ArrayList<String> aux;

		//add true no grafo
		//Debug.p("[" + host.getAddress() + "] <--> [" + otherHost.getAddress() + "]");
		grafo[host.getAddress()][otherHost.getAddress()] = true;

		if(mappedEdges.containsKey(host.toString())){
			aux = mappedEdges.get(host.toString());

			for(String node : aux){
				if(otherHost.toString() == node){
					//Debug.p("Dois nós iguais " + node + " otherHost " + otherHost.toString());
					return;
				}else continue;
					//Debug.p("Dois nós NÃO iguais " + node + " otherHost " + otherHost.toString());
			}

			aux.add(otherHost.toString());
			mappedEdges.remove(host.toString());
			mappedEdges.put(host.toString(), aux);

			Source_Vertex.add(host.toString());
			Target_Vertex.add(otherHost.toString());
			Edge_Weight.add(1.0);
		} else{
			aux = new ArrayList<String>();
			aux.add(otherHost.toString());

			mappedEdges.put(host.toString(), aux);

			Source_Vertex.add(host.toString());
			Target_Vertex.add(otherHost.toString());
			Edge_Weight.add(1.0);
		}

		//Debug.p("WORLD GRAPH " + GA1);
		//calcula betweenness
		GA1.BetweenNess_Centrality_Score(Distinct_Vertex, Source_Vertex, Target_Vertex, Edge_Weight);

		printaGrafo();
		calculaLCC();
	}

	public static void calculaLCC(){
		for(int i = 0;i<numNodes;i++){
			int grau = 0;
			int Li = 0;
			for(int j = 0;j<numNodes;j++)
				if(grafo[i][j]) grau++;

			ArrayList<Integer> vizinhos = new ArrayList<Integer>();
			for(int j = 0;j<numNodes;j++)
				if(grafo[i][j]) vizinhos.add(j);

			for(int j = 0;j<vizinhos.size()-1;j++){
				for(int k = 0;k<vizinhos.size();k++){
					if(grafo[vizinhos.get(j)][vizinhos.get(k)]) Li++;
				}
			}

			Double LCC_i = (2.0 * (double) Li) / ((double)(grau * (grau-1)));
			LCC.set(i,LCC_i);

			/*if(i == 3){
				//debug do no 3
				Debug.p("Grau: " + grau);
				Debug.p("Li: " + Li);
				Debug.p("LCC_3: " + LCC_i);
			}*/
		}

		Debug.p("LCC:");
		for(int i = 0;i<numNodes;i++){
			Debug.p("["+i+"] = " + LCC.get(i));
		}
		Debug.p("");
	}

	public static void printaGrafo(){
		Debug.p("[" + SimClock.getTime()+ "] Grafo:");
		for(int i = 0;i<numNodes;i++){
			System.out.print("Node " + i + "\t");
			for(int j = 0;j<numNodes;j++){
				if(j>0) System.out.print(" ");
				System.out.print(grafo[i][j] == true ? 1 : 0);
			}
			System.out.println();
		}
		System.out.println();
	}
}
