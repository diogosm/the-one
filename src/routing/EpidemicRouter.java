/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Settings;
import core.World;

import java.util.ArrayList;

import static core.SimScenario.NROF_HOSTS_S;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class EpidemicRouter extends ActiveRouter {
	public ArrayList<Integer> grafoAmizade;
	public int numeroNodes = 0;
	public static int addressCopy=0;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public EpidemicRouter(Settings s) {
		super(s);
		//TODO: read&use epidemic router specific settings (if any)
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected EpidemicRouter(EpidemicRouter r) {
		super(r);
		//TODO: copy epidemic settings here (if any)

		Settings s = new Settings("Group");
		s.setSecondaryNamespace("Group");
		numeroNodes = s.getInt(NROF_HOSTS_S);

		grafoAmizade = new ArrayList<Integer>();
		for(int i = 0;i<numeroNodes;i++) grafoAmizade.add(i,0);
	}

	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);

		if (con.isUp()) { // new connection
			DTNHost otherHost = con.getOtherNode(this.getHost());

			int aux = grafoAmizade.get(otherHost.getAddress());
			grafoAmizade.set(otherHost.getAddress(), aux+1);

			//add no grafo global
			World.addGrafo(this.getHost(), otherHost);

            /*
            Debug.p("[UPDATE] [" + this.getHost().getAddress()
                    + "][" + otherHost.getAddress()
                    + "] => " + grafoAmizade.get(otherHost.getAddress()));
            */
		}
		else {
			DTNHost otherHost = con.getOtherNode(this.getHost());

			int aux = grafoAmizade.get(otherHost.getAddress());
			grafoAmizade.set(otherHost.getAddress(), aux-1);

			//World.removeGrafo(this.getHost(), otherHost);
		}
	}


	@Override
	public EpidemicRouter replicate() {
		return new EpidemicRouter(this);
	}

}
