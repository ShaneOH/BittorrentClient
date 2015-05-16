/*
 *@author Shane O'Hanlon
 *@author Andre Marquez
 *
 *
 */
import java.io.*;
import java.util.*;
public class Conversation{

    public Peer peer = null;
    public boolean
        engaged = false;
    public String type;
    public int tid;
    ArrayList<Piece> want = new ArrayList<Piece>();
    public Tracker t;
    Conversation(Peer peer_, int _tid, Tracker _t) throws IOException {
        peer = peer_;
        tid = _tid;
        t= _t;
        engaged = greet();
        int count = 0;
        while(!engaged && count < 50){
            count++;
            System.out.println("Failed in communicating with Peer: " + peer.peerID + " : " + peer.ip + "\nTrying again...");
        }
        System.out.println(peer.peerID + " Finished conversation");
    }

    public void addWant(Piece p){
        want.add(p);
    }

    public boolean greet(){
        try{
            peer.openSocket();
            peer.setupStreams();
            peer.sendHandshake();

            if(!peer.receiveHandshake()){
                System.out.println("Didn't recieve handshake, exiting.");
                peer.closeSocket();
                return false;
            };
            return true;

        } catch(Exception e){
            System.out.println(e);
            peer.closeSocket();
            return false;
        }
    }

    void wait(Peer peer){
        while(peer.choked){
            peer.choked = !(peer.waitForUnchoke());
        }
    }

    void bitfield() throws IOException{
        Message.writeMessage(peer.up, Message.interested);
        System.out.println("Sent interested, waiting for unchoke");
        wait(peer);
        System.out.println("Unchoked ");
    }

    void respond()throws IOException {
        type = peer.listen();
        if(type.equals("bitfield")){

            System.out.println("bitfield");
            bitfield();
            //updateRarity();

        }
    }
/*
    void updateRarity(Piece[] pieces){

        System.out.println("Updating rarity for peer: " + peer.peerID);
        for(int i = 0; i < peer.have.length; i++){
            if(peer.have[i]){
                pieces[i].rarity();
            }
        }

    }*/
/*
    void requestPieces(char s)throws IOException{
        System.out.println("Requesting Pieces...");
        switch(s){
            case 's':  //sequentil
                System.out.println("\t Sequentially");
                for(Piece piece : pieces){
                    while(!piece.full() && piece.assigned.contains(tid)){
                        System.out.println("tid : " + tid + "piece ass: " + piece.assigned + "\t piece id: " + piece.index);
                        for(Block block : piece.blocks){
                            while(!block.full()){
                                Message.writeMessage(peer.up,
                                        new Message.RequestMessage(piece.index,
                                            block.index*16384,
                                            block.size));
                                Message tm = Message.readMessage(peer.down);
                                type = tm.getType();
                                if(type.equals("piece")){
                                    Message.PieceMessage pieceM = (Message.PieceMessage)tm;
                                    block.data(pieceM.getBlock());
                                }
                                else if(type.equals("choke")){
                                    peer.choked = true;
                                    wait(peer);
                                }
                            }
                        }
                    }
                    pieces[piece.index] = piece;
                }
                break;
                
        }
    }
*/

    void requestPieces(Piece[] masterPiece) throws IOException{
        
        for(Piece p : peer.r_pieces){
            System.out.println("peer : " + peer.peerID + "\t " + p.index);
            if(masterPiece[p.index].full()) {
                System.out.println("\t\t\t\t\t ALREADY GOT THIS ONE COOL");
                continue;
            };
            for(Block block : p.blocks){
                while(!block.full()){
                    if(masterPiece[p.index].full()) {
                        System.out.println("\t\t\t\t\t ALREADY GOT THIS ONE COOL");
                        break;
                    };
                    Message.writeMessage(peer.up, new Message.RequestMessage(p.index, block.index*16384, block.size));
                    Message tm = Message.readMessage(peer.down);
                    type = tm.getType();
                    if(type.equals("piece")){
                        Message.PieceMessage pieceM = (Message.PieceMessage)tm;
                        block.data(pieceM.getBlock());
                    }
                    else if(type.equals("choke")){
                        peer.choked = true;
                        wait(peer);
                    }
                }
            }
            t.downloaded += p.size;
            t.left       -= p.size;
            peer.d_pieces.add(p);
        }
    }
}
