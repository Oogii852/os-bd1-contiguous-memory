package cma;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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
            new Label("Process нэр:"), nameField,
            new Label("Хэмжээ (байт):"), sizeField,
            new Label("Strategy:"), strategy, row1, row2, new Separator(),
            new Label("Команд (RQ/RL/C/STAT):"), cmdField, error);
        controls.setPadding(new Insets(10));
        controls.setPrefWidth(250);

        canvas = new Canvas(400, 500);
        Pane holder = new Pane(canvas);
        canvas.widthProperty().bind(holder.widthProperty());
        canvas.heightProperty().bind(holder.heightProperty());
        canvas.widthProperty().addListener(o -> redraw());
        canvas.heightProperty().addListener(o -> redraw());

        stats = new Label();
        stats.setPadding(new Insets(6));
        stats.setWrapText(true);

        log = new TextArea();
        log.setEditable(false);
        log.setPrefRowCount(8);

        BorderPane center = new BorderPane(holder);
        center.setTop(stats);
        BorderPane root = new BorderPane(center);
        root.setLeft(controls);
        root.setBottom(log);

        initMemory();
        stage.setTitle("Contiguous Memory Allocation (IV-2-14)");
        stage.setScene(new Scene(root, 980, 720));
        stage.show();
    }

    private void initMemory() {
        try {
            mm = new MemoryManager(Long.parseLong(totalField.getText().trim()));
            error.setText("");
            log.appendText("INIT: санах ой = " + mm.total() + " байт\n");
        } catch (NumberFormatException e) {
            showError("Санах ойн хэмжээ бүхэл тоо байх ёстой");
            if (mm == null) mm = new MemoryManager(1048576);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            if (mm == null) mm = new MemoryManager(1048576);
        }
        redraw();
    }

    private void apply(String cmd) {
        try {
            String res = mm.execute(cmd);
            error.setText("");
            log.appendText("> " + cmd + "\n" + res + "\n");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            log.appendText("> " + cmd + "\nERROR: " + e.getMessage() + "\n");
        }
        redraw();
    }

    private void showError(String msg) { error.setText("Алдаа: " + msg); }

    private static Color colorOf(String name) {
        return Color.hsb(Math.abs(name.hashCode()) % 360, 0.55, 0.85);
    }

    /** Memory layout: address 0 доор, өндөр хаяг дээр (номын Figure 9.7-той адил). */
    private void redraw() {
        if (mm == null || canvas == null) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        double top = 15, bottom = h - 15, barH = bottom - top, barX = 110, barW = Math.max(60, Math.min(w - 330, 260));
        g.setFont(Font.font(12));
        g.setTextBaseline(VPos.CENTER);

        for (MemoryManager.Block b : mm.snapshot()) {
            double yTop = top + barH * (1.0 - (double) (b.start() + b.size()) / mm.total());
            double bh = barH * b.size() / mm.total();
            g.setFill(b.isHole() ? Color.LIGHTGRAY : colorOf(b.owner()));
            g.fillRect(barX, yTop, barW, bh);
            g.setStroke(Color.BLACK);
            g.strokeRect(barX, yTop, barW, bh);
            g.setFill(Color.BLACK);
            if (bh >= 14) {
                g.fillText((b.isHole() ? "hole" : b.owner()) + " (" + b.size() + ")", barX + 6, yTop + bh / 2);
            }
            if (bh >= 10) g.fillText(String.valueOf(b.start()), 6, yTop + bh); // эхлэх хаяг
        }
        g.fillText(String.valueOf(mm.total()), 6, top);

        // Legend
        double lx = barX + barW + 20, ly = top + 10;
        g.setFill(Color.LIGHTGRAY); g.fillRect(lx, ly, 16, 16);
        g.setFill(Color.BLACK); g.fillText("Hole (чөлөөт)", lx + 24, ly + 8);
        g.setFill(Color.CORNFLOWERBLUE); g.fillRect(lx, ly + 24, 16, 16);
        g.setFill(Color.BLACK); g.fillText("Process (өнгө бүр өөр)", lx + 24, ly + 32);
        g.fillText("Доод = хаяг 0, дээд = өндөр хаяг", lx, ly + 58);
        g.fillText("Жижиг блокийн нэр харагдахгүй бол STAT дар", lx, ly + 76);

        long free = mm.free();
        double pct = free == 0 ? 0 : 100.0 * mm.externalFragmentation() / free;
        stats.setText(String.format(
            "Нийт: %d | Ашигласан: %d | Чөлөөт: %d | Hole: %d | Хамгийн том hole: %d | External fragmentation: %d байт (%.1f%%)",
            mm.total(), mm.used(), free, mm.holeCount(), mm.largestHole(), mm.externalFragmentation(), pct));
    }

    public static void main(String[] args) { launch(args); }
}
