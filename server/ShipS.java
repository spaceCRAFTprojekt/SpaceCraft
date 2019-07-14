package server;
import util.geom.*;
import client.Orbit;
import client.Manoeuvre;
import java.util.ArrayList;
import java.util.Timer;
import java.io.Serializable;
import java.io.ObjectStreamException;
/**
 * Die Space Variante eines Raumschiffs
 */
public class ShipS extends Mass implements Serializable
{
    public static final long serialVersionUID=0L;
    /**
     * Liste aller Man�ver, die das Schiff macht.
     */
    public ArrayList<Manoeuvre> manoeuvres = new ArrayList<Manoeuvre>();
    /**
     * wenn diese Liste leer ist, dann ist das Schiff �ffentlich
     */
    public ArrayList<Integer> ownerIDs=new ArrayList<Integer>();
    /**
     * siehe client.AbstractMass.get/setOutvel
     */
    private double outvel;
    /**
     * siehe client.AbstractMass.getRestMass
     */
    private double restMass;
    public ShipC shipC;
    
    /**
     * Erstellt ein neues Raumschiff
     * @Params:
     * - Masse
     * - Position
     * - Geschwindigkeit
     */
    public ShipS(Main main, double m, VektorD pos, VektorD vel, double outvel, double restMass, Timer spaceTimer)
    {
        super(main,m,pos,vel,spaceTimer);
        shipC=new ShipC(main,new VektorI(20,40),this,spaceTimer);
        this.outvel=outvel;
        this.restMass=restMass;
    }
    
    @Override
    public void setSpaceTimer(Timer t){
        super.setSpaceTimer(t);
    }
    
    protected void spaceTimerSetup(){}
    
    public Sandbox getSandbox(){
        return shipC;
    }
    
    public void setOwner(int playerID){
        if (ownerIDs.indexOf(playerID)==-1)
            ownerIDs.add(playerID);
    }

    public void removeOwner(int playerID){
        if (ownerIDs.indexOf(playerID)!=-1)
            ownerIDs.remove(ownerIDs.indexOf(playerID));
    }

    public boolean isOwner(int playerID){
        return ownerIDs.indexOf(playerID)!=-1 || ownerIDs.size()==0;
    }
    
    /**
     * ben�tigt, da es client.AbstractMass erweitert
     */
    public boolean isControllable(int playerID){
        return isOwner(playerID);
    }
    
    public ArrayList<Manoeuvre> getManoeuvres(){
        return manoeuvres;
    }
    
    public int getRadius(){
        return 2;
    }
    
    public void setManoeuvres(ArrayList<Manoeuvre> manos){
        //(einfache Checks, ob der Client irgendwelchen Unsinn sendet)
        double dMassGes=0; //gesamte verlorene Masse, sollte nat�rlich nicht gr��er als die Restmasse des Schiffs sein
        for (int i=0;i<manos.size();i++){
            if (manos.get(i).outvel>getOutvel()){ //Aussto�geschwindigkeit zu gro�
                return;
            }
            if (manos.get(i).t0<main.getSpace().inGameTime){ //Man�ver schon verstrichen
                return;
            }
            dMassGes=dMassGes+manos.get(i).dMass;
        }
        if (getMass()-dMassGes<restMass){ //Massenaussto� zu gro�
            return;
        }
        manoeuvres=manos;
    }
    
    /**
     * Siehe client.ClientMass.getOutvel()
     */
    public double getOutvel(){
        return outvel;
    }
    public void setOutvel(double ov){
        outvel=ov;
    }
    
    /**
     * Siehe client.ClientMass.getRestMass()
     */
    public double getRestMass(){
        return restMass;
    }
}