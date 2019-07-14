package menu;
import util.geom.VektorI;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Font;

/**
 * Vereinfachung für eine Textbox
 */
public class MenuTextField extends JTextField{
    private Menu m;
    /**
     * Constructor for objects of class MenuTextbox
     */
    public MenuTextField(Menu m, String text, VektorI pos, VektorI size)
    {
        // Erstellt ein neues Textfeld
        super();
        this.m = m;
        setBounds(pos.x, pos.y,size.x,size.y);  // Position und Größe
        setBackground(Color.WHITE);
        setForeground(new Color(0,0,0));
        setEnabled(true);
        setFont(new Font("sansserif",0,MenuSettings.MENU_FONT_SIZE));
        setText(text);
        setVisible(true);
        m.contentPane.add(this); // und fügt ihn zur Pane hinzu
        
    }
}
