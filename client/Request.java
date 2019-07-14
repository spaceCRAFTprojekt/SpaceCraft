package client;
import java.util.ArrayList;
import java.io.Serializable;
import java.net.Socket;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
/**
 * Ein Player (Client) sendet nur Requests an den Server => im client-package keine Referenzen auf
 * das Server-package!
 * Wenn ein neuer Socket erstellt wird, muss er zuallererst einen boolean senden, damit der Server wei�, was es f�r ein Client ist.
 * True steht f�r Request-Client, false f�r Task-Client.
 * Liste aller Request-Funktionen - sollte aktualisiert werden, wenn neue dazukommen (wahrscheinlich trotzdem unvollst�ndig):
     * Main.exit()
     * Main.exitIfNoPlayers()
     * Main.newPlayer(String name,String password)
     * Main.getPlayer(String name)
     * Main.login(String password)
     * Main.logout(PlayerInv inv)
     * Main.getPlayerInv()
     * Main.returnFromMenu(String menuName, Object[] menuParams)
     * Main.synchronizePlayerVariable(String varname, Class class, Object value)
     * Main.synchronizePlayerSVariable(String varname, Class class, Object value)
     * Main.synchronizePlayerCVariable(String varname, Class class, Object value) (diese drei setzen Werte von Variablen der Kopie des Players am Server auf den angegebenen Wert)
     * Main.retrievePlayer() (id wird ja schon mitgegeben) (zur Synchronisierung)
     * Main.writeIntoChat(String message)
     * Main.getChatContent(int numLines)
     * Main.getOtherPlayerTextures(int PlayerID, VektorI upperLeftCorner, VektorI bottomRightCorner)
     * Space.getFocussedMassIndex(VektorD pos, VektorD posToNull, VektorI screenSize, Double scale)
     * Space.getMassPos(Integer index)
     * Space.getMassNumber()
     * Space.getAllMasses()
     * Space.getAllMassesInaccurate()
     * Space.setTime(long time)
     * Space.getSupersandboxIndex(int subsandboxIndex)
     * Sandbox.breakBlock(Integer sandboxIndex, VektorI sPos) v0.3.1_AK
     * Sandbox.placeBlock(Integer sandboxIndex, VektorI sPos, Integer blockID) v0.3.1_AK
     * Sandbox.rightclickBlock(Integer sandboxIndex, VektorI sPos)
     * Sandbox.getMapIDs(Integer sandboxIndex, VektorI upperLeftCorner, VektorI bottomRightCorner)
     * Sandbox.getAllSubsandboxes(Integer sandboxIndex)
     * Sandbox.createShip(VektorI pos, int ownerID)
     * Sandbox.startShip(int shipIndex, int ownerID)
     * 
     * (die hier angegebenen Argumente sind nur die aus params, alle Funktionen haben als �bergabewert auch noch die ID des players)
     * Bei Sandbox.*-Methoden ist der erste Parameter aus params der currentMassIndex des Players.
 */
public class Request implements Serializable{
    public static final long serialVersionUID=0L;
    public int playerID;
    public String todo;
    public Object[] params;
    public volatile Object ret; //muss wahrscheinlich nicht volatile sein
    public Class retClass;
    /**
     * Der Player mit der gegebenen PlayerID stellt den Request, dass der Server todo tut, er �bergibt die Parameter params.
     * Es sollte jedes Mal �berpr�ft werden, ob der Player �berhaupt auf dem Client und online ist.
     * Konvention: todo=Klassenname+"."+Methodenname
     * ret=irgendein R�ckgabewert (der formale R�ckgabewert ist void)
     * (    <IrgendeineKlasse> obj = <IrgendeinKonstruktorOderNull>;
     *      Request req = new Request(p, todo, <IrgendeineKlasse>.class, params);
     *      obj = (<CastAufIrgendeineKlasse>) req.ret;
     *      req = null; //nur Code-Stil, da der Request jetzt nutzlos geworden ist
     *      oder so �hnlich
     * )
     * Wenn retClass gleich null ist, dann ist es ein Request, auf dessen Antwort nicht gewartet wird
     * (dieser hat dann auch keinen R�ckgabewert) (z.B. Main.synchronizePlayerVariable).
     * Da alle Request-Methoden, auf die gewartet wird, also ein R�ckgabeobjekt haben m�ssen, ist hiermit Konvention,
     * dass es bei eigentlichen void-Methoden ein Boolean (Objekt) ist (wird true, wenn der Request erfolgreich war).
     * �bergabewerte der Methode im Server: playerID, params, wobei alle primitiven Parameter zu Objekten konvertiert werden (Arrays sind keine primitiven Objekte.).
     * �ber den Nutzen von retClass lässt sich streiten.
     */
    public Request(int playerID, ObjectOutputStream socketOut, ObjectInputStream socketIn, String todo, Class retClass, Object... params){
        if (ClientSettings.PRINT_COMMUNICATION)
            System.out.println("[Client]: new "+this);
        if (socketOut!=null && socketIn!=null){
            this.playerID=playerID;
            this.todo=todo;
            this.retClass=retClass;
            this.params=params;
            if (retClass!=null){
                try{
                    synchronized(socketIn){
                        synchronized(socketOut){
                            socketOut.reset();
                            socketOut.writeObject(this);
                            socketOut.flush();
                        }
                        ret=socketIn.readObject();
                    }
                }
                catch(Exception e){
                    System.out.println("[Client]: Exception when creating request ("+this+"): "+e);
                }
            }
            else{ //wartet nicht
                try{
                    synchronized(socketOut){
                        socketOut.reset();
                        socketOut.writeObject(this);
                        socketOut.flush();
                    }
                }
                catch(Exception e){
                    System.out.println("[Client]: Exception when creating request ("+this+"): "+e);
                }
            }
        }
    }
    
    public String toString(){
        return "Request: playerID = "+playerID+", todo = "+todo+", retClass = "+retClass+", ret = "+ret;
    }
}