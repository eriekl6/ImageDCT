package DCT;

import java.util.Arrays;

public class DCT {
    private static final int[][] QUANTIZATION_TABLE = new int[][]{
            {1, 1, 2, 4, 8, 16, 32, 64},
            {1, 1, 2, 4, 8, 16, 32, 64},
            {2, 2, 2, 4, 8, 16, 32, 64},
            {4, 4, 4, 4, 8, 16, 32, 64},
            {8, 8, 8, 8, 8, 16, 32, 64},
            {16, 16, 16, 16, 16, 16, 32, 64},
            {32, 32, 32, 32, 32, 32, 32, 64},
            {64, 64, 64, 64, 64, 64, 64, 64}
    };

    public static int[][] getDCTcoefficients(int[][] input_matrix) {
        int n = input_matrix[0].length;
        double[][] matrix = new double[n][n];
        double[][] dct = get_DCT_matrix(n);

        int i = 0;
        for (int[] row : input_matrix) {
            double[] new_row = Arrays.stream(row).asDoubleStream().toArray();
            matrix[i] = new_row;
            i++;
        }

        //Transform: Y = T X transpose(T)
        double[][] A = multiplyMatrices(dct, matrix);
        double[][] Y = multiplyMatrices(A, transpose(dct));

        int[][] res = new int[n][n];

        for (int j = 0; j < n; j++) {
            for (int k = 0; k < n; k++) {
                res[j][k] = (int) Math.round(Y[j][k]);
            }
        }
        return res;
    }

    public static int[][] getQuantizedCoefficients(int[][] DCT_coeff) {
        int n = DCT_coeff.length;
        int[][] res = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                res[i][j] = (int) Math.round(((double)DCT_coeff[i][j]) / ((double)QUANTIZATION_TABLE[i][j]));
            }
        }
        return res;
    }

    public static int[][] inverseDCTcoefficients(int[][] Y) {
        int n = Y.length;
        int[][] res = new int[n][n];

        double[][] T = get_DCT_matrix(n);

        double[][] Y_double = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Y_double[i][j] = Y[i][j];
            }
        }

        // Inverse transform
        // X = transpose(T)YT
        double[][] A = multiplyMatrices(transpose(T), Y_double);
        double[][] X = multiplyMatrices(A, T);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                res[i][j] = (int) Math.round(X[i][j]);
            }
        }
        return res;
    }

    public static int[][] inverseQuantization(int[][] quantized_coefficients) {
        int n = quantized_coefficients.length;
        int[][] dct = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                dct[i][j] = quantized_coefficients[i][j] * QUANTIZATION_TABLE[i][j];
            }
        }
        return dct;
    }

    private static double[][] get_DCT_matrix(int n) {
        double[][] dct = new double[n][n];
        double a;
        for (int i = 0; i < n; i++) {
            if (i == 0) {
                a = Math.sqrt(1.0/n);
            } else {
                a = Math.sqrt(2.0/n);
            }
            for (int j = 0; j < n; j++) {
                dct[i][j] = a*Math.cos(((2*j+1)*i*Math.PI)/(2*n));
            }
        }
        return dct;
    }

    private static double[][] transpose(double[][] A)
    {
        int N = A.length;
        double[][] B = new double[N][N];
        int i, j;
        for (i = 0; i < N; i++)
            for (j = 0; j < N; j++)
                B[i][j] = A[j][i];
        return B;
    }

    private static double[][] multiplyMatrices(double[][] firstMatrix, double[][] secondMatrix) {
        double[][] result = new double[firstMatrix.length][secondMatrix[0].length];

        for (int row = 0; row < result.length; row++) {
            for (int col = 0; col < result[row].length; col++) {
                result[row][col] = multiplyMatricesCell(firstMatrix, secondMatrix, row, col);
            }
        }

        return result;
    }

    private static double multiplyMatricesCell(double[][] firstMatrix, double[][] secondMatrix, int row, int col) {
        double cell = 0;
        for (int i = 0; i < secondMatrix.length; i++) {
            cell += firstMatrix[row][i] * secondMatrix[i][col];
        }
        return cell;
    }
}
