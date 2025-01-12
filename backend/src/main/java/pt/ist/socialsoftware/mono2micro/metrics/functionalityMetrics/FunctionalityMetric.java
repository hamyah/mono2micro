package pt.ist.socialsoftware.mono2micro.metrics.functionalityMetrics;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInfo.AccessesInfo;
import pt.ist.socialsoftware.mono2micro.functionality.domain.Functionality;
import pt.ist.socialsoftware.mono2micro.metrics.Metric;

public abstract class FunctionalityMetric extends Metric {
    public abstract Object calculateMetric(AccessesInfo accessesInfo, Decomposition decomposition, Functionality functionality) throws Exception;
}
