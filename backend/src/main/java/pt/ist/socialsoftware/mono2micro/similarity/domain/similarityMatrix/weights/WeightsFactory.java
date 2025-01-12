package pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights;

import java.util.ArrayList;
import java.util.List;

public class WeightsFactory {
    public static Weights getWeights(String type) {
        switch (type) {
            case AccessesWeights.ACCESSES_WEIGHTS:
                return new AccessesWeights();
            case RepositoryWeights.REPOSITORY_WEIGHTS:
                return new RepositoryWeights();
            default:
                throw new RuntimeException("The type \"" + type + "\" is not a valid weight type.");
        }
    }

    public static List<Weights> getWeightsList(List<String> types) {
        List<Weights> weightsList = new ArrayList<>();
        for (String weightsType : types)
            weightsList.add(getWeights(weightsType));
        return weightsList;
    }
}
