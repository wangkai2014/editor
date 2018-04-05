package com.ggx.editor;

import com.ggx.editor.editor.MarkDownEditorPane;
import com.ggx.editor.interfaces.TreeListAction;
import com.ggx.editor.preview.MarkDownPreviewPane;
import com.ggx.editor.utils.FileUtil;
import com.ggx.editor.widget.TextFieldTreeCellImpl;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.transitions.hamburger.HamburgerBackArrowBasicTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.reactfx.EventStreams;

import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class MainController implements Initializable, TreeListAction {

    @FXML
    public BorderPane rootPane;
    @FXML
    public TreeView<File> treeView;
    @FXML
    public StackPane fileContainer;
    @FXML
    public SplitPane splitePane;
    @FXML
    public JFXHamburger jfxHamburger;
    @FXML
    public StackPane leftBtn;
    @FXML
    public Label title;
    @FXML
    public BorderPane leftPane;
    @FXML
    public BorderPane rightPane;
    @FXML
    public ToggleGroup toggle;
    @FXML
    public HBox toggleContainer;
    @FXML
    public MenuItem save;

    private final Image folderIcon = new Image(ClassLoader.getSystemResourceAsStream("icons/folder_16.png"));
    private final Image fileIcon = new Image(ClassLoader.getSystemResourceAsStream("icons/file_16.png"));

    private HamburgerBackArrowBasicTransition burgerTask3;

    private File currentFile;

    private MarkDownPreviewPane markDownPreview;
    private MarkDownEditorPane markDownEditorPane;


    public void setExecutor(ExecutorService executor){
        markDownEditorPane.setExecutor(executor);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        markDownPreview=new MarkDownPreviewPane();
        markDownEditorPane=new MarkDownEditorPane();

        treeView.setShowRoot(true);
        treeView.setEditable(true);
        treeView.setCellFactory(param -> new TextFieldTreeCellImpl(this));


        jfxHamburger.setMaxSize(20, 10);
        burgerTask3 = new HamburgerBackArrowBasicTransition(jfxHamburger);
        burgerTask3.setRate(-1);

        EventStreams.changesOf(rootPane.widthProperty()).subscribe(numberChange -> {
            splitePane.setDividerPosition(0, 0.18);
            if (rightPane.getCenter() != null) {
                markDownPreview.setWidth((rootPane.getWidth() - leftPane.getWidth()) / 2);
            } else {
                markDownPreview.setWidth(rootPane.getWidth() - leftPane.getWidth());
            }
        });
        EventStreams.changesOf(toggle.selectedToggleProperty()).subscribe(toggleChange -> {
            RadioButton rb = (RadioButton) toggleChange.getNewValue();
            switch (rb.getId()) {
                case "editor":
                    rightPane.setRight(null);
                    rightPane.setCenter(fileContainer);
                    break;
                case "eye":
                    rightPane.setCenter(null);
                    markDownPreview.setWidth(rootPane.getWidth() - leftPane.getWidth());
                    rightPane.setRight(markDownPreview.getPreviewNode());
                    break;
                case "realTime":
                    rightPane.setCenter(fileContainer);
                    markDownPreview.setWidth((rootPane.getWidth() - leftPane.getWidth()) / 2);
                    rightPane.setRight(markDownPreview.getPreviewNode());
                    break;
            }
        });

        markDownPreview.markdownTextProperty().bind(markDownEditorPane.markDownTextProperty());
        markDownPreview.markdownASTProperty().bind(markDownEditorPane.markDownASTProperty());
        markDownPreview.scrollYProperty().bind(markDownEditorPane.scrollYProperty());
        markDownPreview.editorSelectionProperty().bind(markDownEditorPane.selectionProperty());
    }

    private void searchFile(File fileOrDir, TreeItem<File> rootItem) {
        File[] list = fileOrDir.listFiles();
        if (list == null) {
            return;
        }
        Consumer<File> consumer = f -> {
            TreeItem<File> item = new TreeItem<>(f);
            if (f.isDirectory()) {
                ImageView iv = new ImageView(folderIcon);
                iv.setSmooth(true);
                iv.setViewport(new Rectangle2D(0, 0, 16, 16));
                item.setGraphic(iv);
                rootItem.getChildren().add(item);
                searchFile(f, item);
            } else {
                item.setGraphic(new ImageView(fileIcon));
                rootItem.getChildren().add(item);
            }
        };
        Arrays.stream(list).filter(f -> !f.isHidden() && f.isDirectory()).sorted().forEach(consumer);
        Arrays.stream(list).filter(f -> !f.isHidden() && f.isFile()).sorted().forEach(consumer);
    }

    @FXML
    public void doBack(MouseEvent mouseEvent) {
        burgerTask3.setRate(burgerTask3.getRate() * -1);
        burgerTask3.play();
        int size = splitePane.getItems().size();
        if (size > 1) {
            splitePane.setDividerPosition(0, 0);
            splitePane.getItems().remove(0);
        } else {
            splitePane.getItems().add(0, leftPane);
            splitePane.setDividerPosition(0, 0.2);
        }

    }


    @Override
    public void openFile(File file) {
        if(!file.exists()){
            return;
        }
        save.setDisable(false);
        currentFile = file;
        title.setText(FileUtil.prefixName(file) + " " + DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.CHINESE).format(file.lastModified()));
        if (file.getName().endsWith(".md")) {
            title.setGraphic(new ImageView(new Image(ClassLoader.getSystemResourceAsStream("icons/md_24.png"))));
        } else {
            title.setGraphic(new ImageView(new Image(ClassLoader.getSystemResourceAsStream("icons/txt_24.png"))));
        }
        BufferedReader br = null;
        try {
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new FileReader(file));
            br.lines().map(s -> s + "\n").forEach(sb::append);
            markDownEditorPane.setNewFileContent(sb.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (fileContainer.getChildren().size() == 2) {
            fileContainer.getChildren().remove(1);
        }
        fileContainer.getChildren().add(markDownEditorPane.getScrollPane());
        markDownEditorPane.getScrollPane().scrollYToPixel(0);
        toggleContainer.setVisible(true);

    }

    @Override
    public void deleteFile(File file) {
        if (currentFile == file) {
            title.setGraphic(null);
            title.setText(null);
            fileContainer.getChildren().remove(1);
            currentFile = null;
            save.setDisable(false);
        }
    }

    @Override
    public void deleteDir(TreeItem<File> item, File file) {
        // TODO: 2017/12/28 弹出Dialog 确认删除目录
        File[] list = file.listFiles();
        if (list == null) {
            return;
        }
        if (Arrays.stream(list).allMatch(f -> f.getAbsolutePath().equals(currentFile.getAbsolutePath()))) {
            title.setGraphic(null);
            title.setText(null);
            if (fileContainer.getChildren().size() == 2) {
                fileContainer.getChildren().remove(1);
            }
            currentFile = null;
            save.setDisable(true);
        }
        if (FileUtil.deleteDir(file)) {
            item.getParent().getChildren().remove(item);

        }
    }

    @Override
    public void modifyFile(File file) {
        currentFile = file;
        title.setText(FileUtil.prefixName(file) + " " + DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.CHINESE).format(file.lastModified()));
        if (file.getName().endsWith(".md")) {
            title.setGraphic(new ImageView(new Image(ClassLoader.getSystemResourceAsStream("icons/md_24.png"))));
        } else {
            title.setGraphic(new ImageView(new Image(ClassLoader.getSystemResourceAsStream("icons/txt_24.png"))));
        }
    }

    @FXML
    public void openDir() {
        DirectoryChooser chooser=new DirectoryChooser();
        File dir=chooser.showDialog(Main.get());
        if(dir!=null&&dir.exists()){
            //关闭面板
            if (fileContainer.getChildren().size() == 2) {
                fileContainer.getChildren().remove(1);
            }
            title.setText(null);
            title.setGraphic(null);
            currentFile=null;
            save.setDisable(true);
            toggleContainer.setVisible(false);
            ImageView iv = new ImageView(folderIcon);
            iv.setSmooth(true);
            iv.setViewport(new Rectangle2D(0, 0, 16, 16));
            TreeItem<File> rootTree = new TreeItem<>(dir, iv);
            searchFile(dir, rootTree);
            treeView.setRoot(rootTree);
        }

    }

    @FXML
    public void createDir() {
        FileChooser fileChooser=new FileChooser();
        fileChooser.setTitle("Save dir");
        File dir=fileChooser.showSaveDialog(Main.get());
        if(!dir.exists()){
            if(dir.mkdir()){
                System.out.println("创建成功");
                ImageView iv = new ImageView(folderIcon);
                iv.setSmooth(true);
                iv.setViewport(new Rectangle2D(0, 0, 16, 16));
                TreeItem<File> rootTree = new TreeItem<>(dir, iv);
                treeView.setShowRoot(true);
                treeView.setRoot(rootTree);
            }else {
                Alert error=new Alert(Alert.AlertType.ERROR);
                error.setContentText("工作空间创建失败.");
                error.initOwner(Main.get());
                error.show();
            }
        }
    }

    @FXML
    public void exitApp(ActionEvent actionEvent) {
        Main.get().close();
    }

    @FXML
    public void aboutAction(ActionEvent actionEvent) {
        System.out.println("about meun");
    }

    @FXML
    public void onSaveAction(ActionEvent actionEvent) {
        if(currentFile!=null&&currentFile.exists()){
            markDownEditorPane.saveFile(currentFile);
        }
    }
}
