package com.glasiem.app;

import com.glasiem.main.GrammarLexer;
import com.glasiem.main.GrammarParser;
import com.glasiem.main.ThrowingErrorListener;
import com.glasiem.main.VisitorClass;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashSet;

class RowHeaderRenderer extends JLabel implements ListCellRenderer {

    RowHeaderRenderer(JTable table) {
        JTableHeader header = table.getTableHeader();
        setOpaque(true);
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        setHorizontalAlignment(CENTER);
        setForeground(header.getForeground());
        setBackground(header.getBackground());
        setFont(header.getFont());
    }

    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        setText((value == null) ? "" : value.toString());
        return this;
    }
}

public class App extends JFrame
{
    FileInputStream in = null;
    FileOutputStream out = null;
    private DefaultTableModel tableModel;
    private JLabel label;
    private JScrollPane scroll;
    private JTextField formula;
    private JTable table1;
    private JButton calculate;
    private JButton close;
    private JButton window1;
    private JButton window2;
    private JButton save;
    private JButton load;
    private Object[][] otherSide = new String[][] {
            {"0", "1", "2", "3", "4"},
            {"1", "2", "3", "4", "5"},
            {"2", "3", "4", "5", "6"},
            {"3", "4", "5", "6", "7"},
            {"4", "5", "6", "7", "8"}
    };

    public String link(String expression, HashSet<String> set) throws Exception {
        char c;
        String temp = "";
        int i = 0;
        do{
            c = expression.charAt(i);
            if (c == '#') {
                String cell = "#" + expression.charAt(i + 1) + expression.charAt(i + 2);
                if (!set.contains(cell)) {
                    set.add(cell);
                    int row = Character.getNumericValue(expression.charAt(i + 2)) - 1;
                    int column = (expression.charAt(i + 1) - 'A');
                    String toEvaluate = link((String) otherSide[row][column], set);
                    temp += evaluate(toEvaluate);
                    i += 3;
                    set.remove(cell);
                }
                else
                {
                    temp = "ERROR";
                    return temp;
                }
            }
            else
            {
                temp += c;
                i++;
            }
        }
        while(i < expression.length());
        return temp;
    }

    public static double evaluate(String expression) {
        GrammarLexer lexer = new GrammarLexer(new ANTLRInputStream(expression));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener());
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GrammarParser parser = new GrammarParser(tokenStream);
        ParseTree tree = parser.expression();
        VisitorClass visitor = new VisitorClass();
        return visitor.visit(tree);
    }


    public App() {
        int[] selectedCell = new int[]{0, 0};
        ListModel lm = new AbstractListModel() {
            String headers[] = { "1", "2", "3", "4", "5"};

            public int getSize() {
                return headers.length;
            }

            public Object getElementAt(int index) {
                return headers[index];
            }
        };
        label = new JLabel();
        label.setText("Select cell");
        tableModel = new DefaultTableModel(5,5);
        for (int i = 0; i <  5; i++) {
            for (int j = 0; j < 5; j++) {
                tableModel.setValueAt(String.valueOf(i+j)+".0",i,j);
            }
        }
        close = new JButton("Close");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        formula = new JTextField();
        MouseListener tableMouseListener = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table1.columnAtPoint(e.getPoint());
                int row = table1.rowAtPoint(e.getPoint());
                selectedCell[0] = row;
                selectedCell[1] = col;
                //JOptionPane.showMessageDialog(null,selectedCell[0]+"+"+selectedCell[1]);
                formula.setText((String) otherSide[selectedCell[0]][selectedCell[1]]);
                label.setText("Selected cell is " + (char)(Integer.valueOf('A') + col) + String.valueOf(row + 1));
            }
        };
        calculate = new JButton("Calculate");
        calculate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String temp = formula.getText();
                    otherSide[selectedCell[0]][selectedCell[1]] = temp;
                    String cell = String.valueOf('A' + selectedCell[1]) + String.valueOf(selectedCell[0]);
                    HashSet<String> set = new HashSet<>();
                    set.add(cell);
                    temp = link(temp,set);
                    tableModel.setValueAt(evaluate(temp),selectedCell[0],selectedCell[1]);
                    set.remove(cell);
                } catch (Exception ex) {
                    tableModel.setValueAt("ERROR",selectedCell[0],selectedCell[1]);
                }
            }
        });
        window1 = new JButton("Table");
        window1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window2 = new JButton("Menu");
                window2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Box contents = new Box(BoxLayout.Y_AXIS);
                        contents.add(close);
                        contents.add(load);
                        contents.add(save);
                        contents.add(window1);
                        setContentPane(contents);
                        setSize(500, 400);
                        setVisible(true);
                    }
                });
                Box contents1 = new Box(BoxLayout.Y_AXIS);
                contents1.add(close);
                contents1.add(window2);
                contents1.add(scroll);
                contents1.add(label);
                contents1.add(formula);
                contents1.add(calculate);
                setContentPane(contents1);
                setSize(500, 400);
                setVisible(true);
            }
        });
        load = new JButton("Load");
        load.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String fileName = JOptionPane.showInputDialog("Enter file name");
                    File file = new File(fileName);
                    //JOptionPane.showMessageDialog(null,file.exists());
                    if(file.exists()){
                        in = new FileInputStream(file);
                        BufferedReader bi = new BufferedReader(new InputStreamReader(in));
                        int rows = Integer.parseInt(bi.readLine());
                        int columns = Integer.parseInt(bi.readLine());
                        JOptionPane.showMessageDialog(null,rows + columns);
                        for (int i = 0; i < rows; i++) {
                            for (int j = 0; j < columns; j++) {
                                otherSide[i][j] = bi.readLine();
                            }
                        }
                        for (int i = 0; i < rows ; i++) {
                            for (int j = 0; j < columns; j++) {
                                try {
                                    String temp = String.valueOf(otherSide[i][j]);
                                    String cell = String.valueOf('A' + j) + String.valueOf(i + 1);
                                    HashSet<String> set = new HashSet<>();
                                    set.add(cell);
                                    temp = link(temp,set);
                                    tableModel.setValueAt(evaluate(temp),i,j);
                                    set.remove(cell);
                                } catch (Exception ex) {
                                    tableModel.setValueAt("ERROR",i,j);
                                }
                            }
                        }
                    }
                    else throw new Exception();
                }
                catch (Exception ex)
                {
                    JOptionPane.showMessageDialog(null,"ERROR: WRONG FILE NAME");
                }
                try {
                    in.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        save = new JButton("Save");
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String fileName = JOptionPane.showInputDialog("Enter file name");
                    String temp = fileName.substring(fileName.length() - 4);
                    //JOptionPane.showMessageDialog(null,temp + temp.length());
                    if (temp.equals(".txt")){
                        temp = "";
                        out = new FileOutputStream(fileName);
                        BufferedWriter bo = new BufferedWriter(new OutputStreamWriter(out));
                        bo.write(String.valueOf(tableModel.getRowCount()));
                        bo.newLine();
                        bo.write(String.valueOf(tableModel.getColumnCount()));
                        bo.newLine();
                        for (int i = 0; i < tableModel.getRowCount() ; i++) {
                            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                                bo.write(String.valueOf(otherSide[i][j]));
                                bo.newLine();
                            }
                        }
                        bo.close();
                    }
                    else throw new Exception();
                }
                catch (Exception ex)
                {
                    JOptionPane.showMessageDialog(null,"ERROR: WRONG FILE NAME");
                }
            }
        });
        table1 = new JTable(tableModel);
        table1.addMouseListener(tableMouseListener);
        table1.setEnabled(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Box contents = new Box(BoxLayout.Y_AXIS);
        JList rowHeader = new JList(lm);
        rowHeader.setFixedCellWidth(50);
        rowHeader.setFixedCellHeight(table1.getRowHeight());
        rowHeader.setCellRenderer(new RowHeaderRenderer(table1));
        scroll=new JScrollPane(table1);
        scroll.setRowHeaderView(rowHeader);
        contents.add(close);
        contents.add(load);
        contents.add(save);
        contents.add(window1);
        setContentPane(contents);
        setSize(500, 400);
        setVisible(true);
    }
    public static void main(String[] args) {
        new App();
    }
}