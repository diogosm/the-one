package routing;

import core.*;
import util.Tuple;

import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

public class RoteamentoParaAmigos extends ActiveRouter {
    public ArrayList<Integer> amigos;
    public static int meuEnderecoCopia = 0;
    public int numeroNos = 4;

    public RoteamentoParaAmigos(Settings s){
        super(s);
    }

    protected RoteamentoParaAmigos(RoteamentoParaAmigos r){
        super(r);

        int meuEndereco = getNextAdressCopy();

        Debug.p("Eu sou o nó: " + meuEndereco);

        //ler o grafo dos nós
        amigos = new ArrayList<Integer>();
        for(int i = 0;i<numeroNos;i++) amigos.add(i,0);

        //ler o arquivo pra definir meus amigos
        try{
            Scanner scanner = new Scanner(new File("mobilidade/grafo.txt"));

            for(int i = 0;i<numeroNos;i++)
                for(int j = 0;j<numeroNos;j++)
                    if(i == meuEndereco){
                        int ans = scanner.nextInt();
                        if(ans == 1)
                            amigos.set(j, ans);
                    }else{
                        scanner.nextInt();
                    }
        } catch (IOException e){
        }

        Debug.p("Meus amigos são: ");

        for(int i=0;i<numeroNos;i++)
            if(amigos.get(i) == 1) Debug.p("\tNó " + i);
    }

    //funcoes base
    @Override
    public void update(){
        super.update();

        transfereMsgs();
    }

    public void transfereMsgs(){
        List<Tuple<Message, Connection>> tuples = new ArrayList<Tuple<Message, Connection>>();

        for(Connection con : getConnections()){
            DTNHost vizinho = con.getOtherNode(this.getHost());

            if(amigos.get(vizinho.getAddress()) == 1){
                //meu amigo
                for(Message message : this.getMessageCollection()){
                    Message copia = message;

                    DTNHost vizinhoMsg = copia.getTo();

                    if(vizinhoMsg.getRouter().verificaAmigo())
                        tuples.add(new Tuple<Message, Connection>(message, con));
                    else{}
                }

            }else{ continue; }
        }

        tryMessagesForConnected(tuples);
    }

    //funcoes base
    @Override
    public RoteamentoParaAmigos replicate(){
        return new RoteamentoParaAmigos(this);
    }

    private synchronized static int getNextAdressCopy(){
        return meuEnderecoCopia++;
    }

}
