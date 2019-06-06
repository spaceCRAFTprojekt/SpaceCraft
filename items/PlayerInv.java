package items;

import util.geom.*;
import menu.MenuSettings;
import menu.Hotbar;
/**
 * Ein SpielerInventar (Also das inventar mit der entsprechenden Gr��e)
 */
public class PlayerInv extends Inv
{
    public transient Hotbar hotbar;
    public PlayerInv(){
        super(MenuSettings.INV_SIZE);
        
    }
    
    @Override public void update(){
        if (hotbar != null)hotbar.updateSlots();
    }
    
    public void setHotbar(Hotbar hotbar){
        this.hotbar = hotbar;
    }
    
    /**
     * gibt den gerade ausgewählten Stack zurück
     */
    public Stack getHotStack(){
        if(hotbar == null)return null;
        return hotbar.getHotStack();
    }
}
    