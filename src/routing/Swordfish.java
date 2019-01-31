package routing;

import core.*;
import util.Tuple;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import core.World;

import static core.SimScenario.NROF_GROUPS_S;
import static core.SimScenario.NROF_HOSTS_S;
import static core.SimScenario.NROF_INTERF_S;

public class Swordfish extends ActiveRouter {
    //variaveis do RoteamentoAmigos
    public ArrayList<Integer> grafoAmizade;
    public int numeroNodes = 0;
    public static int addressCopy=0;

    public Swordfish(Settings s){
        super(s);
    }

    protected Swordfish(Swordfish r){
        super(r);

        Settings s = new Settings("Group");
        s.setSecondaryNamespace("Group");
        numeroNodes = s.getInt(NROF_HOSTS_S);

        //Ler grafo de amizade
        int enderecoNode = getNextAddressCopy();
        grafoAmizade = new ArrayList<Integer>();
        for(int i = 0;i<numeroNodes;i++) grafoAmizade.add(i,0);

        //Debug.p("Meu número de node = " + enderecoNode + " (Total: " + numeroNodes + ")");
    }

    public void tryOtherMessages(){
        /*
        List<Connection> connections = getConnections();
        if (connections.size() == 0 || this.getNrofMessages() == 0) {
            return;
        }

        Collection<Message> msgCollection = getMessageCollection();
        List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>();

        for(Connection con : getConnections()){
            DTNHost vizinho = con.getOtherNode(this.getHost());

            //ele é meu amigo!!!
            if(grafoAmizade.get(vizinho.getAddress()) == 1){
                for(Message m : msgCollection){
                    messages.add(new Tuple<Message, Connection>(m, con));
                }
            }
        }

        tryMessagesForConnected(messages);
        */
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
            //nothing
        }
    }

    @Override
    public void update(){
        super.update();


        tryOtherMessages();
    }

    @Override
    public Swordfish replicate(){
        return new Swordfish(this);
    }

    private synchronized static int getNextAddressCopy() {
        return addressCopy++;
    }
}
