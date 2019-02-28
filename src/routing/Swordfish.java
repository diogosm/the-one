package routing;

import core.*;
import edu.uci.ics.jung.graph.util.Pair;
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
        //nao tenho mensagens pra enviar ou nao tenho vizinhos
        if(this.getNrofMessages() == 0 || getConnections().size() <= 0) return;

        ArrayList<Tuple<Double, Tuple<DTNHost, Connection>>> vizinhos = new ArrayList<Tuple<Double, Tuple<DTNHost, Connection>>>();
        List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>();

        //Debug.p("Eu sou " + this.getHost());
        //Debug.p("Minhas mensagens " + getMessageCollection());
        //Debug.p("Meus vizinhos: ");
        for(Connection con : getConnections()){
            DTNHost viz = con.getOtherNode(this.getHost());
            //Debug.p("\t" + viz + " ProbForwarding: " + World.PforwardS_i.get(viz.getAddress()));
            //Debug.p("\t\t" + World.B_i.get(viz.getAddress()));

            vizinhos.add(new Tuple<Double, Tuple<DTNHost, Connection>>(World.PforwardS_i.get(viz.getAddress()),
                    new Tuple<DTNHost, Connection>(viz, con)));
        }

        /*
        Debug.p("\nTupla::");
        for(Tuple<Double, Tuple<DTNHost, Connection>> t : vizinhos){
            Debug.p("\t" + t.getKey() + ", " + t.getValue());
        }
        Debug.p("\n\n");
        */

        Comparator<Tuple<Double, Tuple<DTNHost, Connection>>> comparator = new Comparator<Tuple<Double, Tuple<DTNHost, Connection>>>() {
            public int compare(Tuple<Double, Tuple<DTNHost, Connection>> tupleA, Tuple<Double, Tuple<DTNHost, Connection>> tupleB) {
                if(tupleA.getKey() < tupleB.getKey()) return 1;
                return -1;
            }
        };

        Collections.sort(vizinhos, comparator);

        /*
        Debug.p("\nTupla depois do sort::");
        for(Tuple<Double, Tuple<DTNHost, Connection>> t : vizinhos){
            Debug.p("\t" + t.getKey() + ", " + t.getValue());
        }
        Debug.p("\n\n");
        */

        /*
         * Envia ordenado pras conexoes que tem maior prob forwarding
         */
        for(Tuple<Double, Tuple<DTNHost, Connection>> t : vizinhos){
            Connection con = t.getValue().getValue();

            for(Message m : this.getMessageCollection()){
                messages.add(new Tuple<Message, Connection>(m, con));
            }
        }

        tryMessagesForConnected(messages);
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
