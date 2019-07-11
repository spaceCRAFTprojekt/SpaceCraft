package client.menus;
import util.geom.*;
import client.*;
import menu.*;
public class RocketControllerMenu extends PlayerMenu{
    private MenuButton createShip;
    private MenuButton start;
    public RocketControllerMenu(Player p, int sandboxIndex, VektorI pos, int shipIndex){
        super(p,"RocketControllerMenu",new VektorI(260,160));
        createShip=new MenuButton(this, "Schiff erstellen", new VektorI(10,10) , new VektorI(220,40), MenuSettings.MENU_BIG_FONT){
            public void onClick(){
                new Request(p.getID(),p.getRequestOut(),p.getRequestIn(),"Sandbox.createShip",null,sandboxIndex,pos);
                //ownerID und playerID sind eigentlich redundant
                closeMenu(); //da sich der shipIndex irgendwie updaten muss, und das geht nur, wenn ein neues Men� erstellt wird
            }};
        start=new MenuButton(this, "Schiff starten", new VektorI(10,60),new VektorI(220,40),MenuSettings.MENU_BIG_FONT){
            public void onClick(){
                int supersandboxIndex=((Integer) new Request(p.getID(),p.getRequestOut(),p.getRequestIn(),"Space.getSupersandboxIndex",Integer.class,sandboxIndex).ret).intValue();
                //Da dieses Men� dann aus der Subsandbox aufgerufen wird (da sich der Block dann in der Subsandbox befindet), 
                //muss also die h�here Sandbox f�r den Request gew�hlt werden. Dieser Bug hat mich vermutlich �ber eine Stunde gekostet. -LG
                new Request(p.getID(),p.getRequestOut(),p.getRequestIn(),"Sandbox.startShip",null,supersandboxIndex,shipIndex);
                closeMenu();
            }};
        if (shipIndex==-1){
            createShip.setEnabled(true);
            start.setEnabled(false);
        }
        else{
            createShip.setEnabled(false);
            start.setEnabled(true);
        }
    }
}