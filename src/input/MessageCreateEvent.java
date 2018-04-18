/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import core.DTNHost;
import core.Message;
import core.World;
import java.util.ArrayList;

/**
 * External event for creating a message.
 */
public class MessageCreateEvent extends MessageEvent {
	private int size;
	private int responseSize;
	private int[] hostsToList;
	private boolean isMulticast = false;

	/**
	 * Creates a message creation event with a optional response request
	 * @param from The creator of the message
	 * @param to Where the message is destined to
	 * @param id ID of the message
	 * @param size Size of the message
	 * @param responseSize Size of the requested response message or 0 if
	 * no response is requested
	 * @param time Time, when the message is created
	 */
	public MessageCreateEvent(int from, int to, String id, int size,
			int responseSize, double time) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
	}

	//construtor para multicast
	public MessageCreateEvent(int from, int to, String id, int size,
			int responseSize, double time, int[] hostsToList) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
		this.hostsToList = hostsToList;
		this.isMulticast = true;
	}


	/**
	 * Creates the message this event represents.
	 */
	@Override
	public void processEvent(World world) {
		DTNHost to = world.getNodeByAddress(this.toAddr);
		DTNHost from = world.getNodeByAddress(this.fromAddr);

		//old version
		if(!this.isMulticast){
			Message m = new Message(from, to, this.id, this.size);
			m.setResponseSize(this.responseSize);
			from.createNewMessage(m);
		}else{	//faz multicast
			ArrayList<DTNHost> receivers = new ArrayList<DTNHost>();

			for(int i = 0;i<this.hostsToList.length;i++){
				DTNHost aux = world.getNodeByAddress(this.hostsToList[i]);
				receivers.add(aux);
			}

			Message m = new Message(from, to, this.id, this.size, receivers);
			m.setResponseSize(this.responseSize);
			from.createNewMessage(m);
		}
	}

	@Override
	public String toString() {
		return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
		"size:" + size + " CREATE";
	}
}
