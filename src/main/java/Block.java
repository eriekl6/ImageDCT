public class Block {
    private int x; // block row
    private int y; // block col
    private int row_index; // row index where block starts in original image
    private int col_index; // col index where block starts in original image
    private int[][] data; //original image data
    private int[][] dct_coef;
    private int[][] quantized_coef;
    private int[][] restored_lossy;

    public Block(int x, int y, int row_index, int col_index, int[][] data) {
        this.x = x;
        this.y = y;
        this.row_index = row_index;
        this.col_index = col_index;
        this.data = data;
    }

    public int getRow_index() {
        return row_index;
    }

    public int getCol_index() {
        return col_index;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int[][] getData() {
        return data;
    }

    public int[][] getDct_coef() {
        return dct_coef;
    }

    public void setDct_coef(int[][] dct_coef) {
        this.dct_coef = dct_coef;
    }

    public int[][] getQuantized_coef() {
        return quantized_coef;
    }

    public void setQuantized_coef(int[][] quantized_coef) {
        this.quantized_coef = quantized_coef;
    }

    public int[][] getRestored_lossy() {
        return restored_lossy;
    }

    public void setRestored_data_lossy(int[][] restored_lossy) {
        this.restored_lossy = restored_lossy;
    }
}
