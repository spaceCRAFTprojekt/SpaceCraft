package server;
import util.geom.*;
import client.Orbit;
import client.AbstractMass;
import java.util.Timer;
import java.util.ArrayList;
import java.io.Serializable;
public abstract class Mass extends AbstractMass implements Serializable
{
    public static final long serialVersionUID=0L;
    public Main main;
    protected transient Timer spaceTimer;

    public Mass(Main main, double m, VektorD pos, VektorD vel, Timer spaceTimer){
        super(m,pos,vel);
        this.main=main;
        this.spaceTimer=spaceTimer;
        spaceTimerSetup();
    }
    
    protected abstract void spaceTimerSetup();
    //Nur hier können neue TimerTasks hinzugefügt werden.
    
    /**
     * Gibt die Sandbox der Masse zurück
     */
    public abstract Sandbox getSandbox();
    
    public void setSpaceTimer(Timer spaceTimer){
        this.spaceTimer=spaceTimer;
        spaceTimerSetup();
        if (this.getSandbox()!=null){
            this.getSandbox().setSpaceTimer(spaceTimer);
        }
    }
    
    public static Mass sum(Mass m1, Mass m2){
        double mNew=m1.getMass()+m2.getMass();
        return new PlanetS(m1.main,mNew, m1.getPos().multiply(m1.getMass()).divide(mNew).add(m2.getPos().multiply(m2.getMass()).divide(mNew)),null,"",0,0,0,null);
    }
}