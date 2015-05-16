/*
 *@author Shane O'Hanlon
 *@author Andre Marquez
 *
 *
 */

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.channels.FileChannel;
import static java.lang.System.*;

public class RUBTClient{
//HELPER METHODS:

//Validate command line arguments:
    private static boolean validateArgs(String[] args){
            if(args.length == 2){
                if(!(args[0].substring(args[0].lastIndexOf('.') + 1).equals("torrent"))){
                    System.out.println("Invalid argument: the first argument should be of the form <filename>.torrent");
                    return false;
                }
                //we can improve this check (check for file extension)
                else if(args[1].indexOf('.') == -1){ 
                    System.out.println("Invalid argument: the second argument should have a file extension");
                    return false;
                }
                else{
                    return true;
                }
            } 
            else {
                System.out.println("Error: This program takes exactly two arguments <filename>.torrent <filename>.<extension>");
                return false;
            }
    }

    public static void main(String[] args) throws IOException, BencodingException, InterruptedException {
    //Validate arguments
            if(!validateArgs(args)){
                System.out.println("\n Invalid arguments\n");
                return;
            }
//Need to iron out writing the file.
            FileOutputStream writer  = new FileOutputStream(args[1]);
//check command-line arguments
            System.out.println("Creating Tracker...");
            final Tracker myTracker = new Tracker(args[0]);
            myTracker.printFields();
            int piece_size = myTracker.piece_sizes,
                last_piece_size = myTracker.last_piece_size,
                num_pieces = myTracker.num_pieces;
//Set up an array of all the pieces
            
            final Piece[] myPieces = new Piece[num_pieces];
            for(int i = 0; i < num_pieces - 1; i++){
                myPieces[i] = new Piece(piece_size, i);
            }
            myPieces[num_pieces-1] = new Piece(last_piece_size, num_pieces -1);

            //Tracker updates perfectly
            Thread t_t = new Thread("Tracker"){
                public void run() {
                    while(myTracker.event.equals("stopped") == false && myTracker.event.equals("completed") == false){
                        System.out.println("myTracker : " + myTracker.event);
                        try{
                            Thread.sleep(myTracker.interval * 500);
                        }catch(InterruptedException e){

                        }
                        myTracker.update();
                    }
                }
            };

            t_t.start();

            ArrayList<Peer> peers_list = new ArrayList<Peer>();
            for(int i =0; i < myTracker.peerIP.size(); i++){
                Piece[] tp = new Piece[num_pieces];
                for(int k = 0; k < num_pieces - 1; k++){
                    tp[k] = new Piece(piece_size,k);
                }
                tp[num_pieces - 1] = new Piece(last_piece_size, num_pieces-1);
                peers_list.add( new Peer(
                        myTracker.clientID,
                        myTracker.peerID.get(i),
                        myTracker.peerIP.get(i),
                        myTracker.peerPort.get(i),
                        myTracker.SHA1,
                        tp));
            }

            int count = 1;
            
            myTracker.setEvent("started");
            //Group threads together
            ArrayList<Thread> myThreads = new ArrayList<Thread>();
            
            final ArrayList<Conversation> chatroom = new ArrayList<Conversation>();
            count = 0;
            //do rarity assign to global piece list which peers have what
            for( final Peer temp : peers_list){
                System.out.println("Connecting to PeerID: " + temp.peerID);
                System.out.println("\t  PeerIP: " + temp.ip);
                            //int thread_ident = Integer.parseInt(Thread.currentThread().getName());
                myThreads.add(new Thread(Integer.toString(count)){
                    public void run(){
                        try{
                            int t_tid = Integer.parseInt(Thread.currentThread().getName());
                            chatroom.add(new Conversation(temp, t_tid, myTracker));
                            chatroom.get(chatroom.size() -1).respond(); 
                
                            for(Piece piece : myPieces){
                                if(temp.have[piece.index] == true && piece.assigned.contains(t_tid) == false ){
                                
                                piece.assigned.add(t_tid);
                                piece.rarity += 1;
                                temp.r_pieces.add(piece);

                                }
                            }
                        }
                        catch(IOException e){

                        }
                    }
                });
                    //conversation.requestPieces('s');
                    //writeBytes(writer, myPieces);
                    //writer.close();
                count++;
            }
            for(Thread t : myThreads){
                t.start();
            }
            for(Thread t : myThreads){
                System.out.println("Waiting on " + t.getName());
                t.join();
            }
            myThreads.clear();

            count = 0;
            System.out.println("Chatroom size: " + chatroom.size());
            for(final Conversation convo : chatroom){

                Collections.shuffle(convo.peer.r_pieces);
                rareSort(convo.peer.r_pieces);
                myThreads.add(new Thread(Integer.toString(count)){
                    public void run(){

                        try {
                                System.out.println("Thread " + Thread.currentThread().getName() + " requesting pieces") ;
                                convo.requestPieces(myPieces);
                            }
                            catch (Exception e){

                            }

                        }
                });
                count++;
            }

            for(Thread t : myThreads){
                System.out.println("Thread : " + t.getName());
                t.start();
            }
            for(Thread t : myThreads){
                t.join();
            }
            for(Piece p : myPieces){
                if(p.full() == false ) System.out.println("FUCK WE LOST ONE");
            }
            myTracker.setEvent("completed");
            t_t.join();
            writeBytes(writer, myPieces);
            return;
    }

    static void writeBytes(FileOutputStream w, Piece[] p) throws IOException{
        for (int i = 0;i < p.length; i++){
            for(int h =0; h< p[i].blocks.length; h++){
                w.write(p[i].blocks[h].data);
            }
        }
    }
    static void rareSort(ArrayList<Piece> p){
        int max = 0;
        int tmp = 0;
        for(int i = 1; i< p.size(); i++){
            Piece tp = p.get(i);
            tmp = i - 1;
            while(p.get(i).rarity < p.get(tmp).rarity && tmp > 0){
                p.set(i, p.get(tmp));
                p.set(tmp, tp);
                i--;
                tmp--;
            }
        }

    }
}
