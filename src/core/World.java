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
	//lista Betweenness pra cada node
	public static ArrayList<Double> B_i;
	//lista de LCC pra cada node
	public static ArrayList<Double> LCC;
	//resultado GAME
	public static ArrayList<Double> PG_i;
	public static ArrayList<Double> PforwardG_i; //prob de forward
	//resultado SWORDFISH
	public static ArrayList<Double> PS_i;
	public static ArrayList<Double> PforwardS_i; //prob de forward
	//variaveis pra probability forward
	public static double z;
	public static double p;
	public static List<DTNHost> hostsAux;

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

		B_i = new ArrayList<Double>();
		LCC = new ArrayList<Double>();
		for(int i = 0;i<this.hosts.size();i++) LCC.add(0.0);

		//inicializa algoritmo
		PG_i = new ArrayList<Double>();
		PS_i = new ArrayList<Double>();
		PforwardG_i = new ArrayList<Double>();
		PforwardS_i = new ArrayList<Double>();
		for(int i = 0;i<this.hosts.size();i++) {
			PG_i.add(0.0);
			PS_i.add(0.0);
			PforwardG_i.add(0.0);
			PforwardS_i.add(0.0);
		}

		Debug.p("size = " + this.getSizeX());
		p = (double)this.hosts.size()/(double)(this.getSizeX() * this.getSizeY());
		z = (double)hosts.get(0).getInterfaces().get(0).getTransmitRange();
		hostsAux = hosts;

		Debug.p("p = " + p);
		Debug.p("z = " + z);
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

	public static void removeGrafo(DTNHost host, DTNHost otherHost) {
		ArrayList<String> aux;

		//add false no grafo em matriz
		Debug.p("Removendo... [" + host.getAddress() + "] <--> [" + otherHost.getAddress() + "]");
		grafo[host.getAddress()][otherHost.getAddress()] = false;

		//premissa = nunca há um DOWN antes de um UP entre dois nodes
		//logo mappedEdges nunca está vazio para @host
		aux = mappedEdges.get(host.toString());

		//procuro se o nó está mapeado
		for(String node : aux){
			if(otherHost.toString() == node){
				//Debug.p("Dois nós iguais " + node + " otherHost " + otherHost.toString());
				//remove do mapeamento
				//Debug.p("Antes de remover = " + aux);
				aux.remove(node);
				//Debug.p("Depois de remover = " + aux);
				break;
			}
		}

		mappedEdges.remove(host.toString());
		mappedEdges.put(host.toString(), aux);

		for(int i = 0;i<Source_Vertex.size();i++){
			String from = Source_Vertex.get(i);
			String to = Target_Vertex.get(i);

			if(from == host.toString() && to == otherHost.toString()){
				Source_Vertex.remove(i);
				Target_Vertex.remove(i);
				Edge_Weight.remove(i);
				break;
			}
		}

		//Debug.p("WORLD GRAPH " + GA1);
		//calcula betweenness
		B_i = GA1.BetweenNess_Centrality_Score(Distinct_Vertex, Source_Vertex, Target_Vertex, Edge_Weight);

		printaGrafo();
		calculaLCC();
		GAME();
		SWORDFISH();
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
		B_i = GA1.BetweenNess_Centrality_Score(Distinct_Vertex, Source_Vertex, Target_Vertex, Edge_Weight);

		printaGrafo();
		calculaLCC();
		GAME();
		SWORDFISH();
	}

	/*
		PG_i = log2(1 + B_i/max(B))
	 */
	public static void GAME(){
		Double maxB = -9999999.9;

		for(int i = 0;i<numNodes;i++){
			if(B_i.get(i) != null && B_i.get(i) > maxB)
					maxB = B_i.get(i);
		}

		//PARA DEBUG
		Debug.p("Maior Betweenness: " + maxB);

		for(int i = 0;i<numNodes;i++){
			Debug.p("B[" + i + "] = " + B_i.get(i));
		}
		Debug.p("");

		for(int i = 0;i<numNodes;i++){
			if(B_i.get(i) == null){
				PG_i.set(i,0.0);
				continue;
			}

			Double ans = 1 + (double)B_i.get(i)/maxB;
			ans = log2(ans);
			PG_i.set(i, ans);

			Debug.p("PG_i[" + i + "] = " + PG_i.get(i));
		}

		//calcula prob de forward
		for(int i = 0;i<numNodes;i++){
			double minDistance = 99999999.9;
			for(Connection connection : hostsAux.get(i).getConnections()){
				DTNHost me = hostsAux.get(i);
				DTNHost otherHost = connection.getOtherNode(hostsAux.get(i));

				minDistance = Math.min(minDistance, me.getLocation().distance(otherHost.getLocation()));
				//Debug.p("Eu ["+hostsAux.get(i).getAddress()+"] <-> ["+otherHost.getAddress()+"]");
			}

			//Debug.p("z = " + z + " metros - d[i] = " + minDistance + " metros");
			double ans = Math.exp(-p *
					(z - minDistance) / (PG_i.get(i))
			);
			if(PG_i.get(i) < 1e-6) ans = 0;

			PforwardG_i.set(i,ans);
			Debug.p("Me [" + hostsAux.get(i).getAddress() + "] " + "Prob de forward GAME => " + PforwardG_i.get(i));
		}
	}

	public static double logb(double a, double b){
		return Math.log(a)/Math.log(b);
	}

	public static double log2(double valor){
		return logb(valor, 2);
	}

	/*
		PS_i = log2(1 + (LCC_i * B_i)/(LCC_i + B_i))
	 */
	public static void SWORDFISH(){

		for(int i = 0;i<numNodes;i++){
			if(B_i.get(i) == null || LCC.get(i) == null){
				PS_i.set(i,0.0);
				continue;
			}

			Double ans = 1 + (
					(
							(double)LCC.get(i) * (double)B_i.get(i))
								/ (
							(double)LCC.get(i) + (double)B_i.get(i))
					);
			ans = log2(ans);
			PS_i.set(i, ans);
			if(ans.isNaN()) PS_i.set(i,0.0);

			Debug.p("PS[" + i + "] = " + PS_i.get(i));
		}

		//calcula prob de forward
		//calcula prob de forward
		for(int i = 0;i<numNodes;i++){
			double minDistance = 99999999.9;
			for(Connection connection : hostsAux.get(i).getConnections()){
				DTNHost me = hostsAux.get(i);
				DTNHost otherHost = connection.getOtherNode(hostsAux.get(i));

				minDistance = Math.min(minDistance, me.getLocation().distance(otherHost.getLocation()));
				//Debug.p("Eu ["+hostsAux.get(i).getAddress()+"] <-> ["+otherHost.getAddress()+"]");
			}

			//Debug.p("z = " + z + " metros - d[i] = " + minDistance + " metros");
			double ans = Math.exp(-p *
					(z - minDistance) / (PS_i.get(i))
			);
			if(PS_i.get(i) < 1e-6) ans = 0;

			PforwardS_i.set(i,ans);
			Debug.p("Me [" + hostsAux.get(i).getAddress() + "] " + "Prob de forward SWORDFISH => " + PforwardS_i.get(i));
		}
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
				for(int k = j+1;k<vizinhos.size();k++){
					if(grafo[vizinhos.get(j)][vizinhos.get(k)]) Li++;
				}
			}

			Double LCC_i = (2.0 * (double) Li) / ((double)(grau * (grau-1)));
			LCC.set(i,LCC_i);
			if(LCC_i.isNaN()) LCC.set(i,0.0);

			/*if(i == 3){
				//debug do no 3
				Debug.p("Grau: " + grau);
				Debug.p("Li: " + Li);
				Debug.p("LCC_3: " + LCC_i);
			}*/
		}

		Debug.p("LCC:");
		for(int i = 0;i<numNodes;i++){
			Debug.p("LCC["+i+"] = " + LCC.get(i));
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
