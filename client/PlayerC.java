package client;

import util.geom.*;
import util.ImageTools;
import items.*;
import client.menus.*;
import menu.*;
import blocks.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;

import java.io.Serializable;
import java.io.ObjectStreamException;
// ich hab das mal geordnet ~ M�llmann

/**
 * der Craft-Teil eines Spielers
 * alle Variablen, die synchronisiert werden, m�ssen public sein
 * 
 * @History:
 * v0.5.2_AK
 */
public class PlayerC implements Serializable
{
    public static final long serialVersionUID=0L;
    public transient Timer timer; //public, damit er in logout() beendet werden kann
    
    /**
     * Breite eines Blocks in Pixeln
     */
    private int blockBreite = 32; //Hiermit pr�sentiere ich Ihnen die einzige deutsche Variablenbezeichnung im ganzen Spiel (gesch�tzt).
    private Player player;
    /**
     * Position
     */
    public VektorD pos;
    @Deprecated private VektorD hitbox = new VektorD(1,2);
    /**
     * Ein Teil der Welt, in der sich der Spieler befindet, wird gecacht und nur jede Sekunde aktualisiert. Das macht das ganze hoffentlich schneller.
     */
    public transient int[][] mapIDCache;
    /**
     * Position der oberen rechten Ecke des mapIDCaches (relativ zur oberen rechten Ecke der gesamten Map)
     */
    public transient VektorI mapIDCachePos;
    /**
     * Daten zu allen Subsandboxes der Sandbox, in der sich der Spieler gerade befindet (Index, Position, Geschwindigkeit)
     */
    public transient SandboxInSandbox[] subData;
    /**
     * MapIDCaches f�r jede Subsandbox
     */
    public transient int[][][] subMapIDCache;
    /**
     * MapIDCache-Positionen f�r jede Subsandbox
     */
    public transient VektorI[] subMapIDCachePos;

    public transient OverlayPanelC opC;
    public PlayerTexture playerTexture;
    public transient OtherPlayerTexturesPanel otherPlayerTexturesPanel;
    public transient DataPanel dataP;
    private PlayerInv inv;
    public transient Hotbar hotbar;
    public int mapDir;
    public PlayerC(Player player, VektorD pos)
    {
        this.player = player;
        setSandbox(player.currentMassIndex, pos);
        makeTexture();
        //muss man hier auch schon synchronisieren?  ka ~ unknown
        // Inventar:
        inv = new PlayerInv();
        mapIDCache=null;
        mapIDCachePos=null;
        subData=null;
        subMapIDCache=null;
        subMapIDCachePos=null;
        //playerTextureCache = null;
        //PlayerTexture
        playerTexture = new PlayerTexture(0);
    }
    
    /**public int getSector(){
        if(getPosToCache(pos).toInt().x<ClientSettings.PLAYERC_MAPIDCACHE_SIZE.x/2 && getPosToCache(pos).toInt().y<ClientSettings.PLAYERC_MAPIDCACHE_SIZE.y/2){return 1;}
        else if(getPosToCache(pos).toInt().x>ClientSettings.PLAYERC_MAPIDCACHE_SIZE.x/2 && getPosToCache(pos).toInt().y<ClientSettings.PLAYERC_MAPIDCACHE_SIZE.y/2){return 2;}
        else if(getPosToCache(pos).toInt().x>ClientSettings.PLAYERC_MAPIDCACHE_SIZE.x/2 && getPosToCache(pos).toInt().y>ClientSettings.PLAYERC_MAPIDCACHE_SIZE.y/2){return 3;}
        else if(getPosToCache(pos).toInt().x<ClientSettings.PLAYERC_MAPIDCACHE_SIZE.x/2 && getPosToCache(pos).toInt().y>ClientSettings.PLAYERC_MAPIDCACHE_SIZE.y/2){return 4;}
        else{return -1;}
    }
    public int getPlayerHitsDiagonal(){
        if(getSector()==1 && getPosToCache(pos).toInt().x== getPosToCache(pos).toInt().y){return 1;}
        else if(getSector()==3 && getPosToCache(pos).toInt().x== getPosToCache(pos).toInt().y){return 3;}
        else if(getSector()==2 && ClientSettings.PLAYERC_MAPIDCACHE_SIZE.x-getPosToCache(pos).toInt().x == getPosToCache(pos).toInt().y){return 2;}
        else if(getSector()==4 && ClientSettings.PLAYERC_MAPIDCACHE_SIZE.x-getPosToCache(pos).toInt().x == getPosToCache(pos).toInt().y){return 4;}
        else{return -1;}
    }
    public int getNewMapDir(){
        if(getPlayerHitsDiagonal()==1 && mapDir==1){return 4;}
        else if(getPlayerHitsDiagonal()==2 && mapDir==1){return 2;}
        else if(getPlayerHitsDiagonal()==2 && mapDir==2){return 1;}
        else if(getPlayerHitsDiagonal()==3 && mapDir==2){return 3;}
        else if(getPlayerHitsDiagonal()==3 && mapDir==3){return 2;}
        else if(getPlayerHitsDiagonal()==4 && mapDir==3){return 4;}
        else if(getPlayerHitsDiagonal()==4 && mapDir==4){return 3;}
        else if(getPlayerHitsDiagonal()==1 && mapDir==4){return 1;}
        else {return -1;}
    }**/
    
    private void makeTexture(){

    }
    
    /**
     * Diese Methode wird von Player.login aufgerufen.
     * Aus irgendeinem Grund hat n�mlich der PlayerC einen Timer, aber der Player selbst nicht.
     */
    public void timerSetup(){
        if (player.onClient()){
            this.timer=new Timer("PlayerC-"+player.getID()+"-Timer");
            timer.schedule(new TimerTask(){
                    public void run(){
                        repaint();
                        try{
                            if(pos.toIntCeil().x!=pos.x){/**keine ganze Zahl*/
                                if(mapIDCache[new VektorD(getPosToCache(player.currentMassIndex,pos).x, getPosToCache(player.currentMassIndex,pos).y).toIntFloor().x-1]
                                    [new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y]==-1){
                                    pos.y=pos.y + 0.05;new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.synchronizePlayerCVariable",null,"pos",VektorD.class,pos);
                                        //System.out.println(pos.toString());
                                }
                            }
                            else{
                                if(mapIDCache[(int)getPosToCache(player.currentMassIndex,pos).x][new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y]==-1
                                && mapIDCache[(int)getPosToCache(player.currentMassIndex,pos).x-1][new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y]==-1){
                                    pos.y=pos.y + 0.05;new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.synchronizePlayerCVariable",null,"pos",VektorD.class,pos);
                                        //System.out.println(pos.toString());
                                }
                            }
                        }
                        catch(ArrayIndexOutOfBoundsException e){}
                        
                        //mapDir=getNewMapDir();
                    }
                },500,ClientSettings.PLAYERC_TIMER_PERIOD);
            timer.schedule(new TimerTask(){
                    public void run(){
                        player.synchronizeWithServer(); //Variablen des Spielers
                        if (player.isOnline()){ //holt sich einen neuen MapIDCache
                            mapIDCache=(int[][]) (new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Sandbox.getMapIDs",int[][].class,player.currentMassIndex,pos.toInt().subtract(ClientSettings.PLAYERC_MAPIDCACHE_SIZE.divide(2)),pos.toInt().add(ClientSettings.PLAYERC_MAPIDCACHE_SIZE.divide(2))).ret);
                            mapIDCachePos=pos.toInt().subtract(ClientSettings.PLAYERC_MAPIDCACHE_SIZE.divide(2));
                            //Object[] ret = (Object[])(new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.getOtherPlayerTextures",Object[].class,pos.toInt().subtract(ClientSettings.PLAYERC_FIELD_OF_VIEW),pos.toInt().add(ClientSettings.PLAYERC_FIELD_OF_VIEW)).ret);
                            //It was so nice, unknown did it twice. Zweimal der gleiche Request erscheint mir irgendwie sinnlos. -LG
                            otherPlayerTexturesPanel.repaint((Object[])(new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.getOtherPlayerTextures",Object[].class,pos.toInt().subtract(ClientSettings.PLAYERC_FIELD_OF_VIEW),pos.toInt().add(ClientSettings.PLAYERC_FIELD_OF_VIEW)).ret));  // hier sehen Sie wie man ein Object in ein Object[] casten kann - Argh!
                            subData=(SandboxInSandbox[])(new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Sandbox.getAllSubsandboxes",SandboxInSandbox[].class,player.currentMassIndex).ret);
                            subMapIDCache=new int[subData.length][ClientSettings.PLAYERC_MAPIDCACHE_SIZE.x][ClientSettings.PLAYERC_MAPIDCACHE_SIZE.y];
                            subMapIDCachePos=new VektorI[subData.length];
                            for (int i=0;i<subData.length;i++){
                                VektorD posRel=pos.subtract(subData[i].offset);
                                subMapIDCache[i]=(int[][]) (new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Sandbox.getMapIDs",int[][].class,subData[i].index,posRel.toInt().subtract(ClientSettings.PLAYERC_MAPIDCACHE_SIZE.divide(2)),posRel.toInt().add(ClientSettings.PLAYERC_MAPIDCACHE_SIZE.divide(2))).ret);
                                subMapIDCachePos[i]=posRel.toInt().subtract(ClientSettings.PLAYERC_MAPIDCACHE_SIZE.divide(2));
                            }
                        }
                    }
                },0,ClientSettings.SYNCHRONIZE_REQUEST_PERIOD);
            timer.schedule(new TimerTask(){
                    public void run(){
                        if (player.getMenu() instanceof ManoeuvreInfo)
                            ((ManoeuvreInfo) player.getMenu()).update();
                    }
                },0,100);
        }
    }

    
    /**
     * Diese Methode wird von Player.makeFrame aufgerufen.
     */
    public void makeFrame(Frame frame){
        this.opC = frame.getOverlayPanelC();
        this.playerTexture.makeFrame(opC,player.getScreenSize(), getBlockWidth());
        this.otherPlayerTexturesPanel = new OtherPlayerTexturesPanel(opC, this, player.getScreenSize());
        // setup des Invs und der Hotbar
        setPlayerInv((PlayerInv)(new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.getPlayerInv",PlayerInv.class).ret));
        this.dataP = new DataPanel(player.getScreenSize(), this, opC);
    }

    public void setupHotbar(){
        if(hotbar != null){ // wenn eine alte Hotbar da ist diese entfernen
            hotbar.setVisible(false);
            opC.remove(hotbar);
        }
        this.hotbar = new Hotbar(opC, inv, player.getScreenSize());   // wird automatisch dem Overlaypanel geadded
        inv.setHotbar(hotbar);
    }

    Object readResolve() throws ObjectStreamException{
        if (player.onClient()){
            this.makeTexture();
            if (player.isOnline())
                this.timerSetup();
        }
        return this;
    }

    /**
     * Setze den Spieler in einer andere Sandbox.
     */
    public void setSandbox(int sandboxIndex, VektorD pos){
        this.pos = pos;
        if (player.isOnline() && player.onClient()){
            new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.synchronizePlayerCVariable",null,"sandboxIndex",Integer.class,sandboxIndex);
            new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.synchronizePlayerCVariable",null,"pos",VektorD.class,pos);
        }
    }
    
    public boolean collideMapIDCache(VektorI v){
        try{
            if(mapIDCache[v.x][v.y]==-1){return false;}
            else{return true;}
        }
        catch(ArrayIndexOutOfBoundsException e){return false;}
    }
    
    /**public boolean collideSubMapIDCache(VektorI v){
        try{
        for(int x=0;subMapIDCache[x][0][0]!=null;x++){
        if(subMapIDCache[x][v.x][v.y]==-1){return false;}
        else{return true;}
        }
        }
        catch(ArrayIndexOutOfBoundsException e){return false;}
    }
    **/
    
    /**
     * Tastatur event
     * @param:
     *  char type: 'p': pressed
     *             'r': released
     *             't': typed (nur Unicode Buchstaben)
     */
    public void keyEvent(KeyEvent e, char type) {
        if (type == 'p'){
                    //System.out.println("KeyEvent in PlayerC: "+e.getKeyChar()+type);
                    //braucht eigentlich noch einen posInsideOfBounds request o.�.
                 
            switch(e.getKeyCode()){
                case Shortcuts.move_up:
                    if(player.getCreative()==true){pos.y=pos.y - 0.5;}
                    else{
                        try{
                            if(pos.toIntCeil().x!=pos.x){/** keine ganze Zahl*/
                                if(mapIDCache[new VektorD(getPosToCache(player.currentMassIndex,pos).x,getPosToCache(player.currentMassIndex,pos).y).toIntFloor().x-1]
                                [new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y-3]==-1 &&
                                mapIDCache[new VektorD(getPosToCache(player.currentMassIndex,pos).x,getPosToCache(player.currentMassIndex,pos).y).toIntFloor().x-1]
                                [new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y-4]==-1 &&
                                mapIDCache[new VektorD(getPosToCache(player.currentMassIndex,pos).x,getPosToCache(player.currentMassIndex,pos).y).toIntFloor().x-1]
                                [new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y]!=-1 ){
                                    pos.y=pos.y - 1.5;
                                }
                            }
                            else{
                                if(mapIDCache[(int)getPosToCache(player.currentMassIndex,pos).x]  [new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y-4]==-1
                                && mapIDCache[(int)getPosToCache(player.currentMassIndex,pos).x-1][new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y-4]==-1
                                && mapIDCache[(int)getPosToCache(player.currentMassIndex,pos).x]  [new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y-3]==-1
                                && mapIDCache[(int)getPosToCache(player.currentMassIndex,pos).x-1][new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y-3]==-1
                                && mapIDCache[(int)getPosToCache(player.currentMassIndex,pos).x]  [new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y]!=-1
                                && mapIDCache[(int)getPosToCache(player.currentMassIndex,pos).x-1][new VektorI((int) Math.round(pos.x),(int) Math.round(getPosToCache(player.currentMassIndex,pos).y)).y]!=-1){
                                    pos.y=pos.y - 1.5;
                                }
                            }
                        }
                        catch(ArrayIndexOutOfBoundsException exc){}
                    }
                    break;
                case Shortcuts.move_down:
                    if(player.getCreative()==true){pos.y=pos.y + 0.5;} 
                    break;
                case Shortcuts.move_left:
                    if(player.getCreative()==true){ pos.x=pos.x - 0.5;  playerTexture.setMode(PlayerTexture.LEFT); synchronizePlayerTexture();}
                    else{
                        try{
                            if(mapIDCache[getPosToCache(player.currentMassIndex,pos.add(new VektorD(-1.5,0))).x][new VektorI(0,(int) Math.round(getPosToCache(player.currentMassIndex,pos).y-2.5)).y]==-1
                                 && mapIDCache[getPosToCache(player.currentMassIndex,pos.add(new VektorD(-1.5,0))).x][new VektorI(0,(int) Math.round(getPosToCache(player.currentMassIndex,pos).y-1.5)).y]==-1)
                            { pos.x=pos.x - 0.5;  playerTexture.setMode(PlayerTexture.LEFT); synchronizePlayerTexture();}
                        }
                        catch(ArrayIndexOutOfBoundsException exc){}
                    }
                    break;
                case Shortcuts.move_right:
                    if(player.getCreative()==true){ pos.x=pos.x + 0.5;  playerTexture.setMode(PlayerTexture.RIGHT); synchronizePlayerTexture();}
                    else{
                        try{
                            if(mapIDCache[getPosToCache(player.currentMassIndex,pos.add(new VektorD(0,0))).x][new VektorI(0,(int) Math.round(getPosToCache(player.currentMassIndex,pos).y-2.5)).y]==-1
                                 && mapIDCache[getPosToCache(player.currentMassIndex,pos.add(new VektorD(0,0))).x][new VektorI(0,(int) Math.round(getPosToCache(player.currentMassIndex,pos).y-1.5)).y]==-1)
                                 { pos.x=pos.x + 0.5; playerTexture.setMode(PlayerTexture.RIGHT); synchronizePlayerTexture();
                            }
                        }
                        catch(ArrayIndexOutOfBoundsException exc){}
                    }
                  break;
                case Shortcuts.open_inventory:
                    openInventory();
                    break;
            }
            if (player.isOnline() && player.onClient())
                new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.synchronizePlayerCVariable",null,"pos",VektorD.class,pos);
            //System.out.println(pos.toString());
        }
    }
    
    public VektorD getFootR(){
         VektorD nPos=pos;
         nPos.add(new VektorD(0.5,0.5));
         return nPos;
    }
    
    public VektorD getFootL(){
         VektorD nPos=pos;
         nPos.add(new VektorD(-0.5,0.5));
         return nPos;
    }
    
    /**
     * Maus Event
     * @param:
     *  char type: 'p': pressed
     *             'r': released
     *             'c': clicked
     *             'd': dragged
     * entered und exited wurde nicht implementiert, weil es daf�r bisher keine Verwendung gab
     */
    public void mouseEvent(MouseEvent e, char type) {
        if (type == 'c'){
            if (!player.isOnline() || !player.onClient())return;
            VektorI clickPos = new VektorI(e);  // Position in Pixeln am Bildschirm
            VektorD sPos=getPosToPlayer(pos,clickPos,blockBreite); //Position in der Sandbox
            int sbIndex=getInteractSandboxIndex(sPos);
            VektorI cPos; // Position im mapIDCache
            if (sbIndex==-1) //Interaktion mit der Hauptsandbox
                cPos=getPosToCache(player.currentMassIndex,sPos);
            else{ //Interaktion mit einer Subsandbox
                cPos=getPosToCache(subData[sbIndex].index,sPos);
            }
            int[][] interactMapCache;
            if (sbIndex==-1)
                interactMapCache=mapIDCache;
            else
                interactMapCache=subMapIDCache[sbIndex];
            try{
                if (e.getButton() == e.BUTTON1){   // linksclick => abbauen
                        /** 
                         * ABBAUEN
                         */
                    Block block=Blocks.get(interactMapCache[cPos.x][cPos.y]);
                    if (block==null) return; // wenn da kein block ist => nichts machen
                    if(block.breakment_prediction){
                        interactMapCache[cPos.x][cPos.y] = -1;
                        
                        if(block.drop_prediction && block.item != null){
                            if(block.drop == -1)getInv().addStack(new Stack(block.item, 1));
                            else{
                                Item dropItem = Items.get(block.drop);
                                if(dropItem != null)getInv().addStack(new Stack(dropItem, 1));
                            }
                        }
                    }
                    // wenn der Block wahrscheinlich zerstört werden kann wird er im cache entfernt. An den Server wird eine Anfrage gestellt, ob das geht, und 
                    // für den Fall, dass es nicht geht, wird der Block bei der nächsten synchronisierung wieder hergestellt
                    if (sbIndex==-1)
                        new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Sandbox.breakBlock",null,player.currentMassIndex,sPos.toInt());
                    else
                        new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Sandbox.breakBlock",null,subData[sbIndex].index,sPos.subtract(subData[sbIndex].offset).toInt());
                }else if (e.getButton() == e.BUTTON3){  // rechtsklick => platzieren oder rechtsklick
                    if(interactMapCache[cPos.x][cPos.y] == -1){
                        /** 
                         * PLATZIEREN
                         */
                      
                        //System.out.println("Tried to place block at "+sPos.toString());
                        Stack hotStack = inv.getHotStack();
                        if(hotStack == null || hotStack.count < 1)return;
                        int blockID;
                        try{ 
                            blockID = ((BlockItem)(hotStack.getItem())).id; 
                        }
                        catch(Exception e1){return;}// => Craftitem
                        if(blockID == -1 || Blocks.get(blockID) == null) return;
                        if(Blocks.get(blockID).placement_prediction){
                            interactMapCache[cPos.x][cPos.y] = blockID;  
                            // wenn der Block wahrscheinlich platziert werden kann wird er im cache gesetzt. An den Server wird eine Anfrage gestellt, ob das geht, und 
                            // für den Fall, dass es nicht geht, wird der Block bei der nächsten synchronisierung wieder entfernt
                            hotStack.setCount(hotStack.getCount() -1);
                            hotbar.updateSlots();
                        }
                        if (sbIndex==-1)
                            new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Sandbox.placeBlock",null,player.getCurrentMassIndex(),sPos.toInt(), blockID);
                        else
                            new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Sandbox.placeBlock",null,subData[sbIndex].index,sPos.subtract(subData[sbIndex].offset).toInt(), blockID);
                    }else{
                        Block block = Blocks.get(interactMapCache[cPos.x][cPos.y]);
                        if(block instanceof SBlock){
                            if (sbIndex==-1)
                                new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Sandbox.rightclickBlock",null,player.getCurrentMassIndex(),sPos.toInt());
                            else
                                new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Sandbox.rightclickBlock",null,subData[sbIndex].index,sPos.subtract(subData[sbIndex].offset).toInt());
                        }
                    }
                }
            }
            catch(ArrayIndexOutOfBoundsException exc){}
        }
    }

    /**
     * Mausrad"Event"
     * @param:
     * irgend ein EventObjekt; Keine Ahnung was das kann
     */
    public void mouseWheelMoved(MouseWheelEvent e){
        try{
            hotbar.scrollDelta(e.getWheelRotation());
            //System.out.println(e.getWheelRotation());
        }catch(Exception ichhattekeinelustzuueberpruefenobhotbarnullist){}
    }

    
    // und die Methoden, die f�r diese Events gebraucht werden
    public void openInventory(){
        //Just for testing purpose ~unknown //Pourquoi parles-tu en anglais? ~LG // ???????????????????? ~unknown
        if (inv == null)return;

        new InventoryMenu(player, this.inv);
    }

    
    public PlayerInv getInv(){ //von LG zum Testen, auch wenn ich eigentlich keine Ahnung vom inv habe
        return inv;
    }

    /**
     * setzt das Inv ohne die Hotbar upzudaten (nur f�r den Server)
     */
    public void setInv(PlayerInv inv){
        this.inv = inv;
    }
    
    /**
     * setzt das Inv und updated die Hotbar (nur f�r den Client)
     */
    public void setPlayerInv(PlayerInv inv){
        this.inv = inv;
        setupHotbar();
        hotbar = inv.hotbar;
    }

    public int getBlockWidth(){
        return blockBreite;  // Naming on point !!
    }

    public void setPlayerTexture(int id){
        playerTexture.setTexture(id);
    }

    public PlayerTexture getPlayerTexture(){
        return playerTexture;
    }

    /**
     * geht nicht oder doch?
     */
    public void synchronizePlayerTexture(){
        new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.synchronizePlayerCVariable",null,"playerTexture",PlayerTexture.class,playerTexture);
    }

    
    /***********************************************************************************************************************************************************
    /*********3. Methoden f�r Subsandboxes und Raketenstart*****************************************************************************************************
    /***********************************************************************************************************************************************************/

    /**
     * Gibt den Index der ersten Sandbox (im subMapIDCache-Array, also nicht im Space.masses-Array, oder -1 => Hauptsandbox) zur�ck, mit der der Spieler interagieren kann.
     * Bevorzugt wird immer eine Subsandbox.
     * Die Subsandboxen sollten sich also nicht �berschneiden (au�er mit der Hauptsandbox), sonst gibt es hier Probleme.
     * @param:
     * sPos: Position im allgemeinen Map Array (l�sst sich mit getPosToPlayer() aus einer Klick-Position berechnen)
     */
    public int getInteractSandboxIndex(VektorD sPos){
        for (int i=0;i<subData.length;i++){
            VektorD posRel=sPos.subtract(subData[i].offset);
            if (posRel.x>=0 && posRel.y>=0 && posRel.x<subData[i].size.x && posRel.y<subData[i].size.y)
                return i;
        }
        return -1;
    }

    /***********************************************************************************************************************************************************
    /*********4. Methoden f�r Ansicht und Grafikausgabe*********************************************************************************************************
    /***********************************************************************************************************************************************************/


     /**
     * Gibt die obere linken Ecke (int Bl�cken) der aktuellen Spieleransicht an
     */
    public VektorD getUpperLeftCorner(){
        return getUpperLeftCorner(pos);
    }

    /**
     * Gibt die obere linken Ecke (in Bl�cken) der Spieleransicht an
     * @param: pos: Position des Spielers relativ zur oberen linken Ecke der Sandbox
     */
    public VektorD getUpperLeftCorner(VektorD pos){
        // 2.6.2019 AK: .subtract(new VektorD(0.5,0.5)) ist richtig. (Stichwort obere linke ecke des Blocks & Rundung)
        return pos.add(ClientSettings.PLAYERC_FIELD_OF_VIEW.toDouble().multiply(-0.5).subtract(new VektorD(0.5,0.5)) );
    }

    /**
     * gibt die Position eines Blocks (erstmal noch als VektorD, falls noch ein VektorD-Subsandbox-Offset dazuaddiert werden muss)
     * 
     * @param: 
     * posRel: Position des Spielers relativ zu der Sandbox, mit der er interagiert
     * bPos: Position des Klicks
     * blockBreite: Breite eines Blocks in Pixeln
     */
    public VektorD getPosToPlayer(VektorD posRel, VektorI bPos, int blockBreite){
        //System.out.println(bPos.toString()+" "+bPos.toDouble().divide(blockBreite).toString());
        return (getUpperLeftCorner(posRel).add(bPos.toDouble().divide(blockBreite)));
    }

    /**
     * gibt die Position eines Blocks im Cache-Array an
     * 
     * @param: 
     * sandboxIndex: Index der Sandbox, mit der der Spieler interagiert, im Space.masses-Array, im Normalfall player.currentMassIndex
     * sPos: Position im allgemeinen Map Array (l�sst sich mit getPosToPlayer() berechnen)
     */
    public VektorI getPosToCache(int sandboxIndex, VektorD sPos){
        if (sandboxIndex==player.currentMassIndex)
            return sPos.subtract(mapIDCachePos.toDouble()).toInt();
        else{
            for (int i=0;i<subData.length;i++){
                if (subData[i].index==sandboxIndex){
                    return sPos.subtract(subMapIDCachePos[i].toDouble()).subtract(subData[i].offset).toInt();
                }
            }
        }
        return null;
    }

    /**
     * Zeichnen
     */
    public void paint(Graphics g, VektorI screenSize){
        //Request r = new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.getOtherPlayerTextures",Object[].class);
        //playerTextureCache = (Object[])(new Request(player.getID(),player.getRequestOut(),player.getRequestIn(),"Main.getOtherPlayerTextures",Object[].class).ret);
        if (player.isOnline() && player.onClient()){
            if (mapIDCache!=null && mapIDCachePos!=null){
                VektorD fieldOfView=ClientSettings.PLAYERC_FIELD_OF_VIEW.toDouble();
                int minX=(int) Math.floor(pos.x-fieldOfView.x/2)-1; //keine Ahnung, warum die -1 und +1
                int maxX=(int) Math.ceil(pos.x+fieldOfView.x/2)+1;
                int minY=(int) Math.floor(pos.y-fieldOfView.y/2)-1;
                int maxY=(int) Math.ceil(pos.y+fieldOfView.y/2)+1;
                ColorModel cm=ColorModel.getRGBdefault();
                BufferedImage image=new BufferedImage(cm,cm.createCompatibleWritableRaster((maxX-minX)*blockBreite,(maxY-minY)*blockBreite),false,new Hashtable<String,Object>());
                //alle hier erstellten BufferedImages haben den TYPE_INT_ARGB
                int drawX=(int) ((minX-pos.x+((fieldOfView.x)/2))*blockBreite); //es wird nur ein Teil des Bilds gezeichnet (ein Rechteck von (drawX|drawY) mit Breite width und H�he height
                int drawY=(int) ((minY-pos.y+((fieldOfView.y)/2))*blockBreite);
                int width=(int) (fieldOfView.x*blockBreite);
                int height=(int) (fieldOfView.y*blockBreite);

                int[] oldImageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

                Hashtable<Integer,BufferedImage> blockImages=new Hashtable<Integer,BufferedImage>(); //Skalierung
                for (int x = 0; x<=ClientSettings.PLAYERC_MAPIDCACHE_SIZE.x; x++){
                    for (int y = 0; y<=ClientSettings.PLAYERC_MAPIDCACHE_SIZE.y; y++){
                        try{
                            BufferedImage img=Blocks.getTexture(mapIDCache[x][y]);
                            if (img!=null){
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
                                blockImages.put(mapIDCache[x][y],img2);
                            }
                            else{
                                Hashtable<String,Object> properties=new Hashtable<String,Object>();
                                blockImages.put(mapIDCache[x][y],new BufferedImage(cm,cm.createCompatibleWritableRaster(blockBreite,blockBreite),false,properties));
                            }
                        }
                        catch(ArrayIndexOutOfBoundsException e){}
                    }
                }
                for (int x = minX ; x<=maxX; x++){
                    for (int y = minY; y<=maxY; y++){
                        try{
                            int id = mapIDCache[x-mapIDCachePos.x][y-mapIDCachePos.y];
                            if(id != -1){ //Luft
                                BufferedImage img=blockImages.get(id);
                                if (img!=null){
                                    int[] imgData=((DataBufferInt) img.getRaster().getDataBuffer()).getData();
                                    for (int i=0;i<blockBreite;i++){
                                        int index = ((y-minY)*blockBreite + i)*(maxX-minX)*blockBreite + (x-minX)*blockBreite;
                                        //((y-mapIDCachePos.y)*blockBreite + i)*ClientSettings.PLAYERC_FIELD_OF_VIEW.x*blockBreite + (x-mapIDCachePos.x)*blockBreite;
                                        System.arraycopy(imgData,i*blockBreite,oldImageData,index,blockBreite);
                                    }
                                }
                            }
                        }
                        catch(ArrayIndexOutOfBoundsException e){}
                    }
                }
                
                Graphics2D g2=image.createGraphics();
                //Zeichnen von Subsandboxen, recht ähnlich zu dem Zeichnen der Sandbox oberhalb
                if (subData!=null && subMapIDCache!=null && subMapIDCachePos!=null){
                    for (int i=0;i<subData.length;i++){
                        SandboxInSandbox sd=subData[i];
                        int[][] smic=subMapIDCache[i];
                        VektorI smicp=subMapIDCachePos[i];
                        if (sd!=null && smic!=null && smicp!=null){
                            VektorD posRel=pos.subtract(sd.offset);

                            int minXSub=(int) Math.floor(posRel.x-fieldOfView.x/2)-1;
                            int maxXSub=(int) Math.ceil(posRel.x+fieldOfView.x/2)+1;
                            int minYSub=(int) Math.floor(posRel.y-fieldOfView.y/2)-1;
                            int maxYSub=(int) Math.ceil(posRel.y+fieldOfView.y/2)+1;
                            ColorModel subCm=ColorModel.getRGBdefault();
                            BufferedImage subImage=new BufferedImage(subCm,subCm.createCompatibleWritableRaster((maxXSub-minXSub)*blockBreite,(maxYSub-minYSub)*blockBreite),false,new Hashtable<String,Object>());
                            int[] oldSubImageData = ((DataBufferInt) subImage.getRaster().getDataBuffer()).getData();

                            Hashtable<Integer,BufferedImage> subBlockImages=new Hashtable<Integer,BufferedImage>();
                            for (int x = 0; x<=ClientSettings.PLAYERC_MAPIDCACHE_SIZE.x; x++){
                                for (int y = 0; y<=ClientSettings.PLAYERC_MAPIDCACHE_SIZE.y; y++){
                                    try{
                                        BufferedImage img=Blocks.getTexture(smic[x][y]);
                                        if (img!=null){
                                            Hashtable<String,Object> properties=new Hashtable<String,Object>();
                                            String[] prns=image.getPropertyNames();
                                            if (prns!=null){
                                                for (int j=0;j<prns.length;j++){
                                                    properties.put(prns[j],image.getProperty(prns[j]));
                                                }
                                            }
                                            BufferedImage img2=new BufferedImage(cm,cm.createCompatibleWritableRaster(blockBreite,blockBreite),false,properties);
                                            Graphics gr=img2.getGraphics();
                                            gr.drawImage(img,0,0,blockBreite,blockBreite,null);
                                            subBlockImages.put(smic[x][y],img2);
                                        }
                                        else{
                                            Hashtable<String,Object> properties=new Hashtable<String,Object>();
                                            subBlockImages.put(smic[x][y],new BufferedImage(cm,cm.createCompatibleWritableRaster(blockBreite,blockBreite),false,properties));
                                        }
                                    }
                                    catch(ArrayIndexOutOfBoundsException e){}
                                }
                            }
                            for (int x = minXSub ; x<=maxXSub; x++){
                                for (int y = minYSub; y<=maxYSub; y++){
                                    try{
                                        int id = smic[x-smicp.x][y-smicp.y];
                                        if(id != -1){
                                            BufferedImage img=subBlockImages.get(id);
                                            if (img!=null){
                                                int[] imgData=((DataBufferInt) img.getRaster().getDataBuffer()).getData();
                                                for (int j=0;j<blockBreite;j++){
                                                    int index = ((y-minYSub)*blockBreite + j)*(maxXSub-minXSub)*blockBreite + (x-minXSub)*blockBreite;
                                                    System.arraycopy(imgData,j*blockBreite,oldSubImageData,index,blockBreite);
                                                }
                                            }
                                        }
                                    }
                                    catch(ArrayIndexOutOfBoundsException e){}
                                }
                            }
                            
                            //Zeichnen dieses Subsandbox-Bilds auf das allgemeine Bild
                            int drawXSub=(int) ((minXSub-posRel.x+((fieldOfView.x)/2))*blockBreite);
                            int drawYSub=(int) ((minYSub-posRel.y+((fieldOfView.y)/2))*blockBreite);
                            int widthSub=(int) (fieldOfView.x*blockBreite);
                            int heightSub=(int) (fieldOfView.y*blockBreite);
                            subImage=subImage.getSubimage(-drawXSub,-drawYSub,widthSub,heightSub);
                            g2.drawImage(subImage,-drawX,-drawY,new Color(255,255,255,0),null); //keine Ahnung warum -drawX, -drawY, aber es geht
                            
                            //roter Rahmen um die Subsandbox, damit man ihre Grenzen sehen kann
                            g2.setColor(Color.RED);
                            g2.drawRect((int) ((sd.offset.x-minX)*blockBreite),(int) ((sd.offset.y-minY)*blockBreite),sd.size.x*blockBreite,sd.size.y*blockBreite);
                            g2.setColor(Color.BLACK);
                        }
                    }
                }

                g.setColor(new Color(0,0,0,1));
                Color background = new Color(180,230,255,255);// hier kann der Hintergrund verändert werden
                //System.out.println(drawX+" "+drawY+" "+width+" "+height+" "+image.getWidth()+" "+image.getHeight());
                image=image.getSubimage(-drawX,-drawY,width,height);
                g.drawImage(image,0,0,background,null);
            }
            //DataPanel wird upgedated v0.3.13_MH
            dataP.update();
        }
    }
    
    public void repaint(){
        player.repaint();
    }

    public void synchronizeWithServer(){
        player.synchronizeWithServer();
    }

}