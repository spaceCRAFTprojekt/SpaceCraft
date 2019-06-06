package blocks;

 

import java.util.HashMap;
import java.util.Map.Entry;
import java.awt.image.BufferedImage;
import items.*;

/**
 * hier wird je ein Objekt jedes Blocks gespeichert (z.B.: Erde, Sand, Stein, Wasser)
 * Soll nie initialisiert werden!
 * 
 * v0.0.6 AK * Alles ge�ndert
 * 
 * v0.3.2_AK * mit { @Override public void setProperties(){breakment_prediction = false;}};  k�nnen Properties von Bl�cken �berschrieben werden
 */
public abstract class Blocks
{
    public static final HashMap<Integer,Block> blocks = new HashMap<Integer,Block>();
    
    static{
        //System.out.println("static");
        
        new Block(000, "grass", "blocks_grass", true);
        new Block(001, "dirt", "blocks_dirt", true); 
        new Block(002, "stone", "blocks_stone", true);
        new Block(003, "silver", "blocks_silver_ore", true);
        new Block(004, "gold", "blocks_gold_ore", true);
        new Block(005, "copper", "blocks_copper_ore", true);
        new Block(010, "tree", "blocks_tree", true);
        new Block(011, "tree1", "blocks_tree1", true);
        new Block(013, "leaves", "blocks_leaves", true);
         
        
        CraftingRecipes.registerCraftingRecipe(new CraftingRecipe(0, 0, 0, 0, -1, 0, 0, 0, 0, 100, 2));
        new Blocks_Chest(100);
        new Blocks_Note(104); // id kann noch verändert werden
        new Blocks_Piston(300);
      
        // piston_on: 301
        // piston_front: 302
    }
    /**
     * gibt den Block mit der id zu�ck
     */
    public static Block get(int id){
        return blocks.get(id);
    }
    public static BufferedImage getTexture(int id){
        Block block = get(id);
        if(block != null)return block.getImage();
        else return null;
    }
    /**
     * gibt den Block mit dem Namen name zur�ck.
     * Wenn m�glich besser get(int id) verwenden, da daf�r weniger Rechenzeit ben�tigt wird!
     */
    public static Block get(String name){
        for (Entry<Integer, Block> entry : blocks.entrySet()) {
            if (entry.getValue().getName() == name) {
                return entry.getValue();
            }
        }
        return null;
    }
}
