package client.menus;
import util.geom.VektorI;
import menu.*;
import client.Player;
/**
 * Wird angezeigt, wenn man esc dr�gggt...  // es ist kurz vor 0 Uhr; ich kann nicht mehr schreiben
 * Gibt die M�glichkeit das Spiel zu beenden oder weiterzuspielen
 */
public class EscapeMenu extends PlayerMenu {
    private MenuLabel pause;
    private MenuButton restart;
    private MenuButton logout;
    private MenuButton exit;
    //Constructor 
    public EscapeMenu(Player p){
        super(p,"Pause", new VektorI(225, 320));
        
        // erstellt ein neues Label
        pause = new MenuLabel(this, "Pause", new VektorI(60,30) ,new VektorI(90,30), MenuSettings.MENU_HEAD_FONT);
        
        // Erstellt einen neuen Button
        restart = new MenuButton(this, "Weiter", new VektorI(30,120), new VektorI(150, 35)){
            public void onClick(){closeMenu();}
        };
         
        logout = new MenuButton(this, "Logout", new VektorI(30,170), new VektorI(150, 35)){
            public void onClick(){logout();}
        };
        
        exit = new MenuButton(this, "Exit", new VektorI(30,220), new VektorI(150, 35)){
            public void onClick(){exit();}
        };
    }

    public void logout(){
        getPlayer().logout();
        closeMenu();
    }
    
    public void exit(){
        getPlayer().exit();
        closeMenu();
    }
}