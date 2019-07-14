package server;
import util.geom.*;
import java.util.Timer;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import client.Manoeuvre;
/**
 * Die Space Variante eines Planeten
 */
public class PlanetS extends Mass implements Serializable
{
    public static final long serialVersionUID=0L;
    double gravity;
    int radius; // in m
    int lightSource;
    String name;
    PlanetC planetC;
    /**
     * Erstellt einen neuen Planeten
     * @Params:
     * - Masse
     * - Position
     * - Geschwindigkeit
     * - Name
     * - Radius (m)
     * - Planetanziehungskraft
     * - Licht
     */
    public PlanetS(Main main, double m, VektorD pos, VektorD vel, String name, int radius, double gravity, int lightSource, Timer spaceTimer)
    {
        super(main,m,pos,vel,spaceTimer);
        this.gravity = gravity;
        this.name = name;
        this.lightSource = lightSource;
        this.radius = radius;
        planetC = new PlanetC(main,new VektorI((radius*2)+100, (radius*2)+100), this, spaceTimer);
    }
    
    @Override 
    public Sandbox getSandbox()
    {
        return planetC;
    }
    
    public int getRadius(){
        return radius;
    }
    
    @Override
    public void setSpaceTimer(Timer t){
        super.setSpaceTimer(t);
    }
    
    protected void spaceTimerSetup(){}
    
    /**
     * gibt die Planetenanziehungskraft/Beschleunigung zurück
     */
    public double getGravity()
    {
        return gravity;
    }
    
    /**
     * (n�tig, da es client.AbstractMass erweitert)
     */
    public boolean isControllable(int playerID){
        return false;
    }
    
    /**
     * (n�tig, da es client.AbstractMass erweitert)
     */
    public ArrayList<Manoeuvre> getManoeuvres(){
        return new ArrayList<Manoeuvre>();
    }
    
    /**
     * (n�tig, da es client.AbstractMass erweitert)
     */
    public void setManoeuvres(ArrayList<Manoeuvre> manos){}
    
    /**
     * (n�tig, da es client.AbstractMass erweitert)
     */
    public double getOutvel(){
        return 0;
    }
    
    /**
     * (n�tig, da es client.AbstractMass erweitert)
     */
    public void setOutvel(double ov){}
    
    /**
     * (n�tig, da es client.AbstractMass erweitert)
     */
    public double getRestMass(){
        return -1;
    }
}