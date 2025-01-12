package pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInfo;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.metrics.decompositionMetrics.DecompositionMetric;

import java.util.List;
import java.util.Set;

public abstract class RepresentationInfo {
    String decompositionName;
    public abstract String getType();

    public abstract void deleteProperties();

    public abstract void setup(Decomposition decomposition) throws Exception;

    public abstract void update(Decomposition decomposition) throws Exception;
    public abstract void snapshot(Decomposition snapshotDecomposition, Decomposition decomposition) throws Exception;

    public abstract List<DecompositionMetric> getDecompositionMetrics();

    public void renameClusterInFunctionalities(String clusterName, String newName) {}

    public void removeFunctionalitiesWithEntityIDs(Decomposition decomposition, Set<Short> elements) {}

    public abstract String getEdgeWeights(Decomposition decomposition) throws Exception;
    public abstract String getSearchItems(Decomposition decomposition) throws Exception;

    public String getDecompositionName() {
        return decompositionName;
    }

    public void setDecompositionName(String decompositionName) {
        this.decompositionName = decompositionName;
    }
}
