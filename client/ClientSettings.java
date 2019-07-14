package client;
import util.geom.VektorI;
/**
 * Einstellungen, die clientseitig oder server- und clientseitig ben�tigt werden
 */
public abstract class ClientSettings{
    /**
     * in Millisekunden
     */
    public static long PLAYERC_TIMER_PERIOD=30;
    /**
     * in Millisekunden, so h�ufig wird der Spieler mit der Kopie des Spielers am Server synchronisiert
     */
    public static long SYNCHRONIZE_REQUEST_PERIOD=1000;
    /**
     * eigentlich 6.674*10^(-11)
     */
    public static double G=6.674*Math.pow(10,-4);
    /**
     * in Millisekunden
     */
    public static long SPACE_TIMER_PERIOD=1000;
    /**
     * in Sekunden, so oft werden die Bahnen mit dem newtonschen Gravitationsgesetz berechnet
     */
    public static double SPACE_CALC_PERIOD_INGAME=0.1;
    /**
     * jede 100-ste Orbit-Position wird auch tats�chlich gesendet und gezeichnet
     */
    public static int SPACE_GET_ORBIT_ACCURACY=100;
    /**
     * in Sekunden (inGame). So lange werden die Orbits jedes Mal (jede SpaceTimerPeriod) berechnet (mehr als eigentlich n�tig w�re)
     */
    public static long SPACE_CALC_TIME=2000;
    /**
     * in Sekunden (inGame), so oft werden die Subsandboxen auf Kollision getestet
     */
    public static double SANDBOX_CALC_PERIOD_INGAME=0.1;
    /**
     * Anzahl an Bl�cken, die man als Spieler (in Craft) um sich herum sieht
     */
    public static VektorI PLAYERC_FIELD_OF_VIEW = new VektorI(29,19);
    /**
     * in Bl�cken
     */
    public static VektorI PLAYERC_MAPIDCACHE_SIZE = PLAYERC_FIELD_OF_VIEW.multiply(2);
    /**
     * in Bl�cken, so gro� k�nnen alle Schiffe (maximal) sein
     */
    public static VektorI SHIP_SIZE = new VektorI(10,20);
    /**
     * nur weil es nervig ist, st�ndig Zeug aus-und einkommentieren zu müssen
     */
    public static boolean PRINT_COMMUNICATION=false;
    /**
     * In dieser Datei steht die Liste aller Server. Die Endung ist .txt.
     */
    public static String SERVERLIST_FILENAME="serverlist";
    /**
     * IP-Adresse des Servers, mit dem sich der Client verbindet
     */
    public static String SERVER_ADDRESS="localhost";
    /**
     * Port des Servers, mit dem sich der Client verbindet
     */
    public static int SERVER_PORT=30000;
}