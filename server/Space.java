package server;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import util.geom.*;
import java.io.Serializable;
import java.io.ObjectStreamException;
/**
 * Ein Weltall
 */
public class Space implements Serializable
{
    public static final long serialVersionUID=0L;
    ArrayList<Mass>masses = new ArrayList<Mass>(); // hier sind alle Massen (Planeten oder Schiffe) verzeichnet
    transient Timer timer;
    long time; //Alle Zeiten in s
    long inGameTime;
    long inGameDTime; //eine Sekunde in echt => inGameDTime Sekunden im Spiel
    /**
     * Erstellt eine neues Sonnensystem (am Anfang: Sonne, Erde und Mond)
     *
     */
    public Space(long inGameDTime)
    {
        timer=new Timer();
        PlanetS erde=new PlanetS(1000000000L,new VektorD(0,0),new VektorD(10,0),"Erde",250,10,0,timer);
        masses.add(erde);
        PlanetS mond=new PlanetS(200000L,new VektorD(-5000,0),new VektorD(10,5),"Mond",10,10,0,timer);
        masses.add(mond);
        ShipS schiff=new ShipS(20L,new VektorD(500,0),new VektorD(0,10),timer);
        masses.add(schiff);
        erde.getSandbox().addSandbox(mond.getSandbox(),new VektorD(0,0));
        time=0;
        inGameTime=0;
        this.inGameDTime=inGameDTime;
        calcOrbits(inGameDTime*20+1); //so lange Zeit, damit man es gut sieht
        timerSetup();
    }
    
    public Object readResolve() throws ObjectStreamException{
        this.timer=new Timer();
        timerSetup();
        calcOrbits(inGameDTime*20+1);
        return this;
    }
    
    public void timerSetup(){
        timer.schedule(new TimerTask(){
            public void run(){
                time=time+Settings.SPACE_TIMER_PERIOD/1000;
                inGameTime=inGameTime+inGameDTime;
                for (int i=0;i<masses.size();i++){
                    Orbit o=masses.get(i).getOrbit();
                    if (o.getPos(inGameTime)!=null){
                        masses.get(i).setPos(o.getPos(inGameTime));
                    }
                    if (o.getVel(inGameTime)!=null){
                        masses.get(i).setVel(o.getVel(inGameTime));
                    }
                }
                calcOrbits(inGameDTime*20+1); //so lange Zeit, damit man es gut sieht. Verwendet wird davon nur der geringste Teil.
            }
        },Settings.SPACE_TIMER_PERIOD,Settings.SPACE_TIMER_PERIOD);
        for (int i=0;i<masses.size();i++){
            masses.get(i).setSpaceTimer(timer);
        }
    }

    /**
     * Gibt die Masse in der der Spieler spawnt zurück
     */
    public Mass getSpawnMass()
    {
        return masses.get(0);
    }
    
    /**
     * Gibt, falls vorhanden, die Masse mit index i zurück
     */
    public Mass getMass(int i)
    {
        try{
            return masses.get(i);
        }catch(Exception e){
            System.out.println("at Space.getMass(" + i + "): Schwarzes Loch? Jemand hat versucht einen nicht mehr vorhanden Planeten zu finden");
            return null;
        }
    }
    
    /**
     * Die Parameter kommen von einem PlayerS (zur Fokussierung auf einen Planeten).
     * Es ist der Index des Planeten an pos.
     * Einige der Parameter sind Standard mit Requests (=unnötig)
     */
    public Integer getFocussedMassIndex(Integer playerID, VektorI posClick, VektorD posToNull, VektorI screenSize, Double scale){
        posClick.y=-posClick.y+screenSize.y; //invertiertes Koordinatensystem
        posClick=posClick.subtract(screenSize.divide(2));
        System.out.println(posClick);
        posClick=posClick.divide(scale);
        VektorI posClickToNull=posClick.add(posToNull.toInt());
        Integer ret=new Integer(-1);
        for (int i=0;i<masses.size();i++){
            if (masses.get(i)!=null){
                VektorD posPlanet=masses.get(i).getPos();
                double r=2;
                if (masses.get(i) instanceof PlanetS){
                    r=((PlanetS) masses.get(i)).getRadius()*scale;
                }
                double distance=posPlanet.subtract(posClickToNull.toDouble()).getLength()*scale;
                if (distance < r+20){
                    ret=new Integer(i);
                    return ret;
                }
            }
        }
        ret=new Integer(-1);
        return ret;
    }
    
    /**
     * Request-Funktion (Player-ID standardmäßig als Übergabeparameter)
     */
    public VektorD getMassPos(Integer playerID, Integer index){
        VektorD ret=new VektorD(Double.NaN,Double.NaN);
        if (masses.get(index)==null){
            
        }
        else{
            ret=masses.get(index).getPos();
        }
        return ret;
    }
    
    /**
     * Request-Funktion
     */
    public ArrayList<VektorD> getAllPos(Integer playerID){
        ArrayList<VektorD> ret=new ArrayList<VektorD>();
        for (int i=0;i<masses.size();i++){
            ret.add(masses.get(i).getPos());
        }
        return ret;
    }
    
    /**
     * Request-Funktion
     */
    public ArrayList<Integer> getAllRadii(Integer playerID){
        ArrayList<Integer> ret=new ArrayList<Integer>();
        for (int i=0;i<masses.size();i++){
            if (masses.get(i) instanceof PlanetS){
                ret.add(((PlanetS) masses.get(i)).getRadius());
            }
            else{
                ret.add(2);
            }
        }
        return ret;
    }
    
    /**
     * Request-Funktion
     */
    public ArrayList<ArrayList<VektorD>> getAllOrbits(Integer playerID){
        ArrayList<ArrayList<VektorD>> ret=new ArrayList<ArrayList<VektorD>>();
        int accuracy=100;
        for (int i=0;i<masses.size();i++){
            ret.add(new ArrayList<VektorD>());
            for (int j=0;j<masses.get(i).o.pos.size();j=j+accuracy){
                ret.get(i).add(masses.get(i).o.pos.get(j));
            }
        }
        return ret;
    }
    
    /**
     * Berechnet die (Nicht-Kepler-)Orbits aller Objekte in diesem Space ab dem Aufruf dieser Methode für (dtime) Sekunden
     */
    public void calcOrbits(long dtime){ //irgendetwas hier oder in der Verwendung der Orbits ist falsch
        ArrayList<VektorD>[] poss=new ArrayList[masses.size()]; //Positionslisten
        ArrayList<VektorD>[] vels=new ArrayList[masses.size()]; //Geschwindigkeitslisten
        ArrayList<Double>[]masss=new ArrayList[masses.size()]; //Massenlisten, für Schiffe mit MassChanges
        for (int i=0;i<masses.size();i++){
            poss[i]=new ArrayList<VektorD>();
            poss[i].add(masses.get(i).getPos()); //erste Position, Zeit 0
            vels[i]=new ArrayList<VektorD>();
            vels[i].add(masses.get(i).getVel());
            masss[i]=new ArrayList<Double>();
            masss[i].add(masses.get(i).getMass());
        }
        for (double t=0;t<dtime;t=t+Settings.SPACE_CALC_PERIOD_INGAME){
            int k=(int) Math.round(t/Settings.SPACE_CALC_PERIOD_INGAME); //zeitlicher Index in poss und vels
            //k sollte immer kleiner als Double.MAX_VALUE sein
            for (int i=0;i<masses.size();i++){ //Masse, deren Orbit berechnet wird
                double m2=masss[i].get(k);
                VektorD pos2=poss[i].get(k);
                if (pos2!=null){
                    VektorD Fg=new VektorD(0,0);
                    for (int j=0;j<masses.size();j++){
                        double m1=masss[j].get(k);
                        VektorD pos1=poss[j].get(k);
                        if (pos1.x!=pos2.x || pos1.y!=pos2.y){
                            VektorD posDiff=pos1.subtract(pos2);
                            VektorD Fgj=posDiff.multiply(Settings.G*m1*m2/Math.pow(posDiff.getLength(),3));
                            Fg=Fg.add(Fgj);
                        }
                    }
                    VektorD dx=Fg.multiply(Math.pow(Settings.SPACE_CALC_PERIOD_INGAME,2)).divide(m2).divide(2); //x=1/2*a*t^2
                    if (masses.get(i) instanceof ShipS){
                        ArrayList<OrbitChange> os=((ShipS) masses.get(i)).orbitChanges;
                        for (int j=0;j<os.size();j++){
                            if (t+inGameTime>=os.get(j).t0 && t+inGameTime<os.get(j).t1){
                                dx=dx.add(os.get(j).F.multiply(Math.pow(Settings.SPACE_CALC_PERIOD_INGAME,2)).divide(m2).divide(2));
                            }
                        }
                    }
                    dx=dx.add(vels[i].get(k).multiply(Settings.SPACE_CALC_PERIOD_INGAME));
                    boolean hasCrash=false;
                    
                    //irgendwas hier stimmt nicht
                    for (int j=0;j<masses.size();j++){
                        if (j!=i){ //kein Zusammenstoß mit sich selbst
                            /*Intersektion eines Kreises mit einer Linie:
                            K: (x-mx)^2 + (y-my)^2 <= r^2
                            L: x = sx + dx*t
                               y = sy + dy*t
                            => (sx+dx*t-mx)^2 + (sy-dy*t-my)^2 <= r^2 nach t auflösen
                            (sx-mx)^2 + (sx-mx)*dx*t + (dx^2)*(t^2) + (sy-my)^2 + (sy-my)*dy*t + (dy^2)*(t^2) <= r^2
                            a = dx^2 + dy^2
                            b = (sx-mx)*dx + (sy-dy)*dy
                            c = (sx-mx)^2 + (sy-my)^2 - r^2
                            */
                            int r=0;
                            if (masses.get(j) instanceof PlanetS){
                                r=((PlanetS) masses.get(j)).getRadius();
                            }
                            double a = Math.pow(dx.x,2) + Math.pow(dx.y,2);
                            double b = (pos2.x-poss[j].get(k).x)*dx.x + (pos2.y-poss[j].get(k).y)*dx.y;
                            double c = Math.pow(pos2.x-poss[j].get(k).x,2) + Math.pow(pos2.y-poss[j].get(k).y,2) - Math.pow(r,2);
                            double disk=Math.pow(b,2)-4*a*c; //Diskriminante
                            if (disk>=0){
                                //Zusammenstoß mit einem Planeten, hier sollte im Normalfall toCraft aufgerufen werden
                                double t1 = (-b+Math.sqrt(disk)) / (2*a);
                                double t2 = (-b-Math.sqrt(disk)) / (2*a);
                                double t0;
                                if (t1<=t2 && t1>=0 && t1<=1){
                                    t0=t1;
                                }
                                else if (t2>=0 && t2<=1){
                                    t0=t2;
                                }
                                else{
                                    t0=-1;
                                }
                                //das kleinere der beiden, das in [0;1] liegt, da t(0) mit dem Abstand vom derzeitigen Punkt zusammenhängt
                                //(t0<0 => falsche Richtung, t0>1 => zu weit, als dass der Planet tatsächlich erreicht würde)
                                if (Math.signum(t1)!=Math.signum(t2)){ //im Planeten
                                    poss[i].add(pos2);
                                    vels[i].add(new VektorD(0,0));
                                    hasCrash=true;
                                }
                                else if (t0!=-1){ //Crash in diesem Augenblick in den Planeten
                                    VektorD dx1=dx.multiply(t0);
                                    poss[i].add(pos2.add(dx1));
                                    vels[i].add(dx1.divide(Settings.SPACE_CALC_PERIOD_INGAME/t0));
                                    hasCrash=true;
                                    //System.out.println("crash into planet "+dx1.divide(Settings.SPACE_CALC_PERIOD_INGAME/t0)+" "+pos2.add(dx1));
                                }
                            }
                        }
                    }
                    if (!hasCrash){
                        poss[i].add(pos2.add(dx));
                        vels[i].add(dx.divide(Settings.SPACE_CALC_PERIOD_INGAME));
                        //System.out.println(dx+" "+pos2.add(dx));
                    }
                    double mass=masss[i].get(k);
                    if (masses.get(i) instanceof ShipS){
                        for (int j=0;j<((ShipS) masses.get(i)).massChanges.size();j++){
                            MassChange mc=((ShipS) masses.get(i)).massChanges.get(j);
                            if (t+inGameTime>=mc.t0 && t+inGameTime<mc.t1){
                                double dm=mc.dMass/(mc.t1-mc.t0)*(Settings.SPACE_CALC_PERIOD_INGAME);
                                mass=mass+dm;
                            }
                        }
                    }
                    masss[i].add(mass);
                }
            }
        }
        for (int i=0;i<masses.size();i++){
            Orbit o=new Orbit(poss[i],masss[i],inGameTime,inGameTime+dtime);
            masses.get(i).setOrbit(o);
        }
    }
}