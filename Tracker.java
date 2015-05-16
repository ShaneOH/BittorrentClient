/*
 * The goal of this class is to talk to the tracker, get information, and just exist as an object holding all that information.
 * Time to go in. 
 */
/*
 *@author Shane O'Hanlon
 *@author Andre Marquez
 *
 *
 */
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.ArrayList;
import java.net.URL;
import java.net.URLConnection;

public class Tracker{
    public static String 
        announce_url,
        escapedHash,
        url,
        clientID,
        event;
    public static ArrayList<String>
        peerIP = null,
        peerID = null;
    public static ArrayList<Integer>
        peerPort = null;
    public static int
        trackerPort,
        interval,
        mininterval,
        uploaded,
        downloaded,
        left,
        piece_sizes, //new  
        num_pieces,
        last_piece_size, //super important dont fuck this up
        file_size,
        leftoverBytes = 0;
    public final int block_length = 16384;
    public static byte[]
        torrentInfo,
        SHA1,
        response;
    URLConnection tracker_connection;

/*
* Constructor 
*
* @param torrentfile
*      name of the torrent file your are using
*
*/

Tracker(String torrentFile){
        Path torrentFilePath    = Paths.get(torrentFile);
        try{
            torrentInfo             = Files.readAllBytes(torrentFilePath);
        }catch(Exception e){
            System.out.println(e);
        }
        uploaded = 0;
        downloaded =0;
        event = "";
        peerPort = new ArrayList<Integer>();
        peerIP = new ArrayList<String>();
        peerID = new ArrayList<String>();
        url         = constructQuery(torrentInfo);
        response    = UtilityBelt.getURL(url);                   
        parseResponse();
    }
//methods
    
@SuppressWarnings("unchecked")
private static void parseResponse(){
    /* LOCAL VARIABLES */
        Object decodedResponse = null;
        Map<ByteBuffer, Object> responseMap = null;
        Integer int_bb = null, int_min = null;
        int  _peerPort;
        ArrayList<Map<ByteBuffer, Object>> peers = null;
        String peer_id = null,
               peer_ip = null;
        try{
        //Decode response using Bencoder2
            decodedResponse = Bencoder2.decode(response);
        //cast
            responseMap = (Map<ByteBuffer, Object>)decodedResponse;

        //get interval;
            int_bb = (Integer)responseMap.get(UtilityBelt.getKey("interval"));
            int_min = (Integer)responseMap.get(UtilityBelt.getKey("min interval"));
    
        //List of peers returned by tracker:
            peers = 
                (ArrayList<Map<ByteBuffer,Object>>)responseMap.get(UtilityBelt.getKey("peers"));
        //Find valid peer:
            for(Map<ByteBuffer, Object> dict : peers){
                peer_id = UtilityBelt.bufferToString( (ByteBuffer)dict.get(UtilityBelt.getKey("peer id")));
                peer_ip = UtilityBelt.bufferToString((ByteBuffer)dict.get(UtilityBelt.getKey("ip")));
                //if(peer_ip.equals("128.6.171.130")){
                if(!peer_ip.equals("172.31.247.193") && !peer_ip.equals("128.6.171.130")){
                    peerID.add(peer_id);
                    peerPort.add(((Integer)dict.get(UtilityBelt.getKey("port"))).intValue());
                    peerIP.add(peer_ip);
                }
            }
            if(int_bb == null || int_min == null){
                throw new BencodingException("Couldn't get interval size");
            }
            interval = int_bb.intValue();
            mininterval = int_min.intValue();
        }catch(Exception e){
            System.out.println("woops" + e);
        }
}
public static String constructQuery(byte[] torrentArray){
        String url_string   = "",
               escaped_hash = "",
               peer_id      = "";
        TorrentInfo decoded = null;
        //Shane look here
        byte[] escaped      = new byte[20];
        
        uploaded     = 0;
        downloaded   = 0;
        left         = 0;
        file_size    = 0;
        try{
        //get initial decoded torrent info

        //Then here
            decoded = new TorrentInfo(torrentArray);
            decoded.info_hash.get(escaped, 0, escaped.length);
            //At this line you'll have the string in the variable "escaped" as a byte[]

            SHA1        = escaped;
        //Fill in attributes
            //Last but not least this guy
            escaped_hash = UtilityBelt.toURLHex(escaped);
            escapedHash = escaped_hash;
            peer_id     = UtilityBelt.generatePeerID();
            clientID    = peer_id;
            left        = decoded.file_length;
        //Put it all together    
            announce_url = decoded.announce_url.toString();
            piece_sizes = decoded.piece_length;
            file_size = decoded.file_length;
            leftoverBytes = decoded.file_length;
            last_piece_size = file_size%piece_sizes;
            num_pieces =  1 + ((file_size)/piece_sizes);
            url_string = decoded.announce_url.toString()
                            + "?peer_id=" + clientID
                            + "&info_hash=" + escaped_hash
                            + "&uploaded=" + uploaded
                            + "&downloaded=" + downloaded
                            + "&left=" + left;
        }catch(Exception e){
            System.out.println(e);
        }

        return url_string;
    }
    
public static void setEvent(String update){
    if( update.equals("started") || update.equals("stopped") || update.equals("completed"))
        event = update; 
        System.out.println("Updated Event");
    }
public static void update(){
        String url_string;
        url_string = announce_url
            + "?peer_id=" + clientID
            + "&info_hash=" + escapedHash
            + "&uploaded=" + uploaded
            + "&downloaded=" + downloaded
            + "&left=" + left
            + "&event=" + event;
        url = url_string;
        System.out.println("Updating tracker with new GET request: \n" +url_string);
        response = UtilityBelt.getURL(url_string);
        parseResponse();
        System.out.println("Successfully updated tracker");
        printFields();
    }
public static void printFields(){
       System.out.println("\nannounce_url = " + announce_url
               + "\nescapedHash = " + escapedHash
               + "\nurl = " + url
               + "\nclientID = " + clientID
               + "\npeerIP = " + peerIP
               + "\npeerID = " + peerID
               + "\nevent = " + event
               + "\ntrackerPort = " + trackerPort
               + "\npeerPort = " + peerPort
               + "\ninterval = " + interval
               + "\nmininterval = " + mininterval
               + "\nuploaded = " + uploaded
               + "\ndownloaded = " + downloaded
               + "\nleft = " + left
               + "\nleftoverBytes = " + leftoverBytes);
    }
}
