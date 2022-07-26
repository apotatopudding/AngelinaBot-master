package top.strelitzia.util;

import javax.swing.*;
import java.awt.*;
import java.awt.font.GlyphVector;

public class Test extends JComponent {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame test = new JFrame("top.strelitzia.util.Test");

            test.setContentPane(new Test());
            test.pack();
            test.setLocationRelativeTo(null);
            test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            test.setVisible(true);
        });
    }

    Test() {

        setPreferredSize(new Dimension(800, 600));
    }

    @Override
    protected void paintComponent(Graphics g) {

        Font f = new Font("Courier New", Font.BOLD, 140);
        GlyphVector v = f.createGlyphVector(getFontMetrics(f).getFontRenderContext(), "Hello");
        Shape shape = v.getOutline();

        Rectangle bounds = shape.getBounds();

        Graphics2D gg = (Graphics2D) g;
        gg.translate(
                (getWidth() - bounds.width) / 2 - bounds.x,
                (getHeight() - bounds.height) / 2 - bounds.y
        );
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gg.setColor(Color.WHITE);
        gg.fill(shape);
        gg.setColor(Color.BLUE.darker().darker());
        gg.setStroke(new BasicStroke(3));
        gg.draw(shape);
    }
}