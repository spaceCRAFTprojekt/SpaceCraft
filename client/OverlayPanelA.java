package client;
import util.geom.*;
/**
 * Das Overlay Panel f�r alle (Space und Craft). Zum Beispiel f�r den Chat
 */
public class OverlayPanelA extends OverlayPanel
{
    public OverlayPanelA(Frame frame, Player p, VektorI screenSize){
        super(frame,p,screenSize);
        setVisible(true);
    }
}
