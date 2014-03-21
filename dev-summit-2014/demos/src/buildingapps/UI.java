/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/
package buildingapps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Random;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.symbol.Symbol;


public class UI {
  
  // symbols
  static final Symbol STOP_SYM = new SimpleMarkerSymbol(Color.BLUE, 26, Style.CIRCLE);
  static final Symbol BARRIER_SYM = new SimpleMarkerSymbol(Color.RED, 24, Style.X);
  static final Symbol ROUTE_SYM = new SimpleLineSymbol(Color.BLUE, 4, SimpleLineSymbol.Style.DASH);
  static final Symbol RESULT_SYM = new PictureMarkerSymbol(
    "http://static.arcgis.com/images/Symbols/Basic/RedShinyPin.png");
  static final SimpleLineSymbol SYM_ZONE_BORDER = new SimpleLineSymbol(Color.BLACK, 1);
  static final SimpleFillSymbol SYM_DARK = new SimpleFillSymbol(new Color(255, 0, 0, 80), SYM_ZONE_BORDER); // red
  static final SimpleFillSymbol SYM_MED = new SimpleFillSymbol(new Color(255, 120, 0, 80), SYM_ZONE_BORDER); // orange
  static final SimpleFillSymbol SYM_LIGHT = new SimpleFillSymbol(new Color(255, 255, 0, 80), SYM_ZONE_BORDER); // yellow
  static final SimpleFillSymbol[] zoneFillSymbols = new SimpleFillSymbol[] {SYM_DARK, SYM_MED, SYM_LIGHT};
  
  // font
  static final Font FONT = new Font("Segoe UI Semibold", Font.PLAIN, 20);
    
  // cursors
  static final Cursor SELECTED_CURSOR         = new Cursor(Cursor.HAND_CURSOR);
  static final Cursor DEFAULT_CURSOR          = Cursor.getDefaultCursor();
  
  // colors
  final static Dimension VFD  = new Dimension(0, 20);
  final static Dimension VFD_BIG  = new Dimension(0, 100);
  final static Color FOREGROUND = Color.WHITE;
  final static Color BACKGROUND = new Color(30, 30, 255); //new Color(118,34,132);
  final static Color ON_HOVER = Color.YELLOW;
  
  private static Color[] colors = new Color[] {
  Color.BLACK,
  Color.RED,
  Color.MAGENTA,
  Color.BLUE
  };

  public static SimpleLineSymbol createRouteSym() {
  Random random = new Random();
  return new SimpleLineSymbol(
    colors[random.nextInt(colors.length)], 4, SimpleLineSymbol.Style.DASH);
  }
  
  public static void addSeparator(JToolBar toolbar) {
    Box.Filler separator = new Box.Filler(UI.VFD, UI.VFD, UI.VFD);
    separator.setBackground(Color.WHITE);
    toolbar.add(separator);
  }
  
  public static void addBigSeparator(JToolBar toolbar) {
    Box.Filler separator = new Box.Filler(UI.VFD_BIG, UI.VFD_BIG, UI.VFD_BIG);
    separator.setBackground(Color.WHITE);
    toolbar.add(separator);
  }
  
  public static JFrame createWindow() {
    JFrame window = new JFrame();
    window.setUndecorated(true);
    window.setExtendedState(Frame.MAXIMIZED_BOTH);
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.getContentPane().setLayout(new BorderLayout(0, 0));
    return window;
  }
  
  public static JLabel createLabel(String text) {
    return new CustomLabel(text);
  }
  
  public static JToolBar createToolbar() {
    JToolBar toolbar = new JToolBar();
    toolbar.setBorder(new LineBorder(Color.BLACK, 2));
    toolbar.setFloatable(false);
    toolbar.setOrientation(SwingConstants.VERTICAL);
    toolbar.setBackground(UI.BACKGROUND);
    return toolbar;
  }
  
  public static JTextField createSearchBox(String str) {
    final JTextField searchBox = new JTextField("Enter search text");
    searchBox.setHorizontalAlignment(SwingConstants.RIGHT);
    searchBox.setFont(UI.FONT);
    searchBox.setMinimumSize(new Dimension(220, 40));
    searchBox.setMaximumSize(new Dimension(220, 40));
    searchBox.setCaretColor(Color.BLACK);
    searchBox.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent e) {
      }

      @Override
      public void focusGained(FocusEvent e) {
        searchBox.setText("");
      }
    });
    return searchBox;
  }
  
  public static JPanel createSearchPanel(JTextField searchBox) {
    JPanel searchPanel = new JPanel();
    searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.LINE_AXIS));
    searchPanel.setBackground(UI.BACKGROUND);
    searchPanel.setMaximumSize(new Dimension(220, 50));
    searchPanel.setBorder(new LineBorder(Color.BLACK, 2));
    searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    searchPanel.add(searchBox);
    return searchPanel;
  }
  
  public static JButton createButton(String label) {
    JButton btn = new CustomButton(label);
    return btn;
  }
  
  public static JMenuBar createMenuBar(JMenu menuAdd, JButton btnEdit, JButton btnQuery) {
    JMenuBar menuBar = new JMenuBar();
      menuBar.setFont(UI.FONT);
      menuBar.setBackground(UI.BACKGROUND);
      menuBar.setLayout(new FlowLayout());
      menuBar.setAlignmentX(Component.CENTER_ALIGNMENT);
      menuBar.add(menuAdd);
      menuBar.add(new JSeparator());
      menuBar.add(btnEdit);
      menuBar.add(new JSeparator());
      menuBar.add(btnQuery);
      return menuBar;
  }
  
  public static JMenu createMenu(String label) {
    JMenu subMenu = new JMenu(label);
    subMenu.setForeground(UI.FOREGROUND);
    subMenu.setFont(UI.FONT);
    return subMenu;
  }
  
  public static JPanel createQueryResultPanel(DefaultTableModel tblQueryResultModel) {
    JPanel queryPanel = new JPanel();
    queryPanel.setMaximumSize(new Dimension(1000, 200));
    queryPanel.setPreferredSize(new Dimension(1000, 200));
    queryPanel.setLayout(new BoxLayout(queryPanel, BoxLayout.Y_AXIS));
    queryPanel.setVisible(true);
    
    final JTable tblQueryResult = new JTable(tblQueryResultModel);
     /* tblQueryResult.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      
      @Override
      public void valueChanged(ListSelectionEvent e) {
        int row = tblQueryResult.getSelectedRow();
        Point g = (Point) tblQueryResultModel.getValueAt(row, 4);
        map.zoomTo(g);
      }
    });*/
      
      JScrollPane tblQueryScrollPane = new JScrollPane(tblQueryResult);
      //tblQueryScrollPane.getViewport().setBackground(UI.COLOR_PURPLE);
      queryPanel.add(tblQueryScrollPane);
      
    return queryPanel;
  }
}

class CustomButton extends JButton implements MouseListener {

  private static final long serialVersionUID = 1L;
  protected Color transColor = new Color(0, 0, 0, 0);
  private static CustomButton btnLastClicked = null;
  
  public CustomButton() {
  }

  public CustomButton(Icon icon) {
    super(icon);
    setOpaque(false);
    setBorder(null);
    setForeground(UI.FOREGROUND);
    setBackground(transColor);
    addMouseListener(this);
  }

  public CustomButton(String text) {
    super("<html>" + text + "</html>");
    setMinimumSize(new Dimension(100,  100));
    setOpaque(false);
    setBorder(null);
    setForeground(UI.FOREGROUND);
    setBackground(transColor);
    setFont(UI.FONT);
    addMouseListener(this);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    unselect(btnLastClicked);
    select(this);
    btnLastClicked = this;
  }

  @Override
  public void mousePressed(MouseEvent e) {}

  @Override
  public void mouseReleased(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {
    select(this);
  }

  @Override
  public void mouseExited(MouseEvent e) {
    if (btnLastClicked == null) {
      unselect(this);
    } else if (!btnLastClicked.equals(this)) {
      unselect(this);
    }
  }
  
  private static void select(CustomButton btn) {
    if (btn == null) {
      return;
    }
    btn.setForeground(Color.BLACK);
    btn.setBackground(UI.ON_HOVER);
    btn.setOpaque(true);
    btn.setCursor(UI.SELECTED_CURSOR);
  }
  
  private static void unselect(CustomButton btn) {
    if (btn == null) {
      return;
    }
    btn.setForeground(UI.FOREGROUND);
    btn.setBackground(UI.FOREGROUND);
    btn.setOpaque(false);
    btn.setCursor(UI.DEFAULT_CURSOR);
  }
}

class CustomLabel extends JLabel {

  private static final long serialVersionUID = 1L;
  protected Color transColor = new Color(0, 0, 0, 0);
  
  public CustomLabel(String text) {
    super(text);
    setMinimumSize(new Dimension(100,  100));
    setOpaque(true);
    setBorder(null);
    setForeground(Color.BLACK);
    setBackground(UI.ON_HOVER);
    setFont(UI.FONT);
  }
}

