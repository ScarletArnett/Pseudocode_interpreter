package neige.GUI;

import neige.Stdlib;
import neige.lang.*;
import neige.lang.expr.Expression;
import neige.lang.expr.literal.FunExpression;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Map;


/**
 * Created by Hawk on 18/03/2015.
 */

public class Editeur extends JFrame implements ActionListener {

    ArrayList<String> alVar = new ArrayList<>();
    ArrayList<String> resList = new ArrayList<>();


    JEditorPane edit;
    JTextArea t_exec, data;
    JFrame data_frame;
    JMenuItem load, save, exec, stop, restart, newF;
    Border border = BorderFactory.createLineBorder(Color.BLACK, 1);

    int total_row;
    String path;
    String save_path;
    String res = "";
    String[] lines;
    String data_text = "DATA > ";
    String exec_text;
    Parser parser = new InfixParser();

    Stdin in  = new Stdin();
    Stdout out = new Stdout();


    class Stdin extends InputStream {
        private String line;
        private int cur;

        @Override
        public int read() throws IOException {
            if (line == null) {
                load();
                cur = 0;
            } else if (cur >= line.length()) {
                out.write('\n');
                line = null;
                return -1;
            }
            return line.charAt(cur++);
        }

        void load() {
            line = JOptionPane.showInputDialog(getContentPane(), out.removeLastLine(), null) + '\n';
        }
    }

    class Stdout extends OutputStream {
        StringBuilder buf = new StringBuilder();

        @Override
        public void write(int b) throws IOException {
            buf.append((char) b);
            if (b == '\n') {
                t_exec.setText(buf.toString());
            }
        }

        String removeLastLine() {
            int start = buf.lastIndexOf("\n") + 1;
            String last = buf.substring(start);
            buf.delete(start, buf.length());
            return last;
        }
    }



    public Editeur() {
        edit = new JEditorPane();
        edit.setEditorKit(new NumberedEditorKit());

        JScrollPane scroll = new JScrollPane(edit);
        getContentPane().add(scroll);

        setSize(1000, 700);

        addMenu();
        createDataFrame();


        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String a[]) {
        new Editeur();
    }

    private void addMenu() {

        JMenuBar menuBar = new JMenuBar();
        JMenu menuSave   = new JMenu("File");
        JMenu menuEdit   = new JMenu("Edition");
        JMenu menuExec   = new JMenu("Run");


        load  = new JMenuItem("Load");
        save  = new JMenuItem("Save");
        newF  = new JMenuItem("New");

        exec    = new JMenuItem("Run");
        stop    = new JMenuItem("Stop");
        restart = new JMenuItem("Restart");


        menuSave.add(load);
        menuSave.addSeparator();
        menuSave.add(save);
        menuSave.addSeparator();
        menuSave.add(newF);

        menuExec.add(exec);
        menuExec.addSeparator();
        menuExec.add(stop);
        menuExec.addSeparator();
        menuExec.add(restart);


        load.addActionListener(this);
        save.addActionListener(this);
        exec.addActionListener(this);
        newF.addActionListener(this);
        restart.addActionListener(this);


        menuBar.add(menuSave);
        menuBar.add(menuEdit);
        menuBar.add(menuExec);
        setJMenuBar(menuBar);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == save) {
            saveActionPerformed();
        }

        if (e.getSource() == load) try {
            openActionPerformed();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        if (e.getSource() == exec){
            runActionPerformed();
        }

        if (e.getSource() == newF ) {
            newFile();
        }

        if (e.getSource() == restart) {
            data.setText(data_text);
            t_exec.setText("");
            repaint();
            runActionPerformed();
        }
    }

    private void newFile(){
        alVar.clear();
        edit.setText("");
        data.setText(data_text);
        t_exec.setText("");
        repaint();
    }

    private void runActionPerformed(){
        InterpContext ctx = new HashInterpContext(MathContext.DECIMAL128, in, new PrintStream(out), new PrintStream(out), false);
        Stdlib.load(ctx);

        out.buf.setLength(0);
        t_exec.setText("");

        try {
            Expression exp = parser.parse(edit.getText());
            Expression result = exp.evaluate(ctx);
            System.out.println(parser.show(result));
        } catch (ParseException | NeigeRuntimeException e) {
            System.out.println(Throwables.toString(e));
        }

        saveVariables(ctx);
        writeVariables();
    }

    private void saveActionPerformed() {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            System.out.println("Save as file: " + fileToSave.getAbsolutePath());
            save_path = fileToSave.getAbsolutePath();
        }

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(save_path, "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert writer != null;
        lines = edit.getText().split(System.getProperty("line.separator"));
        for (String s : lines) {
            writer.write(s);
            writer.write("\n");
        }
        writer.close();
        writer.flush();
    }

    private void openActionPerformed() throws IOException {

        res = "";
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            path = file.getAbsolutePath();
            try {
                edit.setPage(file.toURI().toURL());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        BufferedReader br = new BufferedReader(new InputStreamReader(new UnicodeBOMInputStream(new FileInputStream(path)).skipBOM(), "UTF-8"));
        String sCurrentLine;

        while ((sCurrentLine = br.readLine()) != null) {
            resList.add(sCurrentLine);
            res += sCurrentLine + '\n';
            total_row++;
        }

        br.close();
        edit.setText(res);
    }


    public void createDataFrame() {
        data_frame = new JFrame();
        data_frame.setLayout(new GridLayout(2, 1));
        addData();
        addExec();
        data_frame.setBounds(1000, 0, 800, 500);
        data_frame.setTitle("Data & Execution trace");
        data_frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        data_frame.setVisible(true);
    }

    private void addExec() {
        t_exec = new JTextArea();
        t_exec.setBackground(Color.WHITE);
        t_exec.setText(exec_text);
        t_exec.setBorder(border);
        t_exec.setEditable(false);
        data_frame.add(t_exec);
    }

    private void addData() {
        data = new JTextArea();
        data.setBackground(Color.WHITE);
        data.setText(data_text);
        data.setLineWrap(true);
        data.setBorder(border);
        data.setRows(5);
        data.setEditable(false);
        data_frame.add(data);
    }

    public void saveVariables(InterpContext ctx) {

        alVar.clear();
        for (Map.Entry<String, Expression> entry : ctx.entrySet()) {
            if (entry.getValue() instanceof FunExpression) {
                continue;
            }
            if (entry.getKey().equals("pi")) {
                continue;
            }
            String name = entry.getKey();
            String var  = parser.show(entry.getValue());
            alVar.add(name + " = " + var);
        }
    }

    public void writeVariables(){

        String tmp = data_text;

        for( int i = alVar.size() ; i > 0 ; i-- ) {
            tmp += "\n" + " > " + alVar.get(i - 1);
            data.setText(tmp);
            data.repaint();
        }
    }
}

class NumberedEditorKit extends StyledEditorKit {
    public ViewFactory getViewFactory() {
        return new NumberedViewFactory();
    }
}

class NumberedViewFactory implements ViewFactory {
    public View create(Element elem) {
        String kind = elem.getName();
        if (kind != null)
            if (kind.equals(AbstractDocument.ContentElementName)) {
                return new LabelView(elem);
            }
            else if (kind.equals(AbstractDocument.
                    ParagraphElementName)) {
//              return new ParagraphView(elem);
                return new NumberedParagraphView(elem);
            }
            else if (kind.equals(AbstractDocument.
                    SectionElementName)) {
                return new BoxView(elem, View.Y_AXIS);
            }
            else if (kind.equals(StyleConstants.
                    ComponentElementName)) {
                return new ComponentView(elem);
            }
            else if (kind.equals(StyleConstants.IconElementName)) {
                return new IconView(elem);
            }
        // default to text display
        return new LabelView(elem);
    }
}

class NumberedParagraphView extends ParagraphView {
    public static short NUMBERS_WIDTH=25;

    public NumberedParagraphView(Element e) {
        super(e);
        short top = 0;
        short left = 0;
        short bottom = 0;
        short right = 0;
        this.setInsets(top, left, bottom, right);
    }

    protected void setInsets(short top, short left, short bottom,
                             short right) {super.setInsets
            (top,(short)(left+NUMBERS_WIDTH),
                    bottom,right);
    }

    public void paintChild(Graphics g, Rectangle r, int n) {
        super.paintChild(g, r, n);
        int previousLineCount = getPreviousLineCount();
        int numberX = r.x - getLeftInset();
        int numberY = r.y + r.height - 5;
        g.drawString(Integer.toString(previousLineCount + n + 1), numberX, numberY);
    }

    public int getPreviousLineCount() {
        int lineCount = 0;
        View parent = this.getParent();
        int count = parent.getViewCount();
        for (int i = 0; i < count; i++) {
            if (parent.getView(i) == this) {
                break;
            }
            else {
                lineCount += parent.getView(i).getViewCount();
            }
        }
        return lineCount;
    }
}
