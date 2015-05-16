/*
 * Andre Marquez
 * Shane O'Hanlon
 */
public class Block{
    public int
        index,
        size;
    public byte[]
        data;
    boolean full;

    Block(int _size, int _index){
        size = _size;
        index = _index;
        full = false;
        data = new byte[size];
    }

    void data(byte[] _data){
        if(_data.length != this.size) System.out.println("\n Warning data.length != size, at index" + index);
        for(int i =0; i < size; i++){
            data[i] = _data[i];
        }
        this.full = true;
    }

    boolean full(){
        return this.full;
    }
}
