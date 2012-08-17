package org.nlogo.deltatick;

import org.nlogo.api.Shape;
import org.nlogo.app.App;
import org.nlogo.deltatick.dialogs.ShapeSelector;
import org.nlogo.deltatick.dialogs.VariationSelector;
import org.nlogo.deltatick.dnd.BehaviorInput;
import org.nlogo.deltatick.xml.Breed;
import org.nlogo.deltatick.xml.OwnVar;
import org.nlogo.deltatick.dnd.PrettyInput;
import org.nlogo.deltatick.xml.Variation;
import org.nlogo.hotlink.dialogs.ShapeIcon;
import org.nlogo.hotlink.dialogs.StackedShapeIcon;
import org.nlogo.nvm.Workspace;
import org.nlogo.shape.DrawableShape;
import org.nlogo.shape.ShapesManagerInterface;
import org.nlogo.shape.TurtleShapesManagerInterface;
import org.nlogo.shape.VectorShape;
import org.nlogo.shape.editor.ImportDialog;
import org.nlogo.window.Widget;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.*;
import java.util.List;

import org.nlogo.deltatick.dialogs.TraitSelector;

// BreedBlock contains code for how whatever happens in BreedBlock is converted into NetLogo code -A. (aug 25)

public strictfp class BreedBlock
        extends CodeBlock
        implements java.awt.event.ActionListener,
        ImportDialog.ShapeParser,
        MouseMotionListener,
        MouseListener {

    // "transient" means the variable's value need not persist when the object is stored  -a.
    String breedShape = "default";
    transient Breed breed;
    transient VectorShape shape = new VectorShape();
    transient Frame parentFrame;
    transient ShapeSelector selector;
    transient JButton breedShapeButton;
    transient PrettyInput number;
    transient PrettyInput plural;
    HashMap<String, Variation> breedVariationHashMap = new HashMap<String, Variation>(); // assuming single trait -A. (Aug 8, 2012)
    HashSet<String> myUsedBehaviorInputs = new HashSet<String>();
    List<String> myUsedAgentInputs = new ArrayList<String>();

    //ShapeSelector myShapeSelector;
    int id;
    transient String trait;
    JTextField traitLabel;
    transient String variation;
    HashSet<String> myUsedTraits = new HashSet<String>();
    //HashMap<String, BehaviorInput> myUsedBehaviorInputs = new HashMap<String, BehaviorInput>();

    // constructor for breedBlock without trait & variation
    public BreedBlock(Breed breed, String plural, Frame frame) {
        super(plural, ColorSchemer.getColor(3));
        this.parentFrame = frame;
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        this.setLocation(0, 0);
        this.setForeground(color);
        this.breed = breed;
        number.setText(breed.getStartQuant());



        //myShapeSelector = new ShapeSelector( parentFrame , allShapes() , this );
        setBorder(org.nlogo.swing.Utils.createWidgetBorder());

        flavors = new DataFlavor[]{
                DataFlavor.stringFlavor,
                codeBlockFlavor,
                breedBlockFlavor,
                //patchBlockFlavor
        };

        //setPreferredSize( 250 , 99 );
        //setSize( 250 , 99 );

    }

    // second constructor for breedBlock with trait & variation
    public BreedBlock(Breed breed, String plural, String traitName, String variationName, Frame frame) {
        super(plural, ColorSchemer.getColor(3));
        this.parentFrame = frame;
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        this.setLocation(0, 0);
        this.setForeground(color);
        //this.id = id;
        this.breed = breed;
        number.setText(breed.getStartQuant());
        this.trait = traitName;
        this.variation = variationName;

        TraitSelector traitSelector = new TraitSelector(frame);
        setBorder(org.nlogo.swing.Utils.createWidgetBorder());

        flavors = new DataFlavor[]{
                DataFlavor.stringFlavor,
                codeBlockFlavor,
                breedBlockFlavor,

        };
    }

    public void addBlock(CodeBlock block) {
        myBlocks.add(block);
        this.add(block);
        block.enableInputs();

        block.showRemoveButton();
        this.add(Box.createRigidArea(new Dimension(this.getWidth(), 4)));
        block.setMyParent(this);
        block.doLayout();
        block.validate();
        block.repaint();
        if (block instanceof TraitBlock) {
            myUsedTraits.add(((TraitBlock) block).getTraitName());
            ((TraitBlock) block).makeNumberActive();
            ((TraitBlock) block).enableDropDown();
            ((TraitBlock) block).colorButton.setEnabled(true);
        }
        else if (block instanceof BehaviorBlock) {
            String tmp = ((BehaviorBlock) block).getBehaviorInputName();
            addBehaviorInputToList(tmp);
            String s = ((BehaviorBlock) block).getAgentInputName();
            addAgentInputToList(s);
        }
        else if (block instanceof ConditionBlock) {
            String tmp = ((ConditionBlock) block).getBehaviorInputName();
            addBehaviorInputToList(tmp);
            String s = ((ConditionBlock) block).getAgentInputName();
            addAgentInputToList(s);

        }

        doLayout();
        validate();
        repaint();

        this.getParent().doLayout();
        this.getParent().validate();
        this.getParent().repaint();
    }




    //TODO: Figure out how breed declaration always shows up first in code
    public String declareBreed() {
        return "breed [ " + plural() + " " + singular() + " ]\n";
    }

    //this is where breeds-own variables show up in NetLogo code -A. (aug 25)
    public String breedVars() {
        String code = "";
        if (breed.getOwnVars().size() > 0) {
            code += plural() + "-own [\n";
            for (OwnVar var : breed.getOwnVars()) {
                code += "  " + var.name + "\n";
            }
            code += "\n";
        }


        return code;
    }


    // code to setup in NetLogo code window. This method is called in MBgInfo -A.
    public String setup() {
        String code = "";
        if (breed.needsSetupBlock()) {
            code += "create-" + plural() + " " + number.getText() + " [\n";
            if (breed.getSetupCommands() != null) {
                code += breed.getSetupCommands();
            }
            for (OwnVar var : breed.getOwnVars()) {
                if (var.setupReporter != null) {
                    code += "set " + var.name + " " + var.setupReporter + "\n";
                }
            }
            code += "]\n";
            code += setBreedShape();
            int i;
            for (CodeBlock block : myBlocks) {
                if (block instanceof TraitBlock) {
                    String activeVariation = ((TraitBlock) block).getActiveVariation();
                    HashMap<String, Variation> tmpHashMap = ((TraitBlock) block).getVariationHashMap();
                    if (breedVariationHashMap.isEmpty()) {
                        breedVariationHashMap.putAll(tmpHashMap);
                    }
                    else {
                        breedVariationHashMap.put(activeVariation, tmpHashMap.get(activeVariation));
                    }
                }
            }
            code += setupTrait();
        }
        return code;
    }

    public String setupTrait() {
        String code = "";

        for (String traitName : myUsedTraits) {
            code += "let all-" + plural() + "-" + traitName + " sort " + plural() + " \n";

            int i = 0;
            int startValue = 0;
            int endValue = 0;

            for (Map.Entry<String, Variation> entry : breedVariationHashMap.entrySet()) {
                String variationType = entry.getKey();
                Variation variation = entry.getValue();

                int k = variation.number;
                endValue = startValue + k;

                code += "let " + traitName + i + " sublist all-" + plural() + "-" + traitName +
                        " " + startValue + " " + endValue + "\n";
                code += "foreach " + traitName + i + " [ ask ? [ set " + traitName + " " + variation.value + " \n";
                code += " set color " + variation.color + " ]] \n";

                i++;
                startValue = endValue;
            }
        }

    return code;

    }

    // moves Update Code from XML file to procedures tab - A. (feb 14., 2012)
    public String update() {
        String code = "";
        if (breed.needsUpdateBlock()) {
            code += "ask " + plural() + " [\n";
            if (breed.getUpdateCommands() != null) {
                code += breed.getUpdateCommands();
            }
            for (OwnVar var : breed.getOwnVars()) {
                if (var.updateReporter != null) {
                    code += "set " + var.name + " " + var.updateReporter + "\n";
                }
            }
            code += "]\n";
        }

        return code;
    }

    // very smart! singular is just prefixed plural -A.
    public String singular() {
        return "one-of-" + plural.getText();
    }


    // get text students have entered in prettyInput, plural (march 1)
    public String plural() {
        return plural.getText();
    }

    //public java.awt.Dimension getMinimumSize() {
    //    return new java.awt.Dimension( 250 , 99 );
    //}

    //public Dimension getPreferredSize() {
    //    return new java.awt.Dimension( 250 , 99 );
    //}


    //I don't understand dataflavors
    public Object getTransferData(DataFlavor dataFlavor)
            throws UnsupportedFlavorException {
        if (isDataFlavorSupported(dataFlavor)) {
            if (dataFlavor.equals(breedBlockFlavor)) {
                return this;
            }
            if (dataFlavor.equals(envtBlockFlavor)) {
                return this;
            }
            if (dataFlavor.equals(DataFlavor.stringFlavor)) {
                return unPackAsCode();
            }
            if (dataFlavor.equals(traitBlockFlavor)) {
                return unPackAsCode();
            }

        } else {
            return "Flavor Not Supported";
        }
        return null;
    }


    public String unPackAsCode() {
        String passBack = "";

        passBack += "ask " + plural() + " [\n";
        for (CodeBlock block : myBlocks) {
            passBack += block.unPackAsCode();
        }
        passBack += "]\n";

        return passBack;
    }

    public void makeLabel() {
        JPanel label = new JPanel();
        // TODO: This is a hard coded hack for now. Fix it.
        label.add(removeButton);
        label.add(new JLabel("Ask"));

        // number of turtles of a breed starting with -a.
        number = new PrettyInput(this);
        number.setText("100");
        label.add(number);

        plural = new PrettyInput(this);
        plural.setText(getName());
        label.add(plural);

        //label.add(new JLabel(" ("));
        label.add(makeBreedShapeButton());
        //label.add(new JLabel(") to..."));


        label.setBackground(getBackground());
        add(label);
    }


    private final javax.swing.Action pickBreedShape =
            new javax.swing.AbstractAction() {
                public void actionPerformed(java.awt.event.ActionEvent e) {

                }
            };

    public JButton makeBreedShapeButton() {
        breedShapeButton = new JButton(new ShapeIcon(org.nlogo.shape.VectorShape.getDefaultShape()));
        breedShapeButton.setActionCommand(this.getName());
        breedShapeButton.addActionListener(this);
        breedShapeButton.setSize(40, 40);
        return breedShapeButton;
    }

    // when clicks on shape selection -a.
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        ShapeSelector myShapeSelector = new ShapeSelector(parentFrame, allShapes(), this);
        myShapeSelector.setVisible(true);
        breedShapeButton.setIcon(new ShapeIcon(myShapeSelector.getShape()));
        breedShape = myShapeSelector.getChosenShape();
    }

    // getting shapes from NL -a.
    String[] allShapes() {
        String[] defaultShapes =
                org.nlogo.util.Utils.getResourceAsStringArray
                        ("/system/defaultShapes.txt");
        String[] libraryShapes =
                org.nlogo.util.Utils.getResourceAsStringArray
                        ("/system/libraryShapes.txt");
        String[] mergedShapes =
                new String[defaultShapes.length + 1 + libraryShapes.length];
        System.arraycopy(defaultShapes, 0,
                mergedShapes, 0,
                defaultShapes.length);
        mergedShapes[defaultShapes.length] = "";
        System.arraycopy(libraryShapes, 0,
                mergedShapes, defaultShapes.length + 1,
                libraryShapes.length);
        return defaultShapes; // NOTE right now just doing default
    }

    public java.util.List<Shape> parseShapes(String[] shapes, String version) {
        return org.nlogo.shape.VectorShape.parseShapes(shapes, version);
    }



    public String setBreedShape() {
        if (breedShape != null) {
            return "set-default-shape " + plural() + " \"" + breedShape + "\"\n";
        }
        return "";
    }


    public Breed myBreed() {
        return breed;
    }

    // not used -A. (Aug 10, 2012)
    public void addTraittoBreed(TraitBlock traitBlock) {
        traitBlock.showColorButton();
        traitBlock.doLayout();
        traitBlock.validate();
        traitBlock.repaint();
    }


    public void mouseEnter(MouseEvent evt) {
    }

    public void mouseExit(MouseEvent evt) {
    }

    public void mouseEntered(MouseEvent evt) {
    }

    public void mouseExited(MouseEvent evt) {
    }

    public void mouseClicked(MouseEvent evt) {
    }

    public void mouseMoved(MouseEvent evt) {
    }

    public void mouseReleased(MouseEvent evt) {
    }

    int beforeDragX;
    int beforeDragY;

    int beforeDragXLoc;
    int beforeDragYLoc;


    public void mousePressed(java.awt.event.MouseEvent evt) {
        Point point = evt.getPoint();
        javax.swing.SwingUtilities.convertPointToScreen(point, this);
        beforeDragX = point.x;
        beforeDragY = point.y;
        beforeDragXLoc = getLocation().x;
        beforeDragYLoc = getLocation().y;
    }


    public void mouseDragged(java.awt.event.MouseEvent evt) {
        Point point = evt.getPoint();
        javax.swing.SwingUtilities.convertPointToScreen(point, this);
        this.setLocation(
                point.x - beforeDragX + beforeDragXLoc,
                point.y - beforeDragY + beforeDragYLoc);
    }

    public void repaint() {
        if (parentFrame != null) {
            parentFrame.repaint();
        }
        super.repaint();
    }

    public void removeTraitBlock(TraitBlock traitBlock) {
        remove(traitBlock);
    }
}

