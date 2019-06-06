package client.menus;

import util.geom.*;
import javax.swing.*;
import menu.*;
import client.*;
import java.awt.Color;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Zum einstellen eines Manoeuvres
 */
public class ManoeuvreInfo extends PlayerMenu
{
    public int massIndex;
    public int manoeuvreIndex; //das manoeuvreIndex-te Man�ver der Masse mit dem MassIndex wird editiert (oder hinzugef�gt). -1 bedeutet einfach hinzuf�gen
    private GroupLayout layout;

    private JLabel rocketInfo;
    private JSeparator separator0;
    private JLabel accLabel;
    private JTextField accField;
    private JLabel angleLabel;
    private JTextField angleField;
    private JToggleButton angleToggle;
    private JSeparator separator1;
    private JPanel tablePanel;
    private JComponent[][]table;
    private JSeparator separator2;
    private JLabel fuelCost;


    public ManoeuvreInfo(Player p, int massIndex, int manoeuvreIndex){
        super(p, "Manoeuvre Info", new VektorI(210,280));   // id muss noch gemacht werden
        this.massIndex=massIndex;
        this.manoeuvreIndex=manoeuvreIndex;

        setFont(MenuSettings.MENU_FONT);
        layout = new GroupLayout(this.getLayeredPane());
        this.getLayeredPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        rocketInfo = new JLabel("Rocket: - ;  mass: - ");
        rocketInfo.setSize(300,12);

        separator0 = new JSeparator(JSeparator.HORIZONTAL);
        separator0.setForeground(Color.gray);

        accLabel = new JLabel("Thrust");
        accLabel.setVisible(true); 
        angleLabel = new JLabel("Angle: ");
        accField = new JTextField();
        angleField = new JTextField();
        angleToggle = new JToggleButton("rel");

        separator1 = new JSeparator(JSeparator.HORIZONTAL);
        separator1.setForeground(Color.gray);

        table = new JComponent[3][4];
        table[1][0] = new JLabel("Time");
        table[2][0] = new JLabel("Location");
        table[0][1] = new JLabel("Start");
        table[0][2] = new JLabel("End");
        table[0][3] = new JLabel("Duration");
        table[1][1] = new JTextField();
        table[1][2] = new JTextField();
        table[1][3] = new JLabel("");
        table[2][1] = new JLabel("");
        table[2][2] = new JLabel("");
        table[2][3] = new JLabel("");

        separator2 = new JSeparator(JSeparator.HORIZONTAL);
        separator2.setForeground(Color.gray);

        fuelCost = new JLabel("Fuel Cost: - ");


        layout.linkSize(SwingConstants.VERTICAL, angleField, accField);
        //layout.linkSize(SwingConstants.HORIZONTAL, table[1][1], table[2][1]);
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(rocketInfo)
                .addComponent(separator0)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(accLabel)
                        .addComponent(angleLabel) )
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER) 
                        .addComponent(accField)
                        .addComponent(angleField) )
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER) 
                        .addComponent(angleToggle) ) )
                .addComponent(separator1)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(table[0][1])
                        .addComponent(table[0][2])
                        .addComponent(table[0][3]))
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER) 
                        .addComponent(table[1][0])
                        .addComponent(table[1][1],50, 50, Short.MAX_VALUE)
                        .addComponent(table[1][2])
                        .addComponent(table[1][3]) )
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER) 
                        .addComponent(table[2][0])
                        .addComponent(table[2][1],50, 50, Short.MAX_VALUE)
                        .addComponent(table[2][2])
                        .addComponent(table[2][3]) )
                    )
                .addComponent(separator2)
                .addComponent(fuelCost)
            ) );


        layout.setVerticalGroup(layout.createSequentialGroup()
            .addComponent(rocketInfo)
            .addComponent(separator0)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(accLabel)
                .addComponent(accField) ) 
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(angleLabel)
                .addComponent(angleField)
                .addComponent(angleToggle) )
            .addComponent(separator1)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(table[1][0])
                .addComponent(table[2][0]) )
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(table[0][1])
                .addComponent(table[1][1])
                .addComponent(table[2][1]) )
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(table[0][2])
                .addComponent(table[1][2])
                .addComponent(table[2][2]) )
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(table[0][3])
                .addComponent(table[1][3])
                .addComponent(table[2][3]) )
            .addComponent(separator2)
            .addComponent(fuelCost)
        );
    }

    public void update(){
        try{
            double angle=Double.parseDouble(angleField.getText())*Math.PI/180; //Eingabe in Grad ist vermutlich sch�ner
            double F=Double.parseDouble(accField.getText());
            VektorD Fvect=new VektorD(Math.cos(angle)*F,Math.sin(angle)*F);
            //Was soll angleToggle tun?
            //dMass und fuelCost m�ssen noch berechnet werd
            long t0=Long.parseLong(((JTextField) table[1][1]).getText());
            long t1=Long.parseLong(((JTextField) table[1][2]).getText());
            Manoeuvre mano=new Manoeuvre(Fvect,0,t0,t1);
            ClientSpace workspace=getPlayer().getPlayerS().getWorkspace();
            ClientMass rocket=workspace.masses.get(massIndex);
            if (manoeuvreIndex>=0 && manoeuvreIndex<rocket.manoeuvres.size()){ //altes Man�ver editieren
                rocket.manoeuvres.set(manoeuvreIndex,mano);
            }
            else if (manoeuvreIndex==-1){ //neues Man�ver hinzuf�gen
                rocket.manoeuvres.add(mano);
                manoeuvreIndex=rocket.manoeuvres.size()-1; 
                //sollte jetzt nat�rlich das neue Man�ver editieren, nicht bei jedem Update ein neues Man�ver hinzuf�gen
            }
            else{ //neues Man�ver an der gegebenen Stelle hinzuf�gen
                rocket.manoeuvres.add(manoeuvreIndex,mano);
            }
            rocketInfo.setText("Rocket: " + "" + ";  mass: " + rocket.getMass());
            fuelCost.setText("Fuelcost: " + "");
            ((JLabel) table[1][3]).setText(Long.toString(t1-t0));
            workspace.calcOrbits(ClientSettings.SPACE_CALC_TIME);
            t0=t0>=workspace.inGameTime ? t0 : workspace.inGameTime; //eher unsch�n, f�hrt zu Ver�nderungen der Werte im Lauf der Zeit
            VektorI pos0=workspace.masses.get(massIndex).getOrbit().getPos(t0).toInt();
            VektorI pos1=workspace.masses.get(massIndex).getOrbit().getPos(t1).toInt();
            ((JLabel) table[2][1]).setText("("+pos0.x+", "+pos0.y+")");
            ((JLabel) table[2][2]).setText("("+pos1.x+", "+pos1.y+")");
            ((JLabel) table[2][3]).setText(Long.toString(Math.round(workspace.masses.get(massIndex).getOrbit().getTravelledDistance(t0,t1))));
        }
        catch(Exception e){}
    }
} 