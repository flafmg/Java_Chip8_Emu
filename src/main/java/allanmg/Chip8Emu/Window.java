package allanmg.Chip8Emu;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Window extends Canvas {
    private JFrame frame;
    private CHIP8 chip8;
    private static int scaleFactor;
    private BufferedImage Buffer;
    private Graphics BufferGraphics;

    public Window(int scaleFactor) {


        this.scaleFactor = scaleFactor;
        this.setBackground(Color.black);

        //set look and feel to os default (because the metal theme is UGLY)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // create the JFrame
        frame = new JFrame("Chip8Emulador - (no roms loaded) - STOPED");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(64 * scaleFactor, 33 * scaleFactor));

        // create the menu bar
        JMenuBar menuBar = new JMenuBar();

        // create the file menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOpenFileDialog();
            }
        });
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        fileMenu.add(openItem);
        fileMenu.add(exitItem);

        // create the emulation menu
        JMenu emulationMenu = new JMenu("Emulation");
        JMenuItem pauseItem = new JMenuItem("Pause");
        pauseItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(loadedFileName != null && chip8.state == State.RUNNING) {
                    chip8.pause();
                    updateTitle();
                }else {
                    JOptionPane.showMessageDialog(null, "No roms loaded or emulator already paused", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JMenuItem playItem = new JMenuItem("Play");
        playItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(loadedFileName != null && chip8.state != State.RUNNING) {
                    chip8.start();
                    updateTitle();
                }else {
                    JOptionPane.showMessageDialog(null, "No roms loaded or emulator already running", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JMenuItem stopItem = new JMenuItem("Stop");
        stopItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if(loadedFileName != null) {
                    chip8.stop();
                    loadedFileName = null;
                    updateTitle();
                }else {
                    JOptionPane.showMessageDialog(null, "No roms loaded, please load a rom", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JMenuItem resetItem = new JMenuItem("Reset");
        resetItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(chip8.state == State.STOPED){
                    JOptionPane.showMessageDialog(null, "No roms loaded, please load a rom", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                chip8.reset();

            }
        });

        JMenuItem advanceItem = new JMenuItem("Advance");
        advanceItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chip8.step();
            }
        });
        emulationMenu.add(pauseItem);
        emulationMenu.add(playItem);
        emulationMenu.add(stopItem);
        emulationMenu.add(resetItem);
        emulationMenu.add(advanceItem);

        // add the menus to the menu bar
        menuBar.add(fileMenu);
        menuBar.add(emulationMenu);

        // add the menu bar to the frame
        frame.setJMenuBar(menuBar);

        // set up the rest of the frame
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        // create the back buffer
        Buffer = new BufferedImage(64, 33, BufferedImage.TYPE_INT_RGB);
        BufferGraphics = Buffer.getGraphics();
        frame.setVisible(true);

        chip8 = new CHIP8(this);
    }
   public void updateTitle(){
        if(loadedFileName != null){
            frame.setTitle("CHIP8 EMU - "+loadedFileName+ " - (" +chip8.state+")" );
        }else {
            frame.setTitle("CHIP8 EMU - (NO ROMS LOADED)" );
        }
   }
   public void setTitle(String title){
        frame.setTitle(title);
   }
    public String loadedFileName;
    void showOpenFileDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        // add a filter to only show .ch8 files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Chip-8 files", "ch8");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            Path path = Paths.get(selectedFile.getAbsolutePath());

            chip8.load(path);
            loadedFileName = path.getFileName().toString();
            updateTitle();
        }
    }

    public void draw(DrawFunction drawFunction) {
        // draw to the back buffer
        drawFunction.draw(BufferGraphics);

        // rescale the buffer and draw to the canvas
        Graphics g = getGraphics();
        g.drawImage(Buffer, 0, 0, 64 * scaleFactor, 33* scaleFactor, null);
        g.dispose();
    }

    @FunctionalInterface
    public interface DrawFunction {
        void draw(Graphics g);
    }
}
