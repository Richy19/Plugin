package Renderers;

import Models.GearSpec.GearSpec;
import Models.GearSpec.GearSpecAuthor;

import javax.swing.*;
import java.awt.*;

/**
 * Created by matthewyork on 4/1/14.
 */

public class GearSpecCellRenderer extends JPanel implements ListCellRenderer {
    private static final Color HIGHLIGHT_COLOR = Color.decode("0x2B2B2B");
    private Color cellBackgroundColor = null;

    JPanel specInfoPanel;
    JLabel nameLabel;
    JLabel authorLabel;

    public GearSpecCellRenderer() {
        setOpaque(true);
        cellBackgroundColor = getBackground();
    }

    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
    {
        GearSpec spec = (GearSpec)value;

        //Check for first runthrough
        if(nameLabel == null){
            //Initialize name panel
            this.setLayout(new FlowLayout());
            specInfoPanel = new JPanel();
            specInfoPanel.setLayout(new BoxLayout(specInfoPanel, BoxLayout.Y_AXIS));
            specInfoPanel.setOpaque(false);


            //Set layout
            this.setLayout(new FlowLayout(FlowLayout.LEFT));

            //Set name label
            nameLabel = new JLabel(spec.getName(), JLabel.LEFT);
            nameLabel.setFont(new Font(nameLabel.getFont().getName(), Font.PLAIN, 18));

            //Set author label
            authorLabel = new JLabel("", JLabel.LEFT);
            authorLabel.setFont(new Font(authorLabel.getFont().getName(), Font.PLAIN, 12));

            //Iterate over all authors for matches
            if (spec.getAuthors() != null){
                String authors = "";
                for (GearSpecAuthor author : spec.getAuthors()){
                    authors = authors+author.getName()+", ";
                }
                //Remove last comma/space
                authors = authors.substring(0, authors.length()-2);

                //Set label text
                authorLabel.setText(authors);
            }

            //Add components
            this.add(specInfoPanel);
            specInfoPanel.add(nameLabel);
            specInfoPanel.add(authorLabel);
        }
        else {
            //Set name label
            nameLabel.setText(spec.getName());

            //Set author label
            if (spec.getAuthors() != null){
                String authors = "";
                for (GearSpecAuthor author : spec.getAuthors()){
                    authors = authors+author.getName()+", ";
                }
                //Remove last comma/space
                authors = authors.substring(0, authors.length()-2);

                //Set label text
                authorLabel.setText(authors);
            }

        }

        if(isSelected) {
            setBackground(cellBackgroundColor.darker());
            setOpaque(true);
            //setForeground(Color.white);
        } else {
            setOpaque(false);
            //setForeground(Color.black);
        }
        return this;
    }
}
