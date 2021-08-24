import DCT.DCT;
import helper.FileHelper;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main extends Application {
    private Button fileButton;
    private Label fileLabel;
    private FileChooser fileChooser;
    private File imageFile;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Open Image File");
        fileLabel = new Label("Select an image file to open");
        fileButton = new Button("Select Image File");
        fileButton.setStyle("-fx-background-color: rgba(77,77,77,0.75);" +
                "-fx-background-radius: 10;" +
                "-fx-font-size: 15;" +
                "-fx-text-fill: white; ");

        fileButton.setOnAction(actionEvent -> {
            fileChooser = new FileChooser();
            fileChooser.setTitle("Select image file");
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            if (!FileHelper.getFileExtension(selectedFile.getAbsolutePath()).equals("bmp")) {
                fileLabel.setText("Please choose a BMP file");
                System.out.println("Please choose a BMP file");
            } else {
                fileLabel.setText("File: " + selectedFile.getName());
                imageFile = selectedFile;
                Image image = compressAndRestoreImage(imageFile);
                displayImages(imageFile, image, primaryStage);
                saveImageFile(image, FilenameUtils.getBaseName(selectedFile.getName()));
            }
        });

        VBox vbox = new VBox(fileButton, fileLabel);
        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(15);
        vbox.setPadding(new Insets(25));
        Scene scene = new Scene(vbox, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    private void saveImageFile(Image image, String name) {
        File file = new File(name+"-ela57.png");
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            System.out.println("Saved image: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveQuantizationResults(int[][] quantizationMatrix, String filename) {
        File file = new File(filename);
        try {
            PrintWriter pw = new PrintWriter(file);
            for (int[] row : quantizationMatrix) {
                pw.println(Arrays.toString(row));
            }
            pw.close();
            System.out.println("Saved quantization result to: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Image compressAndRestoreImage(File imageFile) {
        Image origImage = null;
        try {
            origImage = new Image(new FileInputStream(imageFile.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            System.out.println("IMAGE NOT FOUND!");
            e.printStackTrace();
            System.exit(-1);
        }

        // block preparation
        // transform to YUV
        int width = (int) origImage.getWidth();
        int height = (int) origImage.getHeight();
        int[][] yuvImage_y = new int[height][width];
        int[][] yuvImage_u = new int[height][width];
        int[][] yuvImage_v = new int[height][width];
        for (int i = 0; i < width; i++) { // get counts of each rgb channel
            for (int j = 0; j < height; j++) {
                int pixel = origImage.getPixelReader().getArgb(i, j);
                int r = (pixel >> 16) & 0xFF; // shift and only take last 8 bits
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                int[] yuv = rgb2yuv(r, g, b);
                yuvImage_y[j][i] = yuv[0];
                yuvImage_u[j][i] = yuv[1];
                yuvImage_v[j][i] = yuv[2];
            }
        }

        List<Block> blocks_y = getBlocks(yuvImage_y, 8);
        List<Block> blocks_u = getBlocks(yuvImage_u, 8);
        List<Block> blocks_v = getBlocks(yuvImage_v, 8);

        //dct and quantization
        //then inverse
        for (Block block : blocks_y) {
            int[][] dct_coeff = DCT.getDCTcoefficients(block.getData());
            int[][] quantized_coeff = DCT.getQuantizedCoefficients(dct_coeff);
            block.setDct_coef(dct_coeff);
            block.setQuantized_coef(quantized_coeff);

            int[][] restored_dct = DCT.inverseQuantization(quantized_coeff); //inverse quantization
            int[][] restored_data = DCT.inverseDCTcoefficients(restored_dct); //inverse DCT transform
            block.setRestored_data_lossy(restored_data);
        }
        for (Block block : blocks_u) {
            int[][] dct_coeff = DCT.getDCTcoefficients(block.getData());
            int[][] quantized_coeff = DCT.getQuantizedCoefficients(dct_coeff);
            block.setDct_coef(dct_coeff);
            block.setQuantized_coef(quantized_coeff);

            int[][] restored_dct = DCT.inverseQuantization(quantized_coeff); //inverse quantization
            int[][] restored_data = DCT.inverseDCTcoefficients(restored_dct); //inverse DCT transform
            block.setRestored_data_lossy(restored_data);
        }
        for (Block block : blocks_v) {
            int[][] dct_coeff = DCT.getDCTcoefficients(block.getData());
            int[][] quantized_coeff = DCT.getQuantizedCoefficients(dct_coeff);
            block.setDct_coef(dct_coeff);
            block.setQuantized_coef(quantized_coeff);

            int[][] restored_dct = DCT.inverseQuantization(quantized_coeff); //inverse quantization
            int[][] restored_data = DCT.inverseDCTcoefficients(restored_dct); //inverse DCT transform
            block.setRestored_data_lossy(restored_data);
        }

        int[][] quantizationMatrix_y = reconstructQuantizationFromBlocks(blocks_y, width, height);
        int[][] quantizationMatrix_u = reconstructQuantizationFromBlocks(blocks_u, width, height);
        int[][] quantizationMatrix_v = reconstructQuantizationFromBlocks(blocks_v, width, height);

        //save quantization matrices to file
        saveQuantizationResults(quantizationMatrix_y, FilenameUtils.getBaseName(imageFile.getName())+"-ela57-quantization_y.txt");
        saveQuantizationResults(quantizationMatrix_u, FilenameUtils.getBaseName(imageFile.getName())+"-ela57-quantization_u.txt");
        saveQuantizationResults(quantizationMatrix_v, FilenameUtils.getBaseName(imageFile.getName())+"-ela57-quantization_v.txt");

        int[][] restored_yuvImage_y = reconstructMatrixFromBlocks(blocks_y, width, height);
        int[][] restored_yuvImage_u = reconstructMatrixFromBlocks(blocks_u, width, height);
        int[][] restored_yuvImage_v = reconstructMatrixFromBlocks(blocks_v, width, height);

        return combineYUV2RGB(restored_yuvImage_y, restored_yuvImage_u, restored_yuvImage_v);
    }

    private Image combineYUV2RGB(int[][] yuvImage_y, int[][] yuvImage_u, int[][] yuvImage_v) {
        int width = yuvImage_y[0].length;
        int height = yuvImage_y.length;
        WritableImage writeImage = new WritableImage(width, height);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int y = yuvImage_y[i][j];
                int u = yuvImage_u[i][j];
                int v = yuvImage_v[i][j];
                int[] rgb = yuv2rgb(y, u, v);
                int argb = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]; // rebuild the argb
                writeImage.getPixelWriter().setArgb(j,i,argb);
            }
        }
        return writeImage;
    }

    private int[][] reconstructMatrixFromBlocks(List<Block> blocks, int width, int height) {
        int[][] res = new int[height][width];

        for (Block block : blocks) {
            int x = block.getRow_index();
            int y = block.getCol_index();
            int[][] data = block.getRestored_lossy();
            int n = data.length;
            for (int i = x; i < x + n; i++) {
                for (int j = y; j < y + n; j++) {
                    res[i][j] = data[i-x][j-y];
                }
            }
        }

        return res;
    }

    private int[][] reconstructQuantizationFromBlocks(List<Block> blocks, int width, int height) {
        int[][] res = new int[height][width];

        for (Block block : blocks) {
            int x = block.getRow_index();
            int y = block.getCol_index();
            int[][] data = block.getQuantized_coef();
            int n = data.length;

            for (int i = x; i < x + n; i++) {
                for (int j = y; j < y + n; j++) {
                    res[i][j] = data[i-x][j-y];
                }
            }
        }

        return res;
    }

    private static List<Block> getBlocks(int[][] data, int n) {
        int height = data.length;
        int width = data[0].length;
        List<Block> blocks = new ArrayList<>();

        for (int i = 0; i <= height - n; i+=n) { // go through each row strip
            for (int j = 0; j <= width - n; j+=n) { // go through each column strip
                int[][] temp_block = new int[n][n];

                for (int k = 0; k < n; k++) { // row in the block
                    for (int l = 0; l < n; l++) { // column in the block
                        temp_block[k][l] = data[i+k][j+l];
                    }
                }
                blocks.add(new Block(i/n, j/n, i, j, temp_block));
            }
        }
        return blocks;
    }

    private static int[] yuv2rgb(int y, int u, int v) {
        int r = y + v;
        float g = (float)(y - 0.19421*u -0.50937*v);
        int b = y + u;
        if (r>255) r = 255;
        if (r<0) r = 0;

        if (g>255) g = 255;
        if (g<0) g = 0;

        if (b>255) b = 255;
        if (b<0) b = 0;

        return new int[]{r, Math.round(g), b};
    }


    public static int[] rgb2yuv(int r, int g, int b) {
        float y = (float)(0.299 * r + 0.587 * g + 0.114 * b);
        float u = (float)(-0.299 * r - 0.587 * g + 0.886 * b);
        float v = (float)(0.701 * r - 0.587 * g - 0.114 * b);
        return new int[]{Math.round(y), Math.round(u), Math.round(v)};
    }

    private void displayImages(File imageFile, Image image2, Window parent) {
        Stage subStage = new Stage();
        Image origImage = null;
        try {
            origImage = new Image(new FileInputStream(imageFile.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        ImageView imageView1 = new ImageView(origImage);
        imageView1.setPreserveRatio(true);
        Label label1 = new Label("Original");
//        label1.setTextFill(Paint.valueOf("0xffffff"));
        label1.setFont(Font.font("Cambria", 20));

        VBox vbox1 = new VBox(label1, imageView1);
        vbox1.setSpacing(10);
        vbox1.setAlignment(Pos.CENTER);

        ImageView imageView2 = new ImageView(image2);
        imageView2.setPreserveRatio(true);
        Label label2 = new Label("Quantized result");
//        label2.setTextFill(Paint.valueOf("0xffffff"));
        label2.setFont(Font.font("Cambria", 20));

        VBox vbox2 = new VBox(label2, imageView2);
        vbox2.setAlignment(Pos.CENTER);
        vbox2.setSpacing(10);

        HBox hbox = new HBox(vbox1, vbox2);
        hbox.setAlignment(Pos.CENTER);
        hbox.setSpacing(25);
        hbox.setPadding(new Insets(25));
//        hbox.setStyle("-fx-background-color: rgba(0,0,0,0.9);");

        Button quitButton = new Button("Quit");
        quitButton.setStyle("-fx-background-color: rgba(77,77,77,0.75);" +
                "-fx-background-radius: 10;" +
                "-fx-font-size: 15;" +
                "-fx-text-fill: white; ");
        quitButton.setOnAction(actionEvent -> {
            subStage.close();
        });

        VBox vbox = new VBox(hbox, quitButton);
        vbox.setSpacing(10);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        Scene imageScene = new Scene(vbox, 1650, 800);
        subStage.initModality(Modality.WINDOW_MODAL);
        subStage.initOwner(parent);
        subStage.setResizable(true);
        subStage.setScene(imageScene);
        subStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
