import java.util.ArrayList;
import java.util.Timer;
import java.util.HashMap;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
/**
 * Main Klasse des Spiels SpaceCraft
 * Im Mutliplayer Modus der "Server"
 * 
 * History:
 * 0.0.2 AK * erstellt
 * 0.0.3 AK * Spawnplanet in Player verlegt
 * 0.0.5 LG * Serialisierung
 */
public class Main implements Serializable
{
    static String spacefilename="space"; //sollteen die in Settings sein?
    static String playersfilename="players";
    static String shipCfilename="shipC";
    static String planetCfilename="planetC";
    static String blocksfilename="blocks";
    static String fileEnding=".ser";
    
    private transient ArrayList<Player> players = new ArrayList<Player>(); // normalerweise nur ein Spieler
    private transient Space space;

    /**
     * "Lasset die Spiele beginnen" ~ Kim Jong Un
     * @param:
     * boolean useOldData: true = "alten" Spielstand laden
     *                     false = neues Spiel beginnen  !überschreibt "alten" Spielstand!
     */
    public static Main newMain(boolean useOldData){
        String folder=Settings.GAMESAVE_FOLDER;
        if (useOldData && new File(folder+File.separator+blocksfilename+fileEnding).exists() &&
                          new File(folder+File.separator+spacefilename+fileEnding).exists() &&
                          new File(folder+File.separator+playersfilename+fileEnding).exists() &&
                          new File(folder+File.separator+planetCfilename+"0"+fileEnding).exists() &&
                          new File(folder+File.separator+"main.ser").exists()){ //mindestens einer
            try{
                return Serializer.deserialize();
            }
            catch(Exception e){}
        }
        for(File file: new File(folder).listFiles()) //aus https://stackoverflow.com/questions/13195797/delete-all-files-in-directory-but-not-directory-one-liner-solution (18.4.2019)
            file.delete();
        return new Main();
    }
    
    public static Main newMain(){
        return newMain(false);
    }
    
    /**
     * Konstruktor
     */
    private Main()
    {
        System.out.println("\n==================\nSpaceCraft startet\n==================\n");
        space = new Space(10); //10-fache Beschleunigung im Space
        newPlayer("Singleplayer");
    }
    
    /**
     * "Ich warte" ~ DB Kunde
     * auf die Beschreibung @LG LG
     * Instruktionen zur Serialisierung, siehe Readme.
     * LG
     */
    private Object writeReplace() throws ObjectStreamException{
        String folder=Settings.GAMESAVE_FOLDER;
        new File(folder).mkdirs();
        
        HashMap<Integer,Block> blocks=Blocks.blocks; //Blöcke
        try{
            FileOutputStream blo=new FileOutputStream(folder+File.separator+blocksfilename+fileEnding);
            ObjectOutputStream bloO=new ObjectOutputStream(blo);
            for (int i=0;i<blocks.size();i++){
                bloO.writeObject(blocks.get(i).getName());
                bloO.writeObject(blocks.get(i).getImageString());
            }
        }
        catch(Exception e){
            System.out.println(e+": "+e.getMessage());
        }
        
        ArrayList<ShipC> shipCs=ShipC.shipCs; //Schiffe
        for (int i=0;i<shipCs.size();i++){
            try{
                FileOutputStream sbo=new FileOutputStream(folder+File.separator+shipCfilename+i+fileEnding);
                ObjectOutputStream sboO=new ObjectOutputStream(sbo);
                sboO.writeObject(shipCs.get(i).map);
                sboO.writeObject(shipCs.get(i).getSubsandboxes());
                //sboO.writeObject(shipCs.get(i).getShipS());
            }
            catch(Exception e){
                System.out.println(e+": "+e.getMessage());
            }
        }
        
        ArrayList<PlanetC> planetCs=PlanetC.planetCs; //Planeten
        for (int i=0;i<planetCs.size();i++){
            try{
                FileOutputStream sbo=new FileOutputStream(folder+File.separator+planetCfilename+i+fileEnding);
                ObjectOutputStream sboO=new ObjectOutputStream(sbo);
                sboO.writeObject(planetCs.get(i).map);
                sboO.writeObject(planetCs.get(i).getSubsandboxes());
                //sboO.writeObject(planetCs.get(i).getPlanetS());
            }
            catch(Exception e){
                System.out.println(e+": "+e.getMessage());
            }
        }
        
        try{ //Space
            FileOutputStream spo=new FileOutputStream(folder+File.separator+spacefilename+fileEnding);
            ObjectOutputStream spoO=new ObjectOutputStream(spo);
            spoO.writeObject(space);
        }
        catch(Exception e){
            System.out.println(e+": "+e.getMessage());
        }
        
        try{ //Player
            FileOutputStream plo=new FileOutputStream(folder+File.separator+playersfilename+fileEnding);
            ObjectOutputStream ploO=new ObjectOutputStream(plo);
            ploO.writeObject(players);
        }
        catch(Exception e){
            System.out.println(e+": "+e.getMessage());
        }
        return this;
    }
    
    /**
     * "Ich warte" ~ DB Kunde
     * auf die Beschreibung @LG LG
     * Setup-Funktion, aufgerufen nach der Deserialisierung.
     * LG
     */
    public Object readResolve() throws ObjectStreamException{
        String folder=Settings.GAMESAVE_FOLDER;
        if (!new File(folder).isDirectory()){
            System.out.println("Folder "+folder+" does not exist.");
            return null;
        }
        
        try{
            FileInputStream bli=new FileInputStream(folder+File.separator+blocksfilename+fileEnding);
            ObjectInputStream bliO=new ObjectInputStream(bli);
            String name=null;
            String imageString=null;
            int aval=bliO.available();
            while(aval>0){
                Object o=bliO.readObject();
                if (name==null){
                    name=(String) o;
                }
                else if (imageString==null){
                    imageString=(String) o;
                }
                if (name!=null && imageString!=null){
                    new Block(name,imageString); //fügt sich automatisch in die HashMap ein
                    name=null;
                    imageString=null;
                }
                aval=bliO.available();
            }
        }
        catch(Exception e){
            System.out.println("Main: 1: "+e+": "+e.getMessage());
        }
        
        for (int i=0;i<Integer.MAX_VALUE;i++){
            try{
                if (new File(folder+File.separator+shipCfilename+i+fileEnding).exists()){
                    FileInputStream sbi=new FileInputStream(folder+File.separator+shipCfilename+i+fileEnding);
                    ObjectInputStream sbiO=new ObjectInputStream(sbi);
                    Block[][] map=(Block[][]) sbiO.readObject();
                    ArrayList<Sandbox> subsandboxes=(ArrayList<Sandbox>) sbiO.readObject();
                    ShipS shipS=null; //siehe unten warum
                    Timer spaceTimer=null;
                    new ShipC(map,subsandboxes,shipS,spaceTimer); //fügt sich automatisch in die ArrayList ein
                }
                else{
                    break;
                }
            }
            catch(Exception e){
                System.out.println("Main: 2: "+e+": "+e.getMessage());
            }
        }
        
        for (int i=0;i<Integer.MAX_VALUE;i++){
            try{
                if (new File(folder+File.separator+planetCfilename+i+fileEnding).exists()){
                    FileInputStream sbi=new FileInputStream(folder+File.separator+planetCfilename+i+fileEnding);
                    ObjectInputStream sbiO=new ObjectInputStream(sbi);
                    Block[][] map=(Block[][]) sbiO.readObject();
                    ArrayList<Sandbox> subsandboxes=(ArrayList<Sandbox>) sbiO.readObject();
                    PlanetS planetS=null; //Der PlanetS wird erst später (mit Space) hinzugefügt, um ein Problem mit einer zirkulären Referenz zu vermeiden.
                    Timer spaceTimer=null; //dito // ????
                    new PlanetC(map,subsandboxes,planetS,spaceTimer); //fügt sich automatisch in die ArrayList ein
                }
                else{
                    break;
                }
            }
            catch(Exception e){
                System.out.println("Main: 3: "+e+": "+e.getMessage());
            }
        }
        
        try{
            FileInputStream spi=new FileInputStream(folder+File.separator+spacefilename+fileEnding);
            ObjectInputStream spiO=new ObjectInputStream(spi);
            space=(Space) spiO.readObject();
        }
        catch(Exception e){
            System.out.println("Main: 4: "+e+": "+e.getMessage());
        }
        
        try{
            FileInputStream pli=new FileInputStream(folder+File.separator+playersfilename+fileEnding);
            ObjectInputStream pliO=new ObjectInputStream(pli);
            players=(ArrayList<Player>) pliO.readObject();
            for (int i=0;i<players.size();i++){
                players.get(i).setSpace(space);
                players.get(i).setMain(this);
            }
        }
        catch(Exception e){
            System.out.println("Main: 5: "+e+": "+e.getMessage());
        }
        return this;
    }
    
    /**
     * gibt !!! zu Testzwecken !!! den Bildschirm aller Spieler neu aus
     */
    public void repaint()
    {
        for (int i = 0; i<players.size();i++){
            players.get(i).repaint();
        }
    }
    
    /**
     * gibt das Space Objekt zurück
     */
    public Space getSpace(){
        return space;
    }

    
    /**
     * gibt das Spieler Objekt mit dem Namen name zurück
     * wenn der Spieler nicht vorhanden ist: null
     */
    public Player getPlayer(String name){
        for(int i = 0; i<players.size(); i++){
            if(players.get(i).getName() == name) return players.get(i);
        }
        return null;
    }
    
    /**
     * neuer Spieler (vorerst nur zu Testzwecken)
     */
    public String newPlayer(String name)
    {
        if (getPlayer(name) != null)return "Es gibt bereits einen Spieler mit dem Namen " + name + "!";
        Player p=new Player(name, space);
        p.setMain(this);
        players.add(p);
        p.toSpace();
        return "Spieler " + name + " erfolgreich erstellt";
    }
    
    /**
     * Löscht einen vorhanden Spieler; Beendet das Spiel wenn kein Spieler mehr da ist
     * Sollte den Spieler auch noch irgendwo speichern, oder?
     */
    public void removePlayer(String name){
        Player p = getPlayer(name);
        players.remove(p);
        if (players.size()==0){
            this.close();
        }
    }
    
    /**
     * Löscht einen vorhanden Spieler; Beendet das Spiel wenn kein Spieler mehr da ist
     * Sollte den Spieler auch noch irgendwo speichern, oder?
     */
    public void removePlayer(Player p){
        players.remove(p);
        if (players.size()==0){
            this.close();
        }
    }
    
    /**
     * Schließt das Spiel UND speichert den Spielstand!!!
     */
    public void close(){
        System.out.println("\n===================\nSpaceCraft schließt\n===================\n");
        Serializer.serialize(this);
        System.exit(0);
    }
}