/*
 *
 * Copyright (c) 2000-2009 by Rodney Kinney, Brent Easton
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package VASSAL.counters;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.Objects;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import VASSAL.build.GameModule;
import VASSAL.build.GpIdSupport;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.build.module.documentation.HelpWindow;
import VASSAL.build.module.documentation.HelpWindowExtension;
import VASSAL.build.widget.PieceSlot;
import VASSAL.tools.BrowserSupport;
import VASSAL.tools.ErrorDialog;
import VASSAL.tools.ReflectionUtils;
import VASSAL.tools.swing.SwingUtils;

/**
 * This is the GamePiece designer dialog.  It appears when you edit
 * the properties of a "Single Piece" in the Configuration window.
 */
public class PieceDefiner extends JPanel implements HelpWindowExtension {
  private static final long serialVersionUID = 1L;

  protected static DefaultListModel<GamePiece> availableModel;
  protected DefaultListModel<GamePiece> inUseModel;
  protected ListCellRenderer<GamePiece> r;
  protected PieceSlot slot;
  private GamePiece piece;
  protected static TraitClipboard clipBoard;
  protected String pieceId = "";
  protected JLabel pieceIdLabel = new JLabel("");
  protected GpIdSupport gpidSupport;
  protected boolean changed;

  /** Creates new form test */
  public PieceDefiner() {
    initDefinitions();
    inUseModel = new DefaultListModel<>();
    r = new GamePieceRenderer();
    slot = new PieceSlot();
    initComponents();
    availableList.setSelectedIndex(0);
    setChanged(false);
    gpidSupport = GameModule.getGameModule().getGpIdSupport();
  }

  public PieceDefiner(String id, GpIdSupport s) {
    this();
    pieceId = id;
    pieceIdLabel.setText("Id: "+id);
    gpidSupport = s;
  }

  public PieceDefiner(GpIdSupport s) {
    this();
    gpidSupport = s;
  }

  protected static void initDefinitions() {
    if (availableModel == null) {
      availableModel = new DefaultListModel<>();
      availableModel.addElement(new BasicPiece());
      availableModel.addElement(new Delete());
      availableModel.addElement(new Clone());
      availableModel.addElement(new Embellishment());
      availableModel.addElement(new UsePrototype());
      availableModel.addElement(new Labeler());
      availableModel.addElement(new ReportState());
      availableModel.addElement(new TriggerAction());
      availableModel.addElement(new GlobalHotKey());
      availableModel.addElement(new ActionButton());
      availableModel.addElement(new FreeRotator());
      availableModel.addElement(new Pivot());
      availableModel.addElement(new Hideable());
      availableModel.addElement(new Obscurable());
      availableModel.addElement(new SendToLocation());
      availableModel.addElement(new CounterGlobalKeyCommand());
      availableModel.addElement(new Translate());
      availableModel.addElement(new ReturnToDeck());
      availableModel.addElement(new Immobilized());
      availableModel.addElement(new PropertySheet());
      availableModel.addElement(new TableInfo());
      availableModel.addElement(new PlaceMarker());
      availableModel.addElement(new Replace());
      availableModel.addElement(new NonRectangular());
      availableModel.addElement(new PlaySound());
      availableModel.addElement(new MovementMarkable());
      availableModel.addElement(new Footprint());
      availableModel.addElement(new AreaOfEffect());
      availableModel.addElement(new SubMenu());
      availableModel.addElement(new MenuSeparator());
      availableModel.addElement(new RestrictCommands());
      availableModel.addElement(new Restricted());
      availableModel.addElement(new Marker());
      availableModel.addElement(new DynamicProperty());
      availableModel.addElement(new CalculatedProperty());
      availableModel.addElement(new SetGlobalProperty());
    }
  }

  /**
   * Plugins can add additional GamePiece definitions
   * @param definition
   */
  public static void addDefinition(GamePiece definition) {
    initDefinitions();
    availableModel.addElement(definition);
  }

  public void setPiece(GamePiece piece) {
    inUseModel.clear();
    while (piece instanceof Decorator) {
      final Class<? extends GamePiece> pieceClass = piece.getClass();

      inUseModel.insertElementAt(piece, 0);
      boolean contains = false;
      for (int i = 0,j = availableModel.size(); i < j; ++i) {
        if (pieceClass.isInstance(availableModel.elementAt(i))) {
          contains = true;
          break;
        }
      }

      if (!contains) {
        try {
          availableModel.addElement(pieceClass.getConstructor().newInstance());
        }
        catch (Throwable t) {
          ReflectionUtils.handleNewInstanceFailure(t, pieceClass);
        }
      }

      piece = ((Decorator) piece).piece;
    }

    inUseModel.insertElementAt(Objects.requireNonNullElseGet(piece, BasicPiece::new), 0);
    inUseList.setSelectedIndex(0);
    refresh();
  }

  @Override
  @Deprecated
  public void setBaseWindow(HelpWindow w) {
  }

  private void refresh() {
    if (inUseModel.getSize() > 0) {
      piece = inUseModel.lastElement();
    }
    else {
      piece = null;
    }
    slot.setPiece(piece);
    slot.getComponent().repaint();
  }

  public GamePiece getPiece() {
    return piece;
  }

  public void setChanged(boolean b) {
    changed = b;
  }

  public boolean isChanged() {
    return changed;
  }

  /**
   * This method is called from within the constructor to initialize the form.
   */
  private void initComponents() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(slot.getComponent());

    JPanel controls = new JPanel();
    controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));

    JPanel availablePanel = new JPanel();
    JScrollPane availableScroll = new JScrollPane();
    availableList = new JList<>();
    helpButton = new JButton();
    JButton importButton = new JButton();
    JPanel addRemovePanel = new JPanel();
    addButton = new JButton();
    removeButton = new JButton();
    JPanel inUsePanel = new JPanel();
    JScrollPane inUseScroll = new JScrollPane();
    inUseList = new JList<>();
    propsButton = new JButton();
    JPanel moveUpDownPanel = new JPanel();
    moveUpButton = new JButton();
    moveDownButton = new JButton();
    copyButton = new JButton();
    pasteButton = new JButton();

    availablePanel.setLayout(new BoxLayout(availablePanel, BoxLayout.Y_AXIS));


    availableList.setModel(availableModel);
    availableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    availableList.setCellRenderer(r);
    availableList.addListSelectionListener(evt -> {
      GamePiece gamePiece = availableList.getSelectedValue();
      helpButton.setEnabled(gamePiece instanceof EditablePiece
                            && ((EditablePiece) gamePiece).getHelpFile() != null);
      addButton.setEnabled(gamePiece instanceof Decorator);
    });

    availableScroll.setViewportView(availableList);
    availableScroll.setBorder(new TitledBorder("Available Traits"));

    availablePanel.add(availableScroll);


    helpButton.setText("Help");
    helpButton.addActionListener(evt -> showHelpForPiece());
    availablePanel.add(helpButton);

    importButton.setText("Import");
    importButton.addActionListener(evt -> {
      String className = JOptionPane.showInputDialog(PieceDefiner.this, "Enter fully-qualified name of Java class to import");
      importPiece(className);
    });

    availablePanel.add(importButton);

    controls.add(availablePanel);

    addRemovePanel.setLayout(new BoxLayout(addRemovePanel, BoxLayout.Y_AXIS));

    addButton.setText("Add ->");
    addButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        GamePiece selected = availableList.getSelectedValue();
        if (selected instanceof Decorator) {
          if (inUseModel.getSize() > 0) {
            Decorator c = (Decorator) selected;
            addTrait(c);
            if (inUseModel.lastElement().getClass() == c.getClass()) {
              final boolean addWasSuccessful = edit(inUseModel.size() - 1);
              if (!addWasSuccessful) {
                // Add was cancelled
                if (!inUseModel.isEmpty())
                  removeTrait(inUseModel.size() - 1);
              }
            }
          }
        }
        else if (selected != null && inUseModel.getSize() == 0) {
          GamePiece p = null;
          try {
            p = selected.getClass().getConstructor().newInstance();
          }
          catch (Throwable t) {
            ReflectionUtils.handleNewInstanceFailure(t, selected.getClass());
          }

          if (p != null) {
            setPiece(p);
            if (inUseModel.getSize() > 0) {
              final boolean addWasSuccessful = edit(0);
              if (!addWasSuccessful) {
                // Add was cancelled
                removeTrait(0);
              }
            }
          }
        }
      }
    });
    addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    addRemovePanel.add(addButton);

    removeButton.setText("<- Remove");
    removeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        int index = inUseList.getSelectedIndex();
        if (index >= 0) {
          removeTrait(index);
          if (inUseModel.getSize() > 0) {
            inUseList.setSelectedIndex(
              Math.min(inUseModel.getSize() - 1, Math.max(index, 0)));
          }
        }
      }
    }
    );
    removeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    addRemovePanel.add(removeButton);

    pieceIdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    addRemovePanel.add(pieceIdLabel);

    controls.add(addRemovePanel);

    inUsePanel.setLayout(new BoxLayout(inUsePanel, BoxLayout.Y_AXIS));

    inUseList.setModel(inUseModel);
    inUseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    inUseList.setCellRenderer(r);
    inUseList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent evt) {
        final GamePiece gamePiece = inUseList.getSelectedValue();
        propsButton.setEnabled(gamePiece instanceof EditablePiece);

        final int index = inUseList.getSelectedIndex();
        final boolean copyAndRemove = inUseModel.size() > 0 &&
          (index > 0 || !(inUseModel.getElementAt(0) instanceof BasicPiece));
        copyButton.setEnabled(copyAndRemove);
        removeButton.setEnabled(copyAndRemove);

        pasteButton.setEnabled(clipBoard != null);
        moveUpButton.setEnabled(index > 1);
        moveDownButton.setEnabled(index > 0
                                  && index < inUseModel.size() - 1);
      }
    });

    inUseList.addMouseListener(new MouseAdapter() {
      @Override
// FIXME: mouseClicked()?
      public void mouseReleased(MouseEvent e) {
        if (e.getClickCount() == 2 && SwingUtils.isLeftMouseButton(e)) {
          int index = inUseList.locationToIndex(e.getPoint());
          if (index >= 0) {
            edit(index);
          }
        }
      }
    });
    inUseScroll.setViewportView(inUseList);

    inUseScroll.setBorder(new TitledBorder("Current Traits"));

    inUsePanel.add(inUseScroll);


    propsButton.setText("Properties");
    propsButton.addActionListener(evt -> {
      int index = inUseList.getSelectedIndex();
      if (index >= 0) {
        edit(index);
      }
    });
    inUsePanel.add(propsButton);


    controls.add(inUsePanel);


    moveUpDownPanel.setLayout(new BoxLayout(moveUpDownPanel, BoxLayout.Y_AXIS));

    moveUpButton.setText("Move Up");
    moveUpButton.addActionListener(evt -> {
      int index = inUseList.getSelectedIndex();
      if (index > 1 && index < inUseModel.size()) {
        moveDecoratorUp(index);
      }
    });
    moveUpDownPanel.add(moveUpButton);


    moveDownButton.setText("Move Down");
    moveDownButton.addActionListener(evt -> {
      int index = inUseList.getSelectedIndex();
      if (index > 0 && index < inUseModel.size() - 1) {
        moveDecoratorDown(index);
      }
    });
    moveUpDownPanel.add(moveDownButton);

    copyButton.setText("Copy");
    copyButton.addActionListener(evt -> {
      pasteButton.setEnabled(true);
      int index = inUseList.getSelectedIndex();
      clipBoard = new TraitClipboard((Decorator) inUseModel.get(index));
    });
    moveUpDownPanel.add(copyButton);

    pasteButton.setText("Paste");
    pasteButton.setEnabled(clipBoard != null);
    pasteButton.addActionListener(evt -> {
      if (clipBoard != null) {
        paste();
      }
    });
    moveUpDownPanel.add(pasteButton);

    controls.add(moveUpDownPanel);

    add(controls);

  }

  protected void paste() {
    final Decorator c = (Decorator) GameModule.getGameModule().createPiece(clipBoard.getType(), null);
    if (c instanceof PlaceMarker) {
      ((PlaceMarker) c).updateGpId(GameModule.getGameModule().getGpIdSupport());
    }
    c.setInner(inUseModel.lastElement());
    inUseModel.addElement(c);
    c.mySetState(clipBoard.getState());
    refresh();
  }

  protected void moveDecoratorDown(int index) {
    GamePiece selm1 = inUseModel.elementAt(index - 1);
    Decorator sel = (Decorator) inUseModel.elementAt(index);
    Decorator selp1 = (Decorator) inUseModel.elementAt(index + 1);
    Decorator selp2 = index < inUseModel.size() - 2 ? (Decorator) inUseModel.elementAt(index + 2) : null;
    selp1.setInner(selm1);
    sel.setInner(selp1);
    if (selp2 != null) {
      selp2.setInner(sel);
    }
    inUseModel.setElementAt(selp1, index);
    inUseModel.setElementAt(sel, index + 1);
    inUseModel.lastElement().setProperty(Properties.OUTER, null);
    inUseList.setSelectedIndex(index + 1);
    refresh();
    setChanged(true);
  }

  protected void moveDecoratorUp(int index) {
    final GamePiece selm2 = inUseModel.elementAt(index - 2);
    final Decorator sel = (Decorator) inUseModel.elementAt(index);
    final Decorator selm1 = (Decorator) inUseModel.elementAt(index - 1);
    final Decorator selp1 = index < inUseModel.size() - 1 ?
      (Decorator) inUseModel.elementAt(index + 1) : null;
    sel.setInner(selm2);
    selm1.setInner(sel);
    if (selp1 != null) {
      selp1.setInner(selm1);
    }
    inUseModel.setElementAt(selm1, index);
    inUseModel.setElementAt(sel, index - 1);
    inUseModel.lastElement().setProperty(Properties.OUTER, null);
    inUseList.setSelectedIndex(index - 1);
    refresh();
    setChanged(true);
  }

  protected void importPiece(String className) {
    if (className == null) return;

    Object o = null;
    try {
      o = GameModule.getGameModule().getDataArchive()
                    .loadClass(className).getConstructor().newInstance();
    }
    catch (Throwable t) {
      ReflectionUtils.handleImportClassFailure(t, className);
    }

    if (o == null) return;

    if (o instanceof GamePiece) {
      availableModel.addElement((GamePiece)o);
    }
    else {
      ErrorDialog.show("Error.not_a_gamepiece", className);
    }
  }

  private void showHelpForPiece() {
    Object o = availableList.getSelectedValue();
    if (o instanceof EditablePiece) {
      HelpFile h = ((EditablePiece) o).getHelpFile();
      BrowserSupport.openURL(h.getContents().toString());
    }
  }

  protected boolean edit(int index) {
    Object o = inUseModel.elementAt(index);
    if (!(o instanceof EditablePiece)) {
      return false;
    }
    EditablePiece p = (EditablePiece) o;
    if (p.getEditor() != null) {
      Ed ed;
      Window w = SwingUtilities.getWindowAncestor(this);
      if (w instanceof Frame) {
        ed = new Ed((Frame) w, p);
      }
      else if (w instanceof Dialog) {
        ed = new Ed((Dialog) w, p);
      }
      else {
        ed = new Ed((Frame) null, p);
      }
      final String oldState = p.getState();
      final String oldType = p.getType();
      ed.setVisible(true);
      PieceEditor c = ed.getEditor();
      if (c != null) {
        p.mySetType(c.getType());
        if (p instanceof Decorator) {
          ((Decorator) p).mySetState(c.getState());
        }
        else {
          p.setState(c.getState());
        }
        if ((! p.getType().equals(oldType)) || (! p.getState().equals(oldState))) {
          setChanged(true);
        }
        refresh();
        return true;
      }
    }
    return false;
  }

  /** A Dialog for editing an EditablePiece's properties */
  protected static class Ed extends JDialog {
    private static final long serialVersionUID = 1L;

    PieceEditor ed;

    private Ed(Frame owner, final EditablePiece p) {
      super(owner, p.getDescription() + " properties", true);
      initialize(p);
    }

    private Ed(Dialog owner, final EditablePiece p) {
      super(owner, p.getDescription() + " properties", true);
      initialize(p);
    }

    private void initialize(final EditablePiece p) {
      ed = p.getEditor();
      setLayout(new MigLayout("ins dialog,fill", "[]unrel[]", ""));
      add(ed.getControls(), "spanx 3,grow,push,wrap");

      JButton b = new JButton("Ok");
      b.addActionListener(evt -> dispose());

      add(b, "tag ok");

      b = new JButton("Cancel");
      b.addActionListener(evt -> {
        ed = null;
        dispose();
      });
      add(b, "tag cancel");

      if (p.getHelpFile() != null) {
        b = new JButton("Help");
        b.addActionListener(evt -> p.getHelpFile().showWindow(Ed.this));
        add(b, "tag help");
      }

      pack();
      setLocationRelativeTo(getOwner());
    }

    public PieceEditor getEditor() {
      return ed;
    }
  }

  protected void removeTrait(int index) {
    inUseModel.removeElementAt(index);
    if (index < inUseModel.size()) {
      ((Decorator) inUseModel.elementAt(index)).setInner(inUseModel.elementAt(index - 1));
    }
    refresh();
    setChanged(true);
  }

  protected void addTrait(Decorator c) {
    final Class<? extends Decorator> cClass = c.getClass();
    Decorator d = null;
    try {
      d = cClass.getConstructor().newInstance();
    }
    catch (Throwable t) {
      ReflectionUtils.handleNewInstanceFailure(t, cClass);
    }

    if (d != null) {
      if (d instanceof PlaceMarker) {
        ((PlaceMarker) d).updateGpId(gpidSupport);
      }
      d.setInner(inUseModel.lastElement());
      inUseModel.addElement(d);
      setChanged(true);
    }

    refresh();
  }

  protected JList<GamePiece> availableList;
  private JButton helpButton;
  private JButton addButton;
  private JButton removeButton;
  private JList<GamePiece> inUseList;
  private JButton propsButton;
  private JButton moveUpButton;
  private JButton moveDownButton;
  protected JButton copyButton;
  protected JButton pasteButton;

  private static class GamePieceRenderer extends JLabel implements ListCellRenderer<GamePiece>, Serializable {

    private static final long serialVersionUID = 1L;

    private final DefaultListCellRenderer innerRenderer = new DefaultListCellRenderer();

    @Override
    public Component getListCellRendererComponent(JList<? extends GamePiece> list, GamePiece value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
      innerRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value instanceof EditablePiece) {
        innerRenderer.setText(((EditablePiece) value).getDescription());
      }
      else {
        String s = value.getClass().getName();
        innerRenderer.setText(s.substring(s.lastIndexOf('.') + 1));
      }
      return this;
    }
  }

  /**
   * Contents of the Copy/Paste buffer for traits in the editor
   * @author rkinney
   *
   */
  private static class TraitClipboard {
    private final String type;
    private final String state;
    public TraitClipboard(Decorator copy) {
      type = copy.myGetType();
      state = copy.myGetState();
    }
    public String getType() {
      return type;
    }
    public String getState() {
      return state;
    }
  }
}
