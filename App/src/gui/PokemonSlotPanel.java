package gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class PokemonSlotPanel extends RoundedPanel {

    private int owner;
    private int slotType;
    private GameWindows.VisualPokemonData data;

    private JLabel nameLabel;
    private JLabel spriteLabel;
    private JLabel hpLabel;
    private JLabel energyLabel;

    public PokemonSlotPanel(int owner, int slotType, Color bgColor) {
        super(28, bgColor);
        this.owner = owner;
        this.slotType = slotType;
        setLayout(null);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        nameLabel = new JLabel("");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        nameLabel.setBounds(12, 6, 145, 16);
        add(nameLabel);

        spriteLabel = new JLabel("", SwingConstants.CENTER);
        spriteLabel.setBounds(8, 24, 54, 54);
        add(spriteLabel);

        hpLabel = new JLabel("");
        hpLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hpLabel.setBounds(66, 32, 95, 16);
        add(hpLabel);

        energyLabel = new JLabel("");
        energyLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        energyLabel.setBounds(66, 50, 98, 16);
        add(energyLabel);
    }

    public int getOwner() {
        return owner;
    }

    public int getSlotType() {
        return slotType;
    }

    public GameWindows.VisualPokemonData getData() {
        return data;
    }

    public void setPokemon(GameWindows.VisualPokemonData data) {
        this.data = data;
        refresh();
    }

    public void refresh() {
        if (data == null) {
            nameLabel.setText("");
            spriteLabel.setIcon(null);
            hpLabel.setText("");
            energyLabel.setText("");
        } else {
            nameLabel.setText(data.name + " · " + data.stage);
            hpLabel.setText("PS: " + data.currentHp + "/" + data.maxHp);
            if (data.energyCount > 0) {
                energyLabel.setText(data.energyDisplayName + " x" + data.energyCount);
            } else {
                energyLabel.setText("Sin energía");
            }
            ImageIcon icon = MediaUtils.loadSprite(data.spritePath, 50, 50);
            spriteLabel.setIcon(icon);
        }
        repaint();
    }
}
