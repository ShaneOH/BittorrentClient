/*
 *@author Shane O'Hanlon
 *@author Andre Marquez
 *
 *
 */
import java.util.*;
import java.io.*;

public class Message{
    public static String type = "";
    private final byte id;
    private final int length;

    public byte[] message = null;
    
    public static final byte keepAliveID = -1;
    public static final byte chokeID = 0;
    public static final byte unchokeID = 1;
    public static final byte interestedID = 2;
    public static final byte uninterestedID = 3;
    public static final byte haveID = 4;
    public static final byte bitfieldID = 5;
    public static final byte requestID = 6;
    public static final byte pieceID = 7;
    public static final byte cancelID = 8;

    //sets the length and ID, as seen in the constructor below
    public static Message keepAlive = new Message(0, keepAliveID);
    public static Message choke = new Message(1, chokeID);
    public static Message unchoke = new Message(1, unchokeID);
    public static Message interested = new Message(1, interestedID);
    public static Message uninterested = new Message(1, uninterestedID);

    public int getLength(){
        return this.length;
    }

    public byte getID(){
        return this.id;
    }

    public String getType(){
        return this.type;
    }

    //Constructor
    protected Message(final int length, final byte id){
        this.id = id;
        this.length = length;
    }

    public static Message readMessage(InputStream in) throws IOException{
        DataInputStream down = new DataInputStream(in);
        int length = down.readInt();

        if(length == 0){
            return Message.keepAlive;
        }

        byte id = down.readByte();
        //System.out.println("The message ID is: " + id);
        
        //these next few variables refer to the components of various messages, which are clarified in the unofficial documentation
        int index;
        int begin;
        int requestLength;
        byte[] block;

        switch(id){
            case chokeID:
                type = "choke"; 
                return choke;
            case unchokeID:
                type = "unchoke";
                return unchoke;
            case interestedID:
                type = "interested";
                return interested;
            case uninterestedID:
                type = "uninterested";
                return uninterested;
            case haveID:
                type = "have";
                index = down.readInt();
                return new HaveMessage(index);
            case requestID:
                type = "request";
                index = down.readInt();
                begin = down.readInt();
                requestLength = down.readInt();
                return new RequestMessage(index, begin, requestLength);
            case pieceID:
                type = "piece";
                index = down.readInt();
                begin = down.readInt();
                block = new byte[length-9];
                down.readFully(block);
                return new PieceMessage(index, begin, block);
            case bitfieldID:
                type = "bitfield";
                byte[] bytefield = new byte[length-1];
                down.readFully(bytefield);
                return new BitfieldMessage(bytefield);
            default:
                break;
            }
        return null;
    }

    public static void writeMessage(final OutputStream out, Message message) throws IOException {
        if(message == null){
            throw new IOException("Error: Null message");
        }

        DataOutputStream up = new DataOutputStream(out);
        up.writeInt(message.length);
        
        try{
            if(message.getLength() > 0) {
                up.writeByte(message.getID());
                switch (message.getID()){
                case Message.haveID:
                    HaveMessage hm = (HaveMessage) message;
                    up.writeInt(hm.getPieceIndex());
                    break;
                case Message.requestID:
                    RequestMessage rm = (RequestMessage) message;
                    up.writeInt(rm.getPieceIndex());
                    up.writeInt(rm.getOffset());
                    up.writeInt(rm.getBlockLength());
                    break;
                case Message.bitfieldID:
                    BitfieldMessage bm = (BitfieldMessage) message;
                    byte[] bytes = bm.getBytefield();
                    up.write(bytes);
                    break;
                case Message.pieceID:
                    PieceMessage pm = (PieceMessage) message;
                    up.writeInt(pm.getPieceIndex());
                    up.writeInt(pm.getOffset());
                    up.write(pm.getBlock());
                    break;
                }
            }
            up.flush();
        }
        catch (NullPointerException npe){
            throw new IOException("Cannot write to null stream!");
        }
        
    }

    public static class HaveMessage extends Message{
        private final int index;

        //subclass constructor invokes superclass constructor, has same parameters
        public HaveMessage(int index){
            super(5 /*length prefix*/, haveID);
            this.index = index;
        }
        public int getPieceIndex(){
            return this.index;
        }
    }

    public static class RequestMessage extends Message{

        private final int index;
        private final int begin;
        private final int length;
        
        public RequestMessage(int index, int begin, int length){
            super(13 /*length prefix*/, requestID);
            this.index = index;
            this.begin = begin;
            this.length = length;
        }
        public int getPieceIndex(){
            return index;
        }
        public int getOffset(){
            return begin;
        }
        public int getBlockLength(){
            return length;
        }
    }

    public static class PieceMessage extends Message{

        private int index;
        private int begin;
        private byte[] block;

        public PieceMessage(int index, int begin, byte[] block){
            super(block.length + 9, pieceID);
            this.index = index;
            this.begin = begin;
            this.block = block;
        }
        public int getPieceIndex(){
            return index;
        }
        public int getOffset(){
            return begin;
        }
        public byte[] getBlock(){
            return block;
        }
        public void setBlockOffset(int offset){
            begin = offset;
        }
    }

    public static class BitfieldMessage extends Message{

        private final byte[] bytefield;
        private final boolean[] bitfield;
        
        public BitfieldMessage(byte[] bytefield){
            super(bytefield.length + 1, bitfieldID);
            this.bytefield = bytefield;
            this.bitfield = UtilityBelt.convertBytes(bytefield);
        }
        public byte[] getBytefield(){
            return this.bytefield;
        }
        public boolean[] getBitfield(){
            return this.bitfield;
        }
    }

    public class CancelMessage extends Message{

        private int index;
        private int begin;
        private int length;

        public CancelMessage(int index, int begin, int length){
            super(13 /*you get the idea by now*/, cancelID);
            this.index = index;
            this.begin = begin;
            this.length = length;
        }
        public int getPieceIndex(){
            return index;
        }
        public int getOffset(){
            return begin;
        }
        public int getPieceLength(){
            return length;
        }
    }



    /*
    //initialize message
    Message(byte[] message_){
        message = message_;
        System.out.println("Message length: " + message.length);
        //Store type of message
        int lastLen = message_[3],
            id;
        for(byte i : message){
            if( i != 0) {
                type = "";
                break;
            }
            type = "keep alive";
        }
        //if it's 1, check id
        if(lastLen == 1 || lastLen == 5){
            id = message_[4]; 
            type = types[id];
        }else if(message_[4] == 7){
            type = types[message_[4]];
        }else if(lastLen == 3){
            type = types[6];
        }else if( lastLen != 0){
            type = types[5]; 
            bitLength= message_[3] - 1;
        }
    }
    Message(String type_){
        type = type_;
        if(type.equals("choke")) message = choke;
        if(type.equals("unchoke"))message= unchoke;
        if(type.equals("interested"))message= interested;
        if(type.equals("uninterested"))message=uninterested;
        if(type.equals("have")) message = have;
        if(type.equals("request")){
            message = request;
        }
        if(type.equals("keep alive")) message= keepAlive;
    }
    */
}
