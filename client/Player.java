package client;

import java.util.ArrayList;
import util.geom.*;
import menu.*;
import client.menus.*;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.SwingUtilities;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import items.Inv;
import items.Stack;
import items.Items;
import items.PlayerInv;
/**
 * Ein Spieler
 * Kann entweder in der Craft oder in der Space Ansicht sein
 * Alle Variablen, die synchronisiert werden m�ssen, m�ssen public und nicht transient sein.
 */
public class Player implements Serializable
{
    public static final long serialVersionUID=0L;
    private String name;
    /**
     * zum Senden von Daten, um ihn eindeutig zu identifizieren, Index in der server.Main.players-ArrayList
     */
    private int id;
    private PlayerS playerS;
    private PlayerC playerC;
    /**
     * Variable f�r die Kommunikation zwischen Server und Client (Requests)
     */
    private transient Socket requestSocket;
    /**
     * Variable f�r die Kommunikation zwischen Server und Client (Requests)
     */
    private transient ObjectOutputStream requestOut;
    /**
     * Variable f�r die Kommunikation zwischen Server und Client (Requests)
     */
    private transient ObjectInputStream requestIn;
    /**
     * Variable f�r die Kommunikation zwischen Server und Client (Tasks)
     */
    private transient Socket taskSocket;
    /**
     * Variable f�r die Kommunikation zwischen Server und Client (Tasks)
     */
    private transient ObjectOutputStream taskOut;
    /**
     * Variable f�r die Kommunikation zwischen Server und Client (Tasks)
     */
    private transient ObjectInputStream taskIn;
    /**
     * Variable f�r die Kommunikation zwischen Server und Client (Tasks)
     */
    private transient TaskResolver tr;
    private transient boolean online = false;
    /**
     * true: Der Spieler befindet sich am Client.
     * false: Der Spieler ist nur eine Kopie (d.h. ohne Sockets, Frame, Listener, Panels, Timer...) am Server
     */
    private boolean onClient;
    /**
     * true: die Ansicht des PlayerC wird gezeichnet
     * false: die Ansicht des PlayerS wird gezeichnet
     */
    public boolean inCraft;
    private transient Frame frame;
    /**
     * wenn ein Menu (z.B.: Escape Menu; ChestInterface gerade offen ist)
     */
    private transient Menu openedMenu = null;
    /**
     * der Index des Planeten, auf dem sich der Spieler gerade befindet
     */
    public int currentMassIndex;
    public transient OverlayPanelA opA;
    public transient ChatPanel chatP;
    /**
     * Bestimmte Sachen (gro�e Teleport-Spr�nge in Craft, setTime,...) gehen nur als Administrator.
     * Dieses Attribut ist nur f�r die Kopie des Spielers am Server relevant, am Client nicht.
     */
    private boolean isAdmin;
    /**
     * ob Spieler durch Bl�cke gehen kann
     */
    private boolean creative;
    /**
     * Erstellt neuen Spieler in einem Weltraum
     * 
     * @param:
     * int id: Index in der Playerliste in Main
     * String name: Name des Players
     * boolean onClient: ob der Player sich im Client befindet (ob also er synchronisiert wird oder nicht)
     * boolean isAdmin: ob der Player Administrator ist
     * Sollte nicht verwendet werden (stattdessen static newPlayer(String name)), au�er man wei�, was man tut.
     */
    public Player(int id, String name, boolean onClient, boolean isAdmin)
    {
        this.id=id;
        this.name = name;
        this.onClient=onClient;
        this.isAdmin=isAdmin;
        this.currentMassIndex=0;
        this.inCraft=false;
        //der Spawnpunkt muss nochmal �berdacht werden
        this.playerS=new PlayerS(this,new VektorD(0,0),currentMassIndex);
        this.playerS.reachedMassIDs.add(currentMassIndex);
        this.playerC=new PlayerC(this,new VektorD(50,50));  // spawn Position :)  Ein Herz f�r Benny :)
        //muss man hier auch schon synchronisieren?
    }
    
    /**
     * Diese Funktion erstellt einen neuen Spieler auf einem Server an der Adresse 
     * ClientSettings.SERVER_ADDRESS und dem Port ClientSettings.SERVER_PORT
     * (und macht einen Request, dass das auch am Server geschieht).
     */
    public static Player newPlayer(String name, String password){
        try(Socket s=new Socket(ClientSettings.SERVER_ADDRESS,ClientSettings.SERVER_PORT)){
            ObjectOutputStream newPlayerOut=new ObjectOutputStream(s.getOutputStream());
            synchronized(newPlayerOut){
                newPlayerOut.writeBoolean(true); //Request-Client
                newPlayerOut.writeInt(-1); //eigentlich playerID
                newPlayerOut.flush();
            }
            ObjectInputStream newPlayerIn=new ObjectInputStream(s.getInputStream());
            int id=(Integer) (new Request(-1,newPlayerOut,newPlayerIn,"Main.newPlayer",Integer.class,name,password).ret); //Kopie des Players am Server
            if (id!=-1){
                s.close();
                return new Player(id,name,true,false); //Player hier am Client, Passwort wird nicht am Client gespeichert
            }
            s.close();
        }
        catch(Exception e){
            System.out.println("[Client]: Exception when creating socket: "+e);
        }
        return null;
    }
    
    /**
     * Frame-Vorbereitung (Buttons, Listener etc.) nur hier
     */
    private void makeFrame(){
        this.frame = new Frame(name,new VektorI(928,608),this);
        Listener l=new Listener(this);
        this.frame.addKeyListener(l);
        this.frame.addMouseListener(l);
        this.frame.addMouseMotionListener(l);
        this.frame.addWindowListener(l);
        this.frame.addMouseWheelListener(l);
        //!!
        playerS.makeFrame(frame);
        playerC.makeFrame(frame);
        this.frame.getOverlayPanelS().setVisible(!inCraft);
        this.frame.getOverlayPanelC().setVisible(inCraft);
        this.opA = frame.getOverlayPanelA();
        this.chatP = new ChatPanel(getScreenSize(), opA);
    }
    
    public void disposeFrame(){
        if (frame == null)return;
        this.frame.dispose();
        frame = null;
    }
    
    /**
     * Erstellen zweier Sockets/Clients, einen f�r Requests, einen f�r Tasks
     */
    public void socketSetup() throws UnknownHostException, IOException{
        this.requestSocket=new Socket(ClientSettings.SERVER_ADDRESS,ClientSettings.SERVER_PORT);
        this.requestOut=new ObjectOutputStream(requestSocket.getOutputStream());
        this.requestIn=new ObjectInputStream(requestSocket.getInputStream());
        synchronized(requestOut){
            requestOut.writeBoolean(true); //Der Server muss ja wissen, was der Client eigentlich will. True steht für requestClient, false für TaskClient.
            requestOut.writeInt(id); //zur Identifizierung
            requestOut.flush();
        }
        this.taskSocket=new Socket(ClientSettings.SERVER_ADDRESS,ClientSettings.SERVER_PORT);
        this.taskOut=new ObjectOutputStream(taskSocket.getOutputStream());
        this.taskIn=new ObjectInputStream(taskSocket.getInputStream());
        synchronized(taskOut){
            taskOut.writeBoolean(false); //Task-Client
            taskOut.writeInt(id); //zur Identifizierung
            taskOut.flush();
        }
        this.tr=new TaskResolver(this);
    }
    
    public void socketClose() throws IOException{
        requestSocket.close();
        requestOut=null;
        requestIn=null;
        tr.close();
        tr=null;
        taskSocket.close();
        taskOut=null;
        taskIn=null;
    }
    
    public boolean login(String password){
        if(online)return false;
        if (onClient){
            System.out.println("[Client]: Login (Adresse = "+ClientSettings.SERVER_ADDRESS+", Port = "+ClientSettings.SERVER_PORT+")");
            try{
                socketSetup();
            }
            catch(Exception e){
                System.out.println("[Client]: Exception when creating socket: "+e);
            }
            Boolean success=(Boolean) (new Request(id,requestOut,requestIn,"Main.login",Boolean.class,password).ret);
            if (success){
                this.online = true;
                makeFrame();
                playerC.timerSetup();
            }
            else{
                System.out.println("[Client]: Kein Erfolg beim Einloggen");
                return false;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(){ 
                //automatisches Ausloggen, wenn die Virtual Machine beendet wird => ist etwas sicherer
                public void run(){
                    try{
                        System.out.println("[Client]: Player-Shutdown-Hook l�uft");
                    }
                    catch(Exception e){} //System.out schon geschlossen?
                    Player.this.logout();
                }
            });
            return success;
        }
        return false;
    }
    
    public void logout(){
        if(!online || !onClient)return;
        System.out.println("[Client]: Logout");
        new Request(id,requestOut,requestIn,"Main.logout",null, playerC.getInv());
        this.online = false;
        try{
            socketClose();
        }
        catch(Exception e){}
        playerC.timer.cancel();
        playerS.closeWorkspace(false);
        closeMenu();
        disposeFrame();
        new StartMenu();
    }
    
    /**
     * Wenn Main exited, dann werden alle Player rausgeschmissen (Es werden keine Requests mehr gestellt im Vergleich zu logout()).
     */
    public void logoutTask(){
        if (!online || !onClient)return;
        System.out.println("[Client]: Logout-Task");
        this.online=false;
        try{
            socketClose();
        }
        catch(Exception e){}
        playerC.timer.cancel();
        playerS.closeWorkspace(false);
        closeMenu();
        disposeFrame();
        new StartMenu();
    }
    
    /**
     * Wechselt die Ansicht zur Space Ansicht
     */
    public void toSpace()
    {
        if (!inCraft)return; // wenn der Spieler schon in der Space Ansicht ist, dann wird nichts getan
        inCraft = false;
        if (online && onClient)
            new Request(id,requestOut,requestIn,"Main.synchronizePlayerVariable",null,"inCraft",Boolean.class, inCraft);
        this.frame.getOverlayPanelS().setVisible(true);
        this.frame.getOverlayPanelC().setVisible(false);
        repaint();
    }
    
    /**
     * Wechselt die Ansicht zur Craft Ansicht
    */
    public void toCraft()
    {
        if (inCraft)return; // wenn der Spieler schon in der Craft Ansicht ist, dann wird nichts getan
        inCraft = true;
        if (online && onClient)
            new Request(id,requestOut,requestIn,"Main.synchronizePlayerVariable",null,"inCraft", Boolean.class, inCraft);
        this.frame.getOverlayPanelS().setVisible(false);
        this.frame.getOverlayPanelC().setVisible(true);
        repaint();
    }
    
    /**
     * Wechselt die Ansicht zur anderen Ansicht
     */
    public void changePlayer()
    {
        if (inCraft) toSpace(); else toCraft();
    }
    
    /**
     * soll aufgerufen werden, wenn ein Fenster ge�ffnet wird
     */
    public void openMenu(Menu menu)
    {
        this.openedMenu = menu;
    }
    
    /**
     * soll vom Fenster aus aufgerufen werden, wenn ein Fenster geschlossen wird
     */
    public void removeMenu()
    {
        this.openedMenu = null;
    }
    
    /**
     * schlie�t das gerade ge�ffnete Menu
     */
    public void closeMenu()
    {
        if (openedMenu == null)return;
        openedMenu.closeMenu();
        openedMenu = null;
    }
    
    /**
     * gibt false zur�ck, wenn gerade ein Men� offen ist
     */
    public boolean isActive()
    {
        return openedMenu == null;
    }
    
    public Menu getMenu(){
        return openedMenu;
    }
    
    /**
     * Task-Funktion
     */
    public void showMenu(String menuName, Object[] menuParams){
        if (menuName.equals("NoteblockMenu")){
            new NoteblockMenu(this,(int) menuParams[0], (VektorI) menuParams[1],(String) menuParams[2]);
        }else if(menuName.equals("ChestMenu")){
            new ChestMenu(this,(int) menuParams[0], (VektorI) menuParams[1],(Inv) menuParams[2]);
        }else if (menuName.equals("RocketControllerMenu")){
            new RocketControllerMenu(this,(int) menuParams[0], (VektorI) menuParams[1], (int) menuParams[2]);
        }
    }
    
    /**
     * schlie�t das gesamte Spiel (also auch den Server), falls m�glich
     * System.exit wird allerdings nicht aufgerufen (das muss einzeln z.B. im Escape-Men� geschehen).
     */
    public void exit(){
        if (online && onClient){
            new Request(id,requestOut,requestIn,"Main.exit",null);
        }
    }
    
    /**
     * Tastatur Event
     * @param:
     *  char type: 'p': pressed
     *             'r': released
     *             't': typed (nur Unicode Buchstaben)
     */
    public void keyEvent(KeyEvent e, char type) {
        if (!isActive())return;  // wenn ein Menü offen ist, dann passiert nichts
        if (e.getID() == KeyEvent.KEY_TYPED && e.getKeyChar()=='!'){ //kann den KeyCode nicht hernehmen, da es eine Kombination ist
            ChatWriterMenu cwm=new ChatWriterMenu(this);
            cwm.mtf.setText("!");
            return;
        }
        switch (e.getKeyCode()){
            case Shortcuts.open_escape_menu: 
                if (type != 'p') break;
                new EscapeMenu(this);
                break;
            case Shortcuts.change_space_craft:
                if (type != 'p') break;
                changePlayer();
                repaint();
                break;
            case Shortcuts.open_chat_writer:
                new ChatWriterMenu(this);
                break;
            default:
                if (inCraft)playerC.keyEvent(e,type);
                else playerS.keyEvent(e,type);
        }
    }
    
    /**
     * Mausrad"Event"
     * @param:
     * irgend ein EventObjekt; Keine Ahnung was das kann
     */
    public void mouseWheelMoved(MouseWheelEvent e){
        if (!isActive())return;  // wenn ein Menü offen ist, dann passiert nichts
        if(!inCraft)playerS.mouseWheelMoved(e);
        else playerC.mouseWheelMoved(e);
    }
    
    /**
     * Maus Event
     * @param:
     *  char type: 'p': pressed
     *             'r': released
     *             'c': clicked
     * entered und exited wurde nicht implementiert, weil es dafür bisher keine Verwendung gab
     */
    public void mouseEvent(MouseEvent e, char type) {
        MouseEvent eLayered = SwingUtilities.convertMouseEvent(frame, e, frame.getLayeredPane());  // �bersetzt das Koosy in das Koosy eines LayeredPanes (Issue #26)
        closeMenu();
        if (inCraft)playerC.mouseEvent(eLayered,type);
        else playerS.mouseEvent(eLayered,type);
    }
    
    public void windowEvent(WindowEvent e, char type){
        if (type=='c'){ //Schließen des Fensters
            logout();
        }
    }
    
    /**
     * gibt den Vektor der Gr��e des Bildschirms zurück
     */
    public VektorI getScreenSize(){
        return frame.getScreenSize();
    }
    
    /**
     * ...
     */
    public Frame getFrame(){
        return frame;
    }
    
    public PlayerC getPlayerC(){
        return playerC;
    }
    
    public PlayerS getPlayerS(){
        return playerS;
    }
    
    /**
     * Zeichnen
     */
    public void paint(Graphics g, VektorI screenSize){
        if (onClient && g!=null){
            if (inCraft && playerC != null)playerC.paint(g, screenSize);
            else if (playerS != null) playerS.paint(g, screenSize);
        }
    }
    
    /**
     * (auch) Task-Funktion
     */
    public void repaint(){
        if(frame!=null)frame.repaint();
    }
    
    /**
     * Synchronisierung mit der zu diesem Spieler geh�rigen Kopie am Server
     */
    public void synchronizeWithServer(){
        if (onClient && online){
            Player pOnServer=(Player) (new Request(id,requestOut,requestIn,"Main.retrievePlayer",Player.class).ret);
            synchronizeWithPlayerFromServer(pOnServer);
        }
    }
    
    public void synchronizeWithPlayerFromServer(Player pOnServer){
        inCraft=pOnServer.inCraft;
        currentMassIndex=pOnServer.currentMassIndex;
        //playerS.posToMass=pOnServer.getPlayerS().posToMass;
        playerS.scale=pOnServer.getPlayerS().scale;
        playerS.focussedMassIndex=pOnServer.getPlayerS().focussedMassIndex;
        playerS.reachedMassIDs=pOnServer.getPlayerS().reachedMassIDs;
        playerC.pos=pOnServer.getPlayerC().pos;
    }
    
    public void writeIntoChat(String message){
        if (online && onClient){
            if (message.charAt(0) == '!'){
                String msg = message.substring(1);
                String[] spl = msg.split(" ");
                switch (spl[0]){
                    case "hello":
                        addChatMsg("Es hat jemand Hallo geschrieben.");
                        break;
                    case "logout":
                        addChatMsg("Du wirst ausgeloggt...");
                        logout();
                        break;
                    case "afk":
                        serverChatMsg(name + " ist jetzt afk.");
                        break;
                    case "witzig":
                        serverChatMsg(name + " findet diese Aussage witzig.");
                        break;
                    case "nichtwitzig":
                        serverChatMsg(name + " findet diese Aussage nicht witzig.");
                        break;
                    case "giveme":
                        try{
                            String name = spl[1];
                            int amount;
                            if (spl.length>=3){
                                amount=Integer.parseInt(spl[2]);
                            }
                            else
                                amount=1;
                            Stack s = new Stack(Items.get(name),amount);
                            if (s.getItem()!=null){
                                playerC.getInv().addStack(s);
                                addChatMsg("Du hast " + amount+" " + name + " bekommen.");
                            }
                        }
                        catch(ArrayIndexOutOfBoundsException e){}
                        catch(NullPointerException e){}
                        break;
                    case "time": //hat vielleicht Bugs
                        try{
                            long time=Long.parseLong(spl[1]);
                            new Request(id,requestOut,requestIn,"Space.setTime",null,time);
                            if (playerS.getWorkspace()!=null){
                                playerS.getWorkspace().setTime(((Long) new Request(id,requestOut,requestIn,"Space.getInGameTime",Long.class).ret).longValue());
                            }
                        }
                        catch(Exception e){}
                        break;
                    case "timeSpeed": //hat vielleicht Bugs
                        try{
                            long dtime=Long.parseLong(spl[1]);
                            new Request(id,requestOut,requestIn,"Space.setDTime",null,dtime);
                            if (playerS.getWorkspace()!=null){
                                playerS.getWorkspace().inGameDTime=((Long) new Request(id,requestOut,requestIn,"Space.getInGameDTime",Long.class).ret).longValue();
                            }
                        }
                        catch(Exception e){}
                        break;
                    case "teleportS":
                        try{
                            int index=Integer.parseInt(spl[1]);
                            if (playerS.reachedMassIDs.indexOf(index)!=-1){
                                currentMassIndex=index;
                                new Request(id,requestOut,requestIn,"Main.synchronizePlayerVariable",null,"currentMassIndex",Integer.class,currentMassIndex);
                                addChatMsg("Teleport auf die Masse "+index+".");
                            }
                        }
                        catch(Exception e){}
                        break;
                    case "teleportC":
                        try{
                            double x=Double.parseDouble(spl[1]);
                            double y=Double.parseDouble(spl[2]);
                            playerC.pos=new VektorD(x,y);
                            new Request(id,requestOut,requestIn,"Main.synchronizePlayerCVariable",null,"pos",VektorD.class,playerC.pos);
                            addChatMsg("Teleport an die Stelle ("+x+"|"+y+")");
                        }
                        catch(Exception e){}
                        break;
                    case "players":
                        String[] names=(String[]) new Request(id,requestOut,requestIn,"Main.getOnlinePlayerNames",String[].class).ret;
                        String str="Spieler auf dem Server: ";
                        for (int i=0;i<names.length;i++){
                            str=str+names[i]+", ";
                        }
                        str=str.substring(0,str.length()-2); //letztes Komma
                        addChatMsg(str);
                        break;
                    default:
                        addChatMsg("Unbekannter Command...");
                }
            }
            else new Request(id,requestOut,requestIn,"Main.writeIntoChat",null,message);
        }
    }
    
    public void serverChatMsg(String message){
        new Request(id,requestOut,requestIn,"Main.serverChatMsg",null,message);
    }
    
    /**
     * Diese Methode (ein Task) empf�ngt eine neue Nachricht vom Server und zeigt sie an.
     */
    public void addChatMsg(String msg){
        chatP.add(msg);
    }
    
    /**
     * gibt den Namen des Spielers zur�ck
     */
    public String getName(){
        return name;
    }
    
    public int getID(){
        return id;
    }
    
    public boolean onClient(){
        return onClient;
    }
    
    public int getCurrentMassIndex(){
        return currentMassIndex;
    }
    
    public ObjectOutputStream getRequestOut(){
        return requestOut;
    }
    
    public ObjectInputStream getRequestIn(){
        return requestIn;
    }
    
    public ObjectOutputStream getTaskOut(){
        return taskOut;
    }
    
    public ObjectInputStream getTaskIn(){
        return taskIn;
    }

    public void setCurrentMassIndex(int cmi){
        currentMassIndex=cmi;
        if (online && onClient)
            new Request(id,requestOut,requestIn,"Main.synchronizePlayerVariable",null,"currentMassIndex",Integer.class, cmi);
    }
    
    public boolean isOnline(){
        return online;
    }
    
    /**
     * Diese Methode wird nur von der Kopie des Players im Server verwendet, der Player im Client macht das in login() und logout().
     */
    public void setOnline(boolean b){
        this.online=b;
    }
    
    public boolean isAdmin(){
        return isAdmin;
    }
    
    public boolean getCreative(){
        return creative;
    }
    
    public void setCreative(boolean b){
        this.creative=b;
    }
}