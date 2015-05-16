/*
 * Andre Marquez
 * Shane O'Hanlon
 */
import java.util.*;
public class Piece {
    public int
        index,
        blocksize,
        last_blocksize,
        rarity,
        size;
    public ArrayList<Integer> assigned;
    public Block[]
        blocks;
    public boolean 
        full;

    Piece(int _size, int _index){
        size = _size;
        blocksize = 16384;
        if(size/blocksize == 0){
            blocks = new Block[1];
        }
        else if(size%blocksize != 0){
            blocks = new Block[1 + (size/blocksize)];
        }
        else{
            blocks = new Block[size/blocksize];
        }
        for(int i=0;i<blocks.length-1; i++){
            blocks[i] = new Block(blocksize,i);
        }
        if(size%blocksize == 0){
            last_blocksize = blocksize;
        }
        else{
            last_blocksize = size%blocksize;
        }
        blocks[blocks.length-1] = new Block(last_blocksize, blocks.length-1);
        index = _index;
        full = false;
        assigned = new ArrayList<Integer>();
        rarity = 0;
    }
    void rarity(){
        this.rarity += 1;
    }

    void size(int new_size){
        this.size = new_size;
    }
    void addBlock(int index, Block block){
        blocks[index] = block;
    }
    int size(){
        return this.size;
    }
    boolean full(){
        if(!this.full){
            for(Block block : blocks){
                if(!block.full()) return false;
            }
        }
        this.full = true;
        return true;
    }
}
