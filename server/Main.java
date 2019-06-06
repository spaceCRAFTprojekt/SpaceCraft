package server;
import client.Player;
import client.PlayerS;
import client.PlayerC;
import client.Request;
import client.OtherPlayerTexture;
import client.PlayerTexture;
import client.Task;
import util.geom.VektorI;
import java.util.ArrayList;
import java.util.Timer;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import util.geom.VektorD;
import items.*;

import blocks.*;
/**
 * Der Server, der gro�e Teile des Spielinhalts erledigt. F�r Formalia ist der ServerCreator zust�ndig.
 * History:
 * 0.0.2 AK * erstellt
 * 0.0.3 AK * Spawnplanet in Player verlegt
 * 0.0.5 LG * Serialisierung
 */
public class Main implements Serializable
{
    /**
     * Nur ein Main pro Kopie des Spiels. (Es sind ja sowieso alle Mains (bisher) am selben Port und werden
     * in der selben Datei gespeichert, also ist das vielleicht gar nicht so bl�d, wie es aussieht. Vielleicht.
     */
    //eigentlich nicht n�tig
    public static Main main;
    public static final long serialVersionUID=0L;
    
    /**
     * Liste aller Spieler (Kopien), wird mit dem Client synchronisiert.
     */
    private ArrayList<Player> players = new ArrayList<Player>();
    private ArrayList<String> passwords = new ArrayList<String>(); //ziemlich unsicher!
    private transient ArrayList<String> chat=new ArrayList<String>();
    /**
     * Der Weltraum, der Massen enth�lt, die Sandboxen enthalten
     */
    private Space space;
    /**
     * Dieser k�mmert sich um die tats�chliche Server-Client-Verbindung.
     */
    private transient ServerCreator sc;

    /**
     * "Lasset die Spiele beginnen" ~ Kim Jong Un
     * @param:
     * boolean useOldData: true = "alten" Spielstand laden
     *                     false = neues Spiel beginnen  !�berschreibt "alten" Spielstand!
     */
    public static Main newMain(boolean useOldData){
        String folder=Settings.GAMESAVE_FOLDER;
        if (useOldData && new File(folder+File.separator+"main.ser").exists()){
            try{
                return Serializer.deserialize();
            }
            catch(Exception e){}
        }
        try{
            for(File file: new File(folder).listFiles()) //aus https://stackoverflow.com/questions/13195797/delete-all-files-in-directory-but-not-directory-one-liner-solution (18.4.2019)
                file.delete();
        }catch(Exception e){}
        Main m = new Main();    
        main=m;
        return m;
    }

    /**
     * Konstruktor
     * erstellt ein neues Spiel und keinen neuen Spieler
     * Privat, da newMain verwendet werden sollte.
     */
    private Main()
    {
        System.out.println("\n==================\nSpaceCraft startet\n==================\n");
        serverCreatorSetup();
        space = new Space(this,1); //keine Beschleunigung im Space
    }

    /**
     * "Ich warte" ~ DB Kunde
     * auf die Beschreibung @LG LG
     * Setup-Funktion, aufgerufen nach der Deserialisierung.
     * "Transiente" Variablen werden nicht serialisiert, m�ssen also hier neu erstellt werden.
     * LG
     */
    public Object readResolve() throws ObjectStreamException{
        serverCreatorSetup();
        String folder=Settings.GAMESAVE_FOLDER;
        if (!new File(folder).isDirectory()){
            System.out.println("Folder "+folder+" does not exist.");
            return null;
        }
        this.chat=new ArrayList<String>();
        main=this;
        return this;
    }

    /**
     * Der ServerCreator organisiert den Server. Diese Funktion ist wichtig. ~Schnux Sonst wär sie wahrscheinlich nicht da ~unknown
     */
    public void serverCreatorSetup(){
        this.sc=new ServerCreator(this);
    }

    /**
     * gibt !!! zu Testzwecken !!! den Bildschirm aller Spieler neu aus
     */
    public void repaint()
    {
        for (int i = 0; i<players.size();i++){
            newTask(i,"Player.repaint");
        }
    }

    /**
     * gibt das Space Objekt zur�ck
     */
    public Space getSpace(){
        return space;
    }

    /**
     * gibt das Spieler Objekt mit dem Namen name zur�ck
     * wenn der Spieler nicht vorhanden ist: null
     */
    public Player getPlayer(String name){
        for(int i = 0; i<players.size(); i++){
            if(players.get(i).getName() == name) return players.get(i);
        }
        return null;
    }

    public Player getPlayer(int id){
        if (id>=0 && id<players.size()){
            return players.get(id);
        }
        return null;
    }
    
    public int getPlayerNumber(){
        return players.size();
    }

    public void exitIfNoPlayers(){
        for(int i = 0; i<players.size();i++){
            if(players.get(i).isOnline())return; // wenn ein Spieler online ist abbrechen
        }
        exit(); // sonst Spiel beenden
    }

    /**
     * Schlie�t das Spiel UND speichert den Spielstand!!!
     */
    public void exit(){
        System.out.println("\n===================\nSpaceCraft schlie�t\n===================\n");
        for (int i=0;i<players.size();i++){
            if (players.get(i).isOnline()){
                players.get(i).logout(); //Server-Kopie des Players
                newTask(i,"Player.logoutTask"); //Player im Client
                sc.taskOutputStreams.remove(i);
            }
        }
        Serializer.serialize(this);
        main=null;
        System.exit(0);
    }

    public ServerCreator getServerCreator(){
        return sc;
    }
    
    /**
     * Diese Funktion sollte verwendet werden, um neue Tasks zu erstellen.
     */
    public void newTask(int playerID, String todo, Object... params){
        Task task=new Task(todo, params);
        sc.sendTask(playerID,task);
    }

    //Ab hier Request-Funktionen
    /**
     * Request-Funktion
     */
    public void exit(Integer playerID){
        if (players.get(playerID).isAdmin())
            exit();
        else
            noAdminMsg(playerID);
    }
    
    /**
     * Request-Funktion
     */
    public void exitIfNoPlayers(Integer playerID){
        if (players.get(playerID).isAdmin())
            exitIfNoPlayers();
        else
            noAdminMsg(playerID);
    }
    
    /**
     * Request-Funktion
     */
    public Boolean login(Integer playerID, String password){
        if (passwords.get(playerID).equals(password)){
            players.get(playerID).setOnline(true); //wirkt auf die Kopie in der Liste, der Player im Client setzt sich selbst online
            return new Boolean(true);
        }
        return new Boolean(false);
    }
    
    /**
     * Request-Funktion
     */
    public Boolean logout(Integer playerID){
        players.get(playerID).setOnline(false); //siehe login(Integer playerID)
        sc.taskOutputStreams.remove(playerID);
        return new Boolean(true);
    }
    
    /**
     * Request-Funktion
     * sollte aufgerufen werden, wenn der Spieler ein Men� schlie�t
     */
    public Boolean returnFromMenu(Integer playerID, String menuName, Object[] menuParams){
        try{
            if (menuName.equals("NoteblockMenu")){  // @Käpt'n ernsthaft? Kann man das nicht in die entsprechende Klasse auslagern???
                Sandbox sb = getSandbox((Integer)menuParams[0]);
                Meta mt=sb.getMeta((VektorI) menuParams[1]);
                if (mt!=null){
                    mt.put("text",menuParams[2]);
                    return new Boolean(true);
                }
                return new Boolean(false);
            }else if(menuName.equals("ChestMenu")){
                Sandbox sb = getSandbox((Integer)menuParams[0]);
                Meta mt=sb.getMeta((VektorI) menuParams[1]);
                Inv inv_main = (Inv)menuParams[2];
                if(inv_main != null && mt != null){
                    mt.put("inv_main", inv_main);
                    return new Boolean(true);
                }
            }
        }catch(Exception e){System.out.println("Exception in server.Main.returnFromMenu(): "+ e);}
        return new Boolean(false);
    }
    
    public Sandbox getSandbox(Integer sandboxIndex){ //Ich glaube, dass diese Funktion meistens vergessen wird und die h�ssliche Schreibweise verwendet wird.
        return ((Mass) space.masses.get(sandboxIndex)).getSandbox();
    }

    /**
     * Der Status des Players im Client hat sich ver�ndert, also macht er einen Request, damit der Status der Kopie des Players im Server genauso ist.
     */
    public void synchronizePlayerVariable(Integer playerID, String varname, Class cl, Object value) throws NoSuchFieldException, IllegalAccessException{
        try{
            Player p=players.get(playerID);
            //schlechte Sicherheits�berpr�fungen
            if (!p.isAdmin())
                if (varname=="currentMassIndex" && p.getPlayerS().reachedMassIDs.indexOf((int) value)==-1)
                    noAdminMsg(playerID);
                else if (varname=="isAdmin")
                    noAdminMsg(playerID);
            Class pc=Player.class;
            Field f=pc.getDeclaredField(varname);
            f.set(p,value);
        }
        catch(IndexOutOfBoundsException e){} //Warum das? Ich habe es selbst geschrieben und wieder vergessen. -LG
    }
    
    /**
     * Request-Funktion
     * Siehe synchronizePlayerVariable
     */
    public void synchronizePlayerSVariable(Integer playerID, String varname, Class cl, Object value) throws NoSuchFieldException, IllegalAccessException{
        try{
            PlayerS p=players.get(playerID).getPlayerS();
            if (!players.get(playerID).isAdmin())
                if (varname=="reachedMassIDs")
                    noAdminMsg(playerID);
            Class pc=PlayerS.class;
            Field f=pc.getDeclaredField(varname);
            f.set(p,value);
        }
        catch(IndexOutOfBoundsException e){}
    }
    
    /**
     * Request-Funktion
     * Siehe synchronizePlayerVariable
     */
    public void synchronizePlayerCVariable(Integer playerID, String varname, Class cl, Object value) throws NoSuchFieldException, IllegalAccessException{
        try{
            PlayerC p=players.get(playerID).getPlayerC();
            if (!players.get(playerID).isAdmin())
                if (varname=="pos" && p.pos.subtract((VektorD) value).getLength()>20)
                    noAdminMsg(playerID);
            Class pc=PlayerC.class;
            Field f=pc.getDeclaredField(varname);
            f.set(p,value);
        }
        catch(IndexOutOfBoundsException e){}
    }

    /**
     * neuer Spieler
     * Request-Funktion!
     * playerID wird bei Requests standardm��ig �bergeben, ist hier aber ohne Belang (-1).
     * Return-Wert: Kein Erfolg: -1, sonst die ID
     * Erstellt nur die Kopie des Players am Server. Um einen Player mit Client zu erstellen, wird static client.Player.newPlayer(String name) verwendet.
     */
    public Integer newPlayer(Integer playerID, String name, String password)
    {
        if (getPlayer(name) != null)return new Integer(-1);
        int id=players.size();
        Player p;
        if (id==0) //der erste Spieler ist automatisch Administrator
            p=new Player(id, name, false, true);
        else
            p=new Player(id, name, false, false);
        players.add(p);
        passwords.add(password);
        return id;
    }

    /**
     * Request-Funktion
     * Gibt die Kopie des Players hier vom Server zurück. Zur Synchronisierung (siehe Player.synchronizeWithServer)
     */
    public Player retrievePlayer(Integer playerID){
        return players.get(playerID);
    }
    
    /**
     * Request-Funktion
     */
    public void writeIntoChat(Integer playerID, String message){
        String msg = players.get(playerID).getName()+": "+message;
        chat.add(msg);
        for (int i=0;i<players.size();i++){
            if (players.get(i).isOnline()){
                newTask(i, "Player.addChatMsg", msg);
            }
        }
    }
    /**
     * Request-Funktion
     */
    public void serverChatMsg(Integer playerID, String message){
        String msg = message;
        chat.add(msg);
        for (int i=0;i<players.size();i++){
            if (players.get(i).isOnline()){
                newTask(i, "Player.addChatMsg", msg);
            }
        }
    }
    
    /**
     * Request-Funktion
     */
    public String[] getChatContent(Integer playerID, Integer numLines){
        //die letzten (numLines) Zeilen
        String[] ret=new String[numLines];
        int chatSize=chat.size();
        for (int i=0;i<numLines;i++){
            if (chatSize-numLines+i>=0){
                ret[i]=chat.get(chatSize-numLines+i);
            }
            else{
                ret[i]="";
            }
        }
        return ret;
    }
    
    /**
     * Request-Funktion mit �blicherweise playerID=-1 (ist egal)
     */
    public Player getPlayer(Integer playerID, String name){
        for(int i = 0; i<players.size(); i++){
            //aus irgendeinem Grund geht == nicht mit Requests
            if(players.get(i).getName().equals(name)) return players.get(i);
        }
        return null;
    }

    /**
     * Warum kann ich ein schei� Object[] nicht in ein noch blöderes OtherPlayerTexture[] casten?!?!?!
     * Daher wird Ihnen hier ein schei� Obejct[] zur�ckgegeben :(  
     */
    public Object[] getOtherPlayerTextures(Integer playerID, VektorI upperLeftCorner, VektorI bottomRightCorner){
        if(players.size() < 2)return null; // wenn es nur einen Spieler gibt (Singleplayer), dann null.
        ArrayList<OtherPlayerTexture> ret = new ArrayList<OtherPlayerTexture>();
        int massID = players.get(playerID).getCurrentMassIndex();
        for(int i = 0; i<players.size(); i++){
            if(playerID != i && players.get(i).isOnline() && players.get(i).getCurrentMassIndex() == massID){  // der Spieler selbst soll natürlich nicht im Array zurückgegeben werden
                PlayerC pC = players.get(i).getPlayerC();
                VektorI pos = pC.pos.toInt();
                if(pos.x >= upperLeftCorner.x && pos.y >= upperLeftCorner.y && pos.x <= bottomRightCorner.x && pos.y <= bottomRightCorner.y){
                    PlayerTexture t = pC.getPlayerTexture();
                    ret.add(new OtherPlayerTexture(i, t.mode, t.textureID, pC.pos, players.get(i).getName()));
                }
            }
        }
        return (ret.toArray());
    }
    
    /**
     * etwas bequemer
     */
    public void noAdminMsg(int playerID){
        newTask(playerID,"Player.addChatMsg","Du bist kein Administrator.");
    }
}
// Hallo ~unknown