package cma;

import java.util.List;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/** JavaFX UI: санах ойн layout дүрслэл + удирдлага. */
public class Main extends Application {

    private MemoryManager mm;
    private Canvas canvas;
    private TextArea log;
    private Label stats, error;
    private TextField totalField, nameField, sizeField, cmdField;
    private ComboBox<String> strategy;
    private VBox legendBox;
    private final Tooltip tip = new Tooltip();
    private final java.util.Map<String, Color> colors = new java.util.HashMap<>(); // process -> өнгө

    /** Сүүлийн RQ-ээр байрласан process (хүрээгээр тэмдэглэнэ), байхгүй бол null. */
    private String highlighted;
    /** Сүүлийн C-ийн өмнөх төлөв (өмнө/дараа зэрэгцүүлэхэд), бусад үед null. */
    private List<MemoryManager.Block> beforeCompaction;

    // Зурагдсан баганын байрлал (hover үед блок олоход)
    private double barTop, barH, barX, barW;

    @Override
    public void start(Stage stage) {
        totalField = new TextField("1048576");
        Button initBtn = new Button("Эхлүүлэх / Reset");
        initBtn.setMaxWidth(Double.MAX_VALUE);
        initBtn.setOnAction(e -> initMemory());

        nameField = new TextField("P0");
        sizeField = new TextField("40000");
        strategy = new ComboBox<>();
        strategy.getItems().addAll("F - First fit", "B - Best fit", "W - Worst fit");
        strategy.getSelectionModel().selectFirst();
        strategy.setMaxWidth(Double.MAX_VALUE);

        Button rq = new Button("Хүсэх (RQ)");
        rq.setOnAction(e -> apply("RQ " + nameField.getText().trim() + " " + sizeField.getText().trim()
            + " " + strategy.getValue().charAt(0)));
        Button rl = new Button("Чөлөөлөх (RL)");
        rl.setOnAction(e -> apply("RL " + nameField.getText().trim()));
        Button cp = new Button("Compaction (C)");
        cp.setOnAction(e -> apply("C"));
        Button st = new Button("STAT");
        st.setOnAction(e -> apply("STAT"));
        HBox row1 = new HBox(6, rq, rl);
        HBox row2 = new HBox(6, cp, st);

        cmdField = new TextField();
        cmdField.setPromptText("жнь: RQ P1 20000 B");
        cmdField.setOnAction(e -> { apply(cmdField.getText()); cmdField.clear(); });

        error = new Label();
        error.setTextFill(Color.CRIMSON);
        error.setWrapText(true);

        VBox controls = new VBox(8,
            new Label("Санах ойн хэмжээ (байт):"), totalField, initBtn, new Separator(),
            new Label("Process нэр (том/жижиг үсэг ялгана):"), nameField,
            new Label("Хэмжээ (байт):"), sizeField,
            new Label("Strategy:"), strategy, row1, row2, new Separator(),
            new Label("Команд (RQ/RL/C/STAT/STAT -v/X):"), cmdField, error);
        controls.setPadding(new Insets(10));
        controls.setPrefWidth(250);

        canvas = new Canvas(400, 500);
        Pane holder = new Pane(canvas);
        canvas.widthProperty().bind(holder.widthProperty());
        canvas.heightProperty().bind(holder.heightProperty());
        canvas.widthProperty().addListener(o -> redraw());
        canvas.heightProperty().addListener(o -> redraw());
        // Жижиг блокийн нэр харагдахгүй үед хулганаа очиход мэдээлэл гарна
        canvas.setOnMouseMoved(e -> showBlockTip(e.getX(), e.getY(), e.getScreenX(), e.getScreenY()));
        canvas.setOnMouseExited(e -> tip.hide());

        // Legend: process олон болоход гүйлгэнэ
        legendBox = new VBox(6);
        legendBox.setPadding(new Insets(10));
        ScrollPane legendScroll = new ScrollPane(legendBox);
        legendScroll.setFitToWidth(true);
        legendScroll.setPrefWidth(200);

        stats = new Label();
        stats.setPadding(new Insets(6));
        stats.setWrapText(true);

        log = new TextArea();
        log.setEditable(false);
        log.setPrefRowCount(8);

        BorderPane center = new BorderPane(holder);
        center.setTop(stats);
        center.setRight(legendScroll);
        BorderPane root = new BorderPane(center);
        root.setLeft(controls);
        root.setBottom(log);

        initMemory();
        stage.setTitle("Contiguous Memory Allocation (IV-2-14)");
        stage.setScene(new Scene(root, 1080, 720));
        stage.show();
    }

    private void initMemory() {
        try {
            // Cli-тэй ижил шалгалт, ижил алдааны текст
            mm = new MemoryManager(MemoryManager.parseMemorySize(totalField.getText()));
            colors.clear();
            highlighted = null;
            beforeCompaction = null;
            error.setText("");
            log.appendText("INIT: санах ой = " + mm.total() + " байт\n");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            if (mm == null) mm = new MemoryManager(1048576);
        }
        redraw();
    }

    private void apply(String cmd) {
        if (cmd != null && cmd.trim().equalsIgnoreCase("X")) { // X: програмаас гарах
            javafx.application.Platform.exit();
            return;
        }
        List<MemoryManager.Block> before = mm.snapshot();
        try {
            String res = mm.execute(cmd);
            error.setText("");
            log.appendText("> " + cmd + "\n" + res + "\n");
            String op = cmd.trim().split("\\s+")[0].toUpperCase();
            highlighted = null;
            beforeCompaction = null;
            if (op.equals("RQ")) {
                String[] t = cmd.trim().split("\\s+");
                highlighted = t[1];
                MemoryManager.Block h = mm.lastChosenHole();
                log.appendText("  " + strategyReason(t[3]) + ": hole [" + h.start() + ":" + h.end()
                    + "] (" + h.size() + " байт) сонгосон\n");
            } else if (op.equals("C")) {
                beforeCompaction = before; // өмнө/дараа зэрэгцүүлж харуулна
            }
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            log.appendText("> " + cmd + "\nERROR: " + e.getMessage() + "\n");
        }
        redraw();
    }

    private static String strategyReason(String s) {
        switch (s.toUpperCase()) {
            case "F": return "First fit — хаяг 0-ээс эхлэн хамгийн эхний багтах";
            case "B": return "Best fit — багтах hole-уудаас хамгийн жижиг";
            default:  return "Worst fit — багтах hole-уудаас хамгийн том";
        }
    }

    private void showError(String msg) { error.setText("Алдаа: " + msg); }

    // Process бүрт өөр өнгө: орж ирсэн дарааллаар golden-angle hue өгнө
    private Color colorOf(String name) {
        return colors.computeIfAbsent(name, n -> Color.hsb((colors.size() * 137.5) % 360, 0.55, 0.85));
    }

    /** Memory layout: address 0 доор, өндөр хаяг дээр (номын Figure 9.7-той адил). */
    private void redraw() {
        if (mm == null || canvas == null) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        g.setFont(Font.font(12));
        g.setTextBaseline(VPos.CENTER);

        double top = 30, bottom = h - 15;
        barTop = top;
        barH = bottom - top;
        barX = 110;
        if (beforeCompaction != null) {
            // C-ийн өмнө ба дараах төлөвийг зэрэгцүүлнэ
            barW = Math.max(60, Math.min((w - 150) / 2, 220));
            double prevX = barX + barW + 30;
            drawBar(g, mm.snapshot(), barX, barW, true);
            drawBar(g, beforeCompaction, prevX, barW, false);
            g.setFill(Color.BLACK);
            g.fillText("C-ийн дараа", barX, top - 14);
            g.fillText("C-ийн өмнө", prevX, top - 14);
        } else {
            barW = Math.max(60, Math.min(w - 140, 300));
            drawBar(g, mm.snapshot(), barX, barW, true);
        }
        g.setFill(Color.BLACK);
        g.fillText(String.valueOf(mm.total()), 6, top);

        updateLegend();
        stats.setText(mm.summary());
    }

    /** Нэг санах ойн багана зурна. withAddresses = зүүн талд хаяг бичих эсэх. */
    private void drawBar(GraphicsContext g, List<MemoryManager.Block> blocks, double x, double bw, boolean withAddresses) {
        for (MemoryManager.Block b : blocks) {
            double yTop = barTop + barH * (1.0 - (double) (b.start() + b.size()) / mm.total());
            double bh = barH * b.size() / mm.total();
            g.setFill(b.isHole() ? Color.LIGHTGRAY : colorOf(b.owner()));
            g.fillRect(x, yTop, bw, bh);
            g.setStroke(Color.BLACK);
            g.setLineWidth(1);
            g.strokeRect(x, yTop, bw, bh);
            g.setFill(Color.BLACK);
            if (bh >= 14) {
                g.fillText((b.isHole() ? "hole" : b.owner()) + " (" + b.size() + ")", x + 6, yTop + bh / 2);
            }
            if (withAddresses && bh >= 10) g.fillText(String.valueOf(b.start()), 6, yTop + bh); // эхлэх хаяг
        }
        // Сүүлийн RQ-ийн сонгосон hole-ыг улаан хүрээгээр тэмдэглэнэ
        if (withAddresses && highlighted != null && mm.lastChosenHole() != null) {
            MemoryManager.Block hole = mm.lastChosenHole();
            double yTop = barTop + barH * (1.0 - (double) (hole.start() + hole.size()) / mm.total());
            double bh = Math.max(3, barH * hole.size() / mm.total());
            g.setStroke(Color.RED);
            g.setLineWidth(3);
            g.strokeRect(x - 2, yTop - 1, bw + 4, bh + 2);
            g.setLineWidth(1);
        }
    }

    /** Legend: hole + одоо байгаа process бүр (олон бол ScrollPane гүйлгэнэ). */
    private void updateLegend() {
        legendBox.getChildren().clear();
        legendBox.getChildren().add(legendRow(Color.LIGHTGRAY, "Hole (чөлөөт)"));
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (MemoryManager.Block b : mm.snapshot()) if (!b.isHole()) seen.add(b.owner());
        for (String p : seen) legendBox.getChildren().add(legendRow(colorOf(p), "Process " + p)); // байгаа process бүрийн өнгө legend-д
        Label note = new Label("Доод = хаяг 0, дээд = өндөр хаяг.\nУлаан хүрээ = сүүлийн RQ-ийн сонгосон hole.\nЖижиг блок дээр хулганаа аваачвал нэр гарна.");
        note.setWrapText(true);
        legendBox.getChildren().add(note);
    }

    private static HBox legendRow(Color c, String text) {
        Rectangle r = new Rectangle(16, 16, c);
        r.setStroke(Color.BLACK);
        return new HBox(8, r, new Label(text));
    }

    /** Хулганы доорх блокийн нэр, хаяг, хэмжээг tooltip-оор харуулна. */
    private void showBlockTip(double x, double y, double sx, double sy) {
        if (mm == null || x < barX || x > barX + barW || y < barTop || y > barTop + barH) { tip.hide(); return; }
        double addr = (1.0 - (y - barTop) / barH) * mm.total();
        for (MemoryManager.Block b : mm.snapshot()) {
            if (addr >= b.start() && addr < b.start() + b.size()) {
                tip.setText((b.isHole() ? "Hole" : "Process " + b.owner())
                    + "\n[" + b.start() + ":" + b.end() + "]\n" + b.size() + " байт");
                tip.show(canvas, sx + 12, sy + 12);
                return;
            }
        }
        tip.hide();
    }

    public static void main(String[] args) { launch(args); }
}
