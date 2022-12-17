import AccessesWeights, {ACCESSES_WEIGHTS} from "./AccessesWeights";
import Weights from "./Weights";
import RepositoryWeights, {REPOSITORY_WEIGHTS} from "./RepositoryWeights";
import ClassVectorizationWeights, {CLASS_VECTORIZATION_WEIGHTS} from "./ClassVectorizationWeights";
import EntityVectorizationWeights, {ENTITY_VECTORIZATION_WEIGHTS} from "./EntityVectorizationWeights";
import FunctionalityVectorizationCallGraphWeights, {FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS} from "./FunctionalityVectorizationCallGraphWeights";
import FunctionalityVectorizationAccessesWeights, {FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS} from "./FunctionalityVectorizationAccessesWeights";
import {RepresentationInfoType} from "../representation/RepresentationInfoTypes";

export abstract class WeightsFactory {
    static getWeights(weights: any) : Weights {
        switch (weights.type) {
            case ACCESSES_WEIGHTS:
                return new AccessesWeights(weights);
            case REPOSITORY_WEIGHTS:
                return new RepositoryWeights(weights);
            case CLASS_VECTORIZATION_WEIGHTS:
                return new ClassVectorizationWeights(weights);
            case ENTITY_VECTORIZATION_WEIGHTS:
                return new EntityVectorizationWeights(weights);
            case FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS:
                return new FunctionalityVectorizationCallGraphWeights(weights);
            case FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS:
                return new FunctionalityVectorizationAccessesWeights(weights);
            default:
                throw new Error('Type ' + weights.type + ' unknown.');
        }
    }

    static getWeightsList(weightsList: any[]) : Weights[] {
        return weightsList.map(weights => WeightsFactory.getWeights(weights));
    }

    static getWeightListByRepresentationInfoType(representationTypes: string[]) : Weights[] {

        if (representationTypes.includes(RepresentationInfoType.ACCESSES_INFO) && representationTypes.includes(RepresentationInfoType.REPOSITORY_INFO))
            return [
                new AccessesWeights({ accessMetricWeight: 17, writeMetricWeight: 17, readMetricWeight: 17, sequenceMetricWeight: 17}),
                new RepositoryWeights({authorMetricWeight: 16, commitMetricWeight: 16})
            ];
        else if (representationTypes.includes(RepresentationInfoType.ACCESSES_INFO))
            return [new AccessesWeights({accessMetricWeight: 25, writeMetricWeight: 25, readMetricWeight: 25, sequenceMetricWeight: 25})];
        else if (representationTypes.includes(RepresentationInfoType.REPOSITORY_INFO))
            return [new RepositoryWeights({authorMetricWeight: 50, commitMetricWeight: 50})];
        else if (representationTypes.includes(RepresentationInfoType.CLASS_VECTORIZATION_INFO))
            return [new ClassVectorizationWeights({})];
        else if (representationTypes.includes(RepresentationInfoType.ENTITY_VECTORIZATION_INFO))
            return [];
        else if (representationTypes.includes(RepresentationInfoType.FUNCTIONALITY_VECTORIZATION_CALLGRAPH_INFO))
            return [new FunctionalityVectorizationCallGraphWeights({controllersWeight: 25, servicesWeight: 25, intermediateMethodsWeight: 25, entitiesWeight: 25})];
        else if (representationTypes.includes(RepresentationInfoType.FUNCTIONALITY_VECTORIZATION_ACCESSES_INFO))
            return [new FunctionalityVectorizationAccessesWeights({readMetricWeight: 50, writeMetricWeight: 50})];
        throw new Error('No known type of Representation Info in Weights Factory.');
    }
}
