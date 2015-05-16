/*
 *@author Shane O'Hanlon
 *@author Andre Marquez
 *
 *
 */
/*
 * this contains keys, and helper methods. Our client will be batman. 
 */
import java.nio.ByteBuffer;
import java.util.Random;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;


public class UtilityBelt{
    //KEYS:
    private final static ByteBuffer KEY_MIN_INTERVAL = ByteBuffer.wrap(new byte[]
            {'m','i','n',' ','i','n','t','e','r','v','a','l'});
    private final static ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[]
            {'i','n','t','e','r','v','a','l'});
    private final static ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[]
            {'p','e','e','r','s'});
    private final static ByteBuffer KEY_PEERID= ByteBuffer.wrap(new byte[]
            {'p','e','e','r',' ','i','d'});
    private final static ByteBuffer KEY_IP   = ByteBuffer.wrap(new byte[]
            {'i','p'});
    private final static ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[]
            {'p','o','r','t'});
    private final static String KEY_VALIDATE_PEER= new String("-AZ5400-");
    
    private static final char[] HEX_CHARS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    //GET KEYS
    public static ByteBuffer getKey(String keyid){
        ByteBuffer retval = null;
        if(keyid.equals("interval"))return KEY_INTERVAL;
        if(keyid.equals("peers"))   return KEY_PEERS;
        if(keyid.equals("peer id")) return KEY_PEERID;
        if(keyid.equals("ip"))      return KEY_IP;
        if(keyid.equals("port"))    return KEY_PORT;
        if(keyid.equals("min interval"))return KEY_MIN_INTERVAL;
        return null;
    }
    public static String validate(){
        return KEY_VALIDATE_PEER;
    }
    /*
     * HELPER methods
     */
        //Get the escaped hash
    public static String toURLHex(byte[] bytes){
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < bytes.length; ++i){
            sb.append('%')
                .append(HEX_CHARS[(bytes[i]>>4&0x0F)])
                .append(HEX_CHARS[(bytes[i]&0x0F)]);
        }
        return sb.toString();
    }
    //Randomly generate a peer id with restrictinos on first five characters
    public static String generatePeerID(){
        //everything excluding "B" to guarantee not having RUBT int he beginning
        char[] chars = 
            "ACDEFGHIJKLMNOPQRSTUVWXYZacdefghijklmnopqrstuvwxyz1234567890".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for(int i = 0; i < 20; i++){
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }
    
    //Get response from URL
    public static byte[] getURL(String string_url){
        URL tracker_connect_url;
        URLConnection tracker_connection;
        BufferedInputStream response_reader;
        ByteArrayOutputStream temp_output;
        byte[] buf = new byte[1],
                tracker_response;
        try{

            tracker_connect_url = new URL(string_url);
            tracker_connection  =
                (HttpURLConnection)tracker_connect_url.openConnection();

            response_reader = 
                new BufferedInputStream(tracker_connection.getInputStream());
            temp_output     = new ByteArrayOutputStream();

            while(response_reader.read(buf) != -1){
                temp_output.write(buf);
            }
              
            response_reader.close();
            tracker_response = temp_output.toByteArray();
            return tracker_response;

        }catch(Exception e){
            System.out.println(e);
        }
        return null;
    }
    
    //from buffer to string
    public static String bufferToString(ByteBuffer buffer){
        byte[] bufferBytes = new byte[buffer.capacity()];
        buffer.get(bufferBytes, 0, bufferBytes.length);
        String value = new String(bufferBytes);
        return value;
    }

    public static boolean[] convertBytes(byte[] bits, int sig_bits){
        boolean[] returnValue = new boolean[sig_bits];
        int boolIndex = 0;
        for (int byteIndex = 0; byteIndex < bits.length; ++byteIndex) {
            for (int bitIndex = 7; bitIndex >= 0; --bitIndex) {
                if (boolIndex >= sig_bits) {
                    return returnValue;
                }

                returnValue[boolIndex++] = (bits[byteIndex] >> bitIndex & 0x01) == 1 ? true : false;
            }
        }
        return returnValue;

    }

    public static boolean[] convertBytes(byte[] bits) {
        return UtilityBelt.convertBytes(bits, bits.length * 8);
    }

    public static byte[] convertBytes(boolean[] bools) {
        int length = bools.length/8;
        int mod = bools.length % 8;
        if(mod != 0){
            ++length;
        }
        byte[] returnValue = new byte[length];
        int boolIndex = 0;
        for (int byteIndex = 0; byteIndex < returnValue.length; ++byteIndex) {
            for (int bitIndex = 7; bitIndex >= 0; --bitIndex) {
                if (boolIndex >= bools.length) {
                    return returnValue;
                }
                if (bools[boolIndex++]) {
                    returnValue[byteIndex] |= (byte) (1 << bitIndex);
                }
            }
        }
        return returnValue;
    }

}
