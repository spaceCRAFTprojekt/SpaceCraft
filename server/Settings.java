package server;
import util.geom.*;
import java.io.File;
/**
 * Klasse f�r alle Konstanten, die nur serverseitig ben�tigt werden (alle anderen stehen in client.ClientSettings).
 * Ob diese dann aber alle einstellbar sind, ist noch nicht sicher.
 */
public abstract class Settings{
    /**
     * Wenn der Server schlie�t, wird das Spiel hier gespeichert (bisher immer in einer Datei namens "main.ser,
     * also kann es nur einen Server geben).
     */
    public static String GAMESAVE_FOLDER="."+File.separator+"gamesaves";
    /**
     * Anzahl der Bl�cke, die ein Piston nach vorne bewegen kann
     */
    public static int CRAFT_PISTON_PUSH_LIMIT = 4;
    /**
     * Port, auf dem der Server l�uft
     */
    public static int SERVER_PORT=30000;
    /**
     * Nach 10 Sekunden ohne Aktion wird ein Request-Client rausgeschmissen.
     */
    public static long REQUEST_THREAD_TIMEOUT=10000;
}