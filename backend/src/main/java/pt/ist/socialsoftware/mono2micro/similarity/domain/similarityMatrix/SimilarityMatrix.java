package pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.annotation.Transient;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.Recommendation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimilarityMatrix {
    private static final int INTERVAL = 100;
    private static final int STEP = 10;
    public String name;
    private List<Weights> weightsList;
    @Transient
    private GridFsService gridFsService;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Weights> getWeightsList() {
        return weightsList;
    }

    public void setWeightsList(List<Weights> weightsList) {
        this.weightsList = weightsList;
    }

    public void setGridFsService(GridFsService gridFsService) {
        this.gridFsService = gridFsService;
    }

    public SimilarityMatrix() {}

    public SimilarityMatrix(List<Weights> weightsList) {
        this.weightsList = weightsList;
    }

    public SimilarityMatrix(String name, List<Weights> weightsList) {
        this.name = name;
        this.weightsList = weightsList;
    }

    private int getTotalNumberOfWeights() {
        return getWeightsList().stream().reduce(0, (totalNumberOfWeights, weights) -> totalNumberOfWeights + weights.getNumberOfWeights(), Integer::sum);
    }

    private Weights getWeightsByType(String type) {
        return getWeightsList().stream().filter(weight -> weight.getType().equals(type)).findFirst().orElse(null);
    }

    public boolean hasSameWeights(List<Weights> weightsList) {
        return weightsList.stream().allMatch(weights -> {
            Weights w = getWeightsByType(weights.getType());
            if (w == null)
                return false;
            return weights.equals(w);
        });
    }

    private float[] getWeightsAsArray() {
        float[] allWeightsAsArray = new float[getTotalNumberOfWeights()];
        int i = 0;

        for (Weights weights : getWeightsList()) {
            float[] weightsAsArray = weights.getWeights();
            for (float weight : weightsAsArray) allWeightsAsArray[i++] = weight;
        }
        return allWeightsAsArray;
    }

    public void generate(GridFsService gridFsService, Similarity similarity, Set<Short> elements) throws Exception {
        float[][][] rawMatrix = getEmptyRawMatrix(elements.size(), getTotalNumberOfWeights());
        setGridFsService(gridFsService);

        int fillFromIndex = 0;
        for (Weights weights : getWeightsList()) {
            weights.fillMatrix(gridFsService, similarity, rawMatrix, elements, fillFromIndex);
            fillFromIndex += weights.getNumberOfWeights();
        }

        JSONObject matrixJSON = getSimilarityMatrixAsJSON(elements, rawMatrix, getWeightsAsArray());
        setName(similarity.getName() + "_similarityMatrix");
        gridFsService.saveFile(new ByteArrayInputStream(matrixJSON.toString().getBytes()), getName());
    }

    public float[][][] getEmptyRawMatrix(int numberOfEntities, int numberOfWeights) {
        return new float[numberOfEntities][numberOfEntities][numberOfWeights];
    }

    public JSONObject getSimilarityMatrixAsJSON(Set<Short> elements, float[][][] matrix, float[] weights) throws JSONException {
        JSONObject matrixData = new JSONObject();
        JSONArray matrixJSON = new JSONArray();

        int i = 0;
        for (short e1ID : elements) {
            JSONArray matrixRow = new JSONArray();
            int j = 0;

            for (short e2ID : elements) {
                if (e1ID == e2ID) {
                    matrixRow.put(1);
                    j++;
                    continue;
                }

                float metric = 0;
                for (int k = 0; k < weights.length; k++)
                    metric += matrix[i][j][k] * weights[k] / 100;

                matrixRow.put(metric);
                j++;
            }
            matrixJSON.put(matrixRow);
            i++;
        }

        matrixData.put("elements", elements);
        matrixData.put("matrix", matrixJSON);
        return matrixData;
    }

    public Set<String> generateMultipleMatrices(GridFsService gridFsService, Recommendation recommendation, Set<Short> elements, int totalNumberOfWeights) throws Exception {
        setGridFsService(gridFsService);

        float[][][] rawMatrix = getEmptyRawMatrix(elements.size(), totalNumberOfWeights);

        int fillFromIndex = 0;
        for (Weights weights : getWeightsList()) {
            weights.fillMatrix(gridFsService, recommendation, rawMatrix, elements, fillFromIndex);
            fillFromIndex += weights.getNumberOfWeights();
        }

        int[] weights = new int[totalNumberOfWeights];
        weights[0] = INTERVAL;
        int[] remainders = new int[totalNumberOfWeights];
        remainders[0] = INTERVAL;

        Set<String> similarityMatrices = new HashSet<>();
        getMatrixCombinations(similarityMatrices, elements, rawMatrix, weights, remainders, 0);
        return similarityMatrices;
    }

    // Creates matrices based on combinations of weights ([100, 0, 0], [90, 10, 0], [90, 0, 10], [80, 20, 0], [80, 10, 10], ...)
    public void getMatrixCombinations(
            Set<String> similarityMatrices,
            Set<Short> elements,
            float[][][] rawMatrix,
            int[] weights,
            int[] remainders, int i
    )
            throws Exception
    {
        if (i + 1 == remainders.length) {
            createAndWriteSimilarityMatrix(similarityMatrices, elements, rawMatrix, weights);
            return;
        }
        else {
            remainders[i + 1] = remainders[i] - weights[i];
            weights[i + 1] = remainders[i + 1];
            getMatrixCombinations(similarityMatrices, elements, rawMatrix, weights, remainders, i+1);
        }

        weights[i] = weights[i] - STEP;
        if (weights[i] >= 0)
            getMatrixCombinations(similarityMatrices, elements, rawMatrix, weights, remainders, i);
    }

    private void createAndWriteSimilarityMatrix(
            Set<String> similarityMatrices,
            Set<Short> elements,
            float[][][] rawMatrix,
            int[] weights
    ) throws Exception {
        float[] weightsAsFloats = new float[weights.length];
        for (int i = 0; i < weights.length; i++)
            weightsAsFloats[i] = weights[i];

        JSONObject matrixJSON = getSimilarityMatrixAsJSON(elements, rawMatrix, weightsAsFloats);

        StringBuilder similarityMatrixName = new StringBuilder(getName());
        for (float weight : weights)
            similarityMatrixName.append(",").append(getWeightAsString(weight));

        similarityMatrices.add(similarityMatrixName.toString());
        gridFsService.saveFile(new ByteArrayInputStream(matrixJSON.toString().getBytes()), similarityMatrixName.toString());
    }

    public String getWeightAsString(float weight) {
        return Float.toString(weight).replaceAll("\\.?0*$", "");
    }
}
