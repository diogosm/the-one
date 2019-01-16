package routing;

import core.*;
import util.Tuple;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class RoteamentoAmigos extends ActiveRouter {
    //variaveis do RoteamentoAmigos
    public ArrayList<Integer> grafoAmizade;
    public static final int numeroNodes = 4;
    public static int addressCopy=0;

    public RoteamentoAmigos(Settings s){
        super(s);
    }

    protected RoteamentoAmigos(RoteamentoAmigos r){
        super(r);

        int enderecoNode = getNextAddressCopy();

        //Ler grafo de amizade
        grafoAmizade = new ArrayList<Integer>();
        for(int i = 0;i<numeroNodes;i++) grafoAmizade.add(i,0);

        Debug.p("Meu número de node = " + enderecoNode);

        //Ler o arquivo
        try{
            Scanner scanner = new Scanner(new File("mobilidade/grafo.txt"));
            for(int i = 0;i<numeroNodes;i++)
                for(int j = 0;j<numeroNodes;j++)
                    if(i == enderecoNode){
                        grafoAmizade.set(j, scanner.nextInt());
                    }else
                        scanner.nextInt();

            Debug.p("Meus amigos são: ");
            for(int i = 0;i<numeroNodes;i++)
                if(grafoAmizade.get(i) == 1)
                    Debug.p("\tNó" + i);
        } catch (FileNotFoundException e){

        }
    }

    public void tryOtherMessages(){
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
    }

    @Override
    public void update(){
        super.update();

        tryOtherMessages();
    }

    @Override
    public RoteamentoAmigos replicate(){
        return new RoteamentoAmigos(this);
    }

    private synchronized static int getNextAddressCopy() {
        return addressCopy++;
    }
}
