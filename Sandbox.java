import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Timer;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.ColorModel;
import java.awt.Color;
import geom.*;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.ObjectOutputStream;
import java.io.IOException;
/**
 * Eine virtuelle Umgebung aus Blöcken
 * 
 * @Benny: Bitte bedenke, dass die Sandboxes mit dem Serializer gespeichert werden können müssen (teutsche Sprache, swerer Schprache) und du Änderungen
 * auch dort hinzufügen musst. Wie immer ist Linus dein Ansprechpatner
 */
public abstract class Sandbox implements Serializable
{
    public transient Block[][]map;
    // Sandboxen können Sandboxen enthalten (Kompositum). z.B.: Schiff auf Planet
    protected transient ArrayList<Sandbox> subsandboxes = new ArrayList<Sandbox>(); //Namensänderung, war früher "sandboxes"
    protected transient Timer spaceTimer; //nur eine Referenz
    /**
     * erstellt eine neue Sandbox
     * @param: Vektor size: gibt die größe der Sandbox an (Bereich in dem Blöcke sein können)
     */
    public Sandbox(VektorI size, Timer spaceTimer){
        map = new Block[size.x][size.y];
        this.spaceTimer=spaceTimer;
        this.spaceTimerSetup();
    }
    
    public Sandbox(Block[][] map, ArrayList<Sandbox> subsandboxes, Timer spaceTimer){
        this.map=map;
        this.subsandboxes=subsandboxes;
        this.spaceTimer=spaceTimer;
        this.spaceTimerSetup();
    }
    
    public void setSpaceTimer(Timer t){
        this.spaceTimer=t;
        this.spaceTimerSetup();
    }
    
    protected abstract void spaceTimerSetup();
    //Nur hier können neue TimerTasks hinzugefügt werden.
        
    /**
     * gibt die Größe der Sandbox zurück
     */
    public VektorI getSize(){
        return new VektorI(map.length, map[0].length);
    }
    
    /**
     * Ersetzt die Map mit einer anderen
     */
    public void setMap(Block[][]map){
        if(map!= null)this.map = map;
    }
    
    /**
     * Fügt eine Sandbox hinzu
     */
    public void addSandbox(Sandbox sbNeu){
        if(sbNeu!=null)subsandboxes.add(sbNeu);
    }
    
    /**
     * Löscht eine Sandbox
     */
    public void removeSandbox(Sandbox sbR){
        if(sbR!=null)subsandboxes.remove(sbR);
    }
    
    public ArrayList<Sandbox> getSubsandboxes(){
        return subsandboxes;
    }
    
    /**
     * Setzt einen Block in die Welt
     */
    public boolean placeBlock(Block block, VektorI pos){
        try{
            if (map[pos.x][pos.y] == null){
                map[pos.x][pos.y]=block; 
                System.out.println("Block at "+pos.toString()+" placed!");
                return true;
            }else{
                return false;
            }
        }catch(Exception e){ return false; }
    }
    
    /**
     * Gibt die obere rechte Ecke (int Blöcken) der Spieleransicht an
     * @param: pos: Position des Spielers relativ zur oberen rechten Ecke der Sandbox
     * 
     * @Benny:
     * Das hat Linus programmiert. Die Bilder aller Blöcke werden zuerst zusammengeführt in ein großes Bild und dann nur dieses Bild "gezeichnet". 
     * Das ist deutlich schneller als jedes Bild einzeln zu zeichen. Bitte setz dich mit Linus (König der Kommentare) in Verbindung um das zu verstehen
     * und zu verbessern. Man kann z.B. zur Zeit nur ganze Koordianten darstellen...
     */
    public VektorD getUpperLeftCorner(VektorD pos){
        return pos.add( Settings.PLAYERC_FIELD_OF_VIEW.toDouble().multiply(-0.5) ).add(new VektorD(0.5,0.5));
    }
    
    /**
     * Gibt die Position eines Blocks an
     * 
     * @param: 
     * bPos: Position des Blocks relativ zur oberen rechten Ecke der Spieleransicht in Pixeln
     * pPos: Position des Spielers relativ zur oberen rechten Ecke der Sandbox in Blöcken
     * blockBreite: Breite eines Blocks in Pixeln
     */
    public VektorI getPosToPlayer(VektorI bPos, VektorD pPos, int blockBreite){
        //System.out.println(bPos.toString()+" "+bPos.toDouble().divide(blockBreite).toString());
        return (getUpperLeftCorner(pPos).add(bPos.toDouble().divide(blockBreite))).toIntFloor();
    }
    
    /**
     * Grafik ausgeben
     * @param: 
     * pos: Position des Spielers relativ zur oberen rechten Ecke der Sandbox
     * blockBreite: Breite eines Blocks in Pixeln
     */
    public void paint(Graphics g, VektorI screenSize, VektorD pos, int blockBreite){
        VektorI upperLeftCorner = getUpperLeftCorner(pos).toInt();  // obere linke Ecke der Spieleransicht relativ zur oberen linken Ecke der sb
        VektorI bottomRightCorner = upperLeftCorner.add(Settings.PLAYERC_FIELD_OF_VIEW);  // untere rechte Ecke der Spieleransicht relativ zur oberen linken Ecke der sb
        //System.out.println("UpperLeftCorner: "+ upperLeftCorner.toString()+ " BottomRightCorner: " + bottomRightCorner.toString());
        
        ColorModel cm=ColorModel.getRGBdefault();
        BufferedImage image=new BufferedImage(cm,cm.createCompatibleWritableRaster(Settings.PLAYERC_FIELD_OF_VIEW.x*blockBreite,Settings.PLAYERC_FIELD_OF_VIEW.y*blockBreite),false,new Hashtable<String,Object>());
        //alle hier erstellten BufferedImages haben den TYPE_INT_ARGB
        int[] oldImageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        
        Hashtable<String,BufferedImage> blockImages=new Hashtable<String,BufferedImage>(); //Skalierung
        for (int x = upperLeftCorner.x; x<=bottomRightCorner.x; x++){
            for (int y = upperLeftCorner.y; y<=bottomRightCorner.y; y++){
                Block block=map[x][y];
                if (block!=null && blockImages.get(block.getName())==null){
                    BufferedImage img=block.getImage();
                    Hashtable<String,Object> properties=new Hashtable<String,Object>();
                    String[] prns=image.getPropertyNames();
                    if (prns!=null){
                        for (int i=0;i<prns.length;i++){
                            properties.put(prns[i],image.getProperty(prns[i]));
                        }
                    }
                    BufferedImage img2=new BufferedImage(cm,cm.createCompatibleWritableRaster(blockBreite,blockBreite),false,properties);
                    Graphics gr=img2.getGraphics();
                    gr.drawImage(img,0,0,blockBreite,blockBreite,null);
                    blockImages.put(block.getName(),img2);
                }
            }
        }
        
        for (int x = upperLeftCorner.x; x<bottomRightCorner.x; x++){
            for (int y = upperLeftCorner.y; y<bottomRightCorner.y; y++){
                Block block = map[x][y];
                if(block != null){
                    BufferedImage img=blockImages.get(block.getName());
                    int[] imgData=((DataBufferInt) img.getRaster().getDataBuffer()).getData();
                    for (int i=0;i<blockBreite;i++){
                        int index = ((y-upperLeftCorner.y)*blockBreite + i)*Settings.PLAYERC_FIELD_OF_VIEW.x*blockBreite + (x-upperLeftCorner.x)*blockBreite;
                        System.arraycopy(imgData,i*blockBreite,oldImageData,Math.min(index,oldImageData.length-blockBreite-1),blockBreite);
                    }
                }
            }
        }
        g.setColor(new Color(0,0,0,1));
        g.drawImage(image,0,0,new Color(0,0,0,255),null);
    }
}
