/*
 * Andre Marquez
 * Shane O'Hanlon
 */
import java.util.*;
import java.nio.ByteBuffer;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;

public class Peer{
    public String
        peerID="",
        ip= "",
        handshake="";
    public int
        port;
    public Socket sock = null;
    public DataOutputStream up = null;
    public DataInputStream down= null;
    public byte[] 
        message,
        clientID,
        clientHash;
    public boolean
        choked = true,
        interested = false; 
    public BitSet[] bitfield;

    public boolean[] have =null;
    public boolean[] want = null;
    public Piece[] pieces = null;
    public ArrayList<Piece> d_pieces = null;
    public ArrayList<Piece> r_pieces = null;

//initialize peer
    public Peer(String client_id, String peer_id, String temp_ip, int temp_port,byte[] hash, Piece[] p){

        clientID = client_id.getBytes();
        peerID   = peer_id;
        ip       = temp_ip;
        port     = temp_port;
        sock     = null;
        clientHash= hash;
        pieces   = p;
        r_pieces = new ArrayList<Piece>();
        d_pieces = new ArrayList<Piece>();

    }
/*Socket things: #Sockembopem*/

    public boolean openSocket(){
        try {
            sock = new Socket(ip, port);
            if(sock == null) System.out.println("WOOPS");
            return true;

        }catch (Exception e){
            System.out.println("WOOPS");
            System.out.println(e);
            return false;
        }
    }
    public void updatePieceList(ArrayList<Piece> p){
        r_pieces = p;
    }
    public boolean closeSocket(){
        try {
            if (sock != null){
                sock.close(); 
            return true;
            }
        }catch(Exception e){
            System.out.println(e);
            return false;
        }
            return true;
    }
//establish the up and down streams
    
    public boolean setupStreams(){
        try {
            up = new DataOutputStream(sock.getOutputStream());
            down = new DataInputStream(new BufferedInputStream(
                        sock.getInputStream()));
        }catch (Exception e){
            System.out.println(e);
        }
        if(up == null || down == null){
            return false;
        }else {
            return true;
        }
    }

//create and send our handshake
    public boolean sendHandshake(){
        int outlength = 0;
        byte[] out  = new byte[68];
        out[0] = 0x13;
        outlength++;
        byte[] temp = new String("BitTorrent protocol").getBytes();
        System.arraycopy(temp, 0, out, outlength, temp.length);
        outlength += temp.length;
        byte[] zeros = new byte[8];
        System.arraycopy(zeros, 0, out, outlength, zeros.length);
        outlength += zeros.length;
        System.arraycopy(clientHash, 0, out, outlength, clientHash.length);
        outlength += clientHash.length;
        System.arraycopy(clientID, 0, out, outlength, clientID.length);
        outlength+= clientID.length;
        try {
            up.write(out);
            up.flush();
            return true;
        }catch(Exception e){
            System.out.println(e);
            return false;
        }
    }
    public boolean receiveHandshake(){
        try{
            byte[] responseHash =  new byte[20];
            byte[] response = new byte[68];
            down.read(response);
            System.arraycopy(response, 28, responseHash, 0, 20);
            //Verify handshake
            for(int i = 0; i < 20; i++){
                if (responseHash[i] != clientHash[i]){
                    return false;
                }
            }
            return true;
        } catch(Exception e){
            System.out.println(e);
        }
        return false;
    }
 
//wait to not be choked
    public boolean waitForUnchoke() {
        try {
            if (down.read() == 1 && down.read() == 1){
                return true;
            }
        }catch(Exception e){
            System.out.println(e);
        }
        return false;
    }



    public String listen(){
        //byte[] income = new byte[5];
        byte[] original_message  = null;
        try{
            Message message = Message.readMessage(down);
            //down.read(income);
            //message = Message.readMessage(income);
            original_message = message.message;
            String type = message.getType();
            if(type.equals("bitfield")){
                //Do Stuff
                Message.BitfieldMessage bitM = (Message.BitfieldMessage) message;
                byte[] bytefield = bitM.getBytefield();
                //down.read(bytefield);
                boolean[] bitfield = bitM.getBitfield();
                //bitfield = UtilityBelt.convertBytes(bytefield);
                have = bitfield;
                want = have;
                //pieces = new byte[have.length][16384];
            }if(type.equals("unchoke")){
                
            }
            if(type.equals("piece")){
                //grab the length prefix
                /*
                byte[] lengthPrefix = new byte[4];
                for (int i = 0; i < 4; i++){
                    lengthPrefix[i] = income[i];
                }
                ByteBuffer lp = ByteBuffer.wrap(lengthPrefix);
                int length = lp.getInt();
                System.out.println("The length is: " + length);
                int index = down.readInt();
                int offset = down.readInt();
                byte[] bytefield = new byte[length-9];
                down.readFully(bytefield);
                System.out.println("The payload: " + Arrays.toString(bytefield));
            */
                
            }
            return type;
        }catch(Exception e){
            System.out.println(e);
        }
        return null;
    }
//byte to bits
    public static BitSet fromByte(byte b){
        BitSet bits = new BitSet(8);
        for(int i = 0; i< 8; i++){
            bits.set(i, (b & 1) == 1);
            b >>= 1;
        }
        return bits;
    }
    public static BitSet[] bitfield(byte[] ba){
        System.out.println("BA: " + ba.length);
        BitSet[] bits = new BitSet[8*ba.length];
        for(int i =0;i < ba.length; i++){
            bits[i] = fromByte(ba[i]);
        }
        return bits;
    }
    public void printFields(){
        System.out.println("peerID: " + this.peerID);
        System.out.println("ip: " + this.ip);
    }
//REQUESTS

}
