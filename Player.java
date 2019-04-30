import java.util.ArrayList;
import geom.*;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
/**
 * Ein Spieler
 * Kann entweder in der Craft oder in der Space Ansicht sein
 */
public class Player implements Serializable
{
    private transient Main main; //Referenz auf den Server, wird nicht gespeichert (logischerweise)
    private String name;
    private transient Space space; // mein privater Weltraum xD
    private boolean inCraft = true;
    private transient Frame frame;
    private PlayerS playerS;
    private PlayerC playerC;
    private boolean notActive = false; // wenn zum Beispiel ein Inventar oder das escape Menu offen ist
    
    /**
     * Erstellt neuen Spieler in einem Weltraum
     */
    public Player(String name, Space space)
    {
        this.space = space;
        this.name = name;
        //der Spawnpunkt muss nochmal überdacht werden
        this.playerS=new PlayerS(this,new VektorD(0,0));
        makeFrame();
        this.playerC=new PlayerC(this, space.getSpawnPlanet(), new VektorD(100,50),frame);
    }
    
    private void makeFrame(){ //Frame-Vorbereitung (Buttons, Listener etc.) nur hier
        this.frame = new Frame(name,new VektorI(928,608),this);
        Listener l=new Listener(this);
        this.frame.addKeyListener(l);
        this.frame.addMouseListener(l);
        this.frame.addWindowListener(l);
    }
    
    public Space getSpace(){
        return space;
    }
    
    public void setSpace(Space s){ //nur für nach der Deserialisierung, damit alle Spieler den selben Space erhalten
        this.space=s;
    }
    
    public void setMain(Main m){
        if (m.getSpace()!=this.space){
            throw new IllegalArgumentException();
        }
        this.main=m;
    }
    
    public Object readResolve() throws ObjectStreamException{
        this.makeFrame();
        return this;
    }

    /**
     * gibt den Namen des Spielers zurück
     */
    public String getName(){
        return name;
    }
    
    /**
     * Wechselt die Ansicht zur Space Ansicht
     */
    public void toSpace()
    {
        if (!inCraft)return; // wenn der Spieler schon in der Space Ansicht ist, dann wird nichts getan
        inCraft = false;
        repaint();
    }
    
    /**
     * Wechselt die Ansicht zur Space Ansicht
    */
    public void toCraft()
    {
        if (inCraft)return; // wenn der Spieler schon in der Space Ansicht ist, dann wird nichts getan
        inCraft = true;
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
     * Setzt die notActive Variable wieder auf false
     * z.B. wenn ein Menu geschlossen wird
     */
    public void activate()
    {
        notActive = false;
    }
    
    /**
     * Setzt die notActive Variable auf true
     * z.B. wenn ein Menu geöffnet wird
     */
    public void deactivate()
    {
        notActive = true;
    }
    
    /**
     * Tastatur Event
     * @param:
     *  char type: 'p': pressed
     *             'r': released
     *             't': typed (nur Unicode Buchstaben)
     */
    public void keyEvent(KeyEvent e, char type) {
        if(notActive == true)return;  // Wenn ein anderes Menu offen ist passiert nichts!
        switch (e.getKeyCode()){
            case Shortcuts.open_escape_menu: 
                if (type != 'p') break;
                new EscapeMenu(this);
                deactivate();
                break;
            case Shortcuts.change_space_craft:
                if (type != 'p') break;
                changePlayer();
                repaint();
                break;
            default:
                if (inCraft)playerC.keyEvent(e,type);
                //else playerS.keyEvent(e,type);
        }
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
        if (inCraft)playerC.mouseEvent(e,type);
        //else playerS.mouseEvent(e,type);
    }
    
    public void windowEvent(WindowEvent e, char type){
        if (type=='c'){ //Schließen des Fensters
            if (this.main!=null){
                main.removePlayer(this.name);
            }
            else{
                System.exit(0); //Fehler
            }
        }
    }
    
    /**
     * Spiel (für diesen Spieler) beenden!
     */
    public void exit(){
        main.removePlayer(this);
        frame.dispose();
    }
    
    /**
     * Grafik ausgeben
     */
    public void paint(Graphics g, VektorI screenSize){
        if (g!=null){
            if (inCraft)playerC.paint(g, screenSize);
            else playerS.paint(g, screenSize);
        }
    }
    public void repaint(){
        if(frame!=null)frame.repaint();
    }
}