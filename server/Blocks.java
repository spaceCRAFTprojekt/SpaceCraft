package server;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * hier wird je ein Objekt jedes Blocks gespeichert (z.B.: Erde, Sand, Stein, Wasser)
 * Soll nie initialisiert werden!
 * 
 * v0.0.6 AK * Alles geändert  (auch in Mapgen anpassen)!!!
 */
public abstract class Blocks
{
    public static final HashMap<Integer,Block> blocks = new HashMap<Integer,Block>();
    
    static{
        new Block(000, "stone", "blocks_stone"); 
        new Block(001, "dirt", "blocks_dirt"); 
        new Block(002, "grass", "blocks_grass"); 
        new Blocks_Note(104); // id kann noch verändert werden
        new Blocks_Piston(300);
        // piston_on: 301
        // piston_front: 302
    }
    /**
     * gibt den Block mit der id zuück
     */
    static Block get(int id){
        return blocks.get(id);
    }
    /**
     * gibt den Block mit dem Namen name zurück.
     * Wenn möglich besser get(int id) verwenden, da dafür weniger Rechenzeit benötigt wird!
     */
    static Block get(String name){
        for (Entry<Integer, Block> entry : blocks.entrySet()) {
            if (entry.getValue().getName() == name) {
                return entry.getValue();
            }
        }
        return null;
    }
}