package pt.ist.socialsoftware.mono2micro.domain.clusteringAlgorithm;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import pt.ist.socialsoftware.mono2micro.domain.Cluster;
import pt.ist.socialsoftware.mono2micro.domain.decomposition.AccessesSciPyDecomposition;
import pt.ist.socialsoftware.mono2micro.domain.source.AccessesSource;
import pt.ist.socialsoftware.mono2micro.domain.strategy.AccessesSciPyStrategy;
import pt.ist.socialsoftware.mono2micro.domain.strategy.Strategy;
import pt.ist.socialsoftware.mono2micro.dto.decompositionDto.AccessesSciPyRequestDto;
import pt.ist.socialsoftware.mono2micro.dto.decompositionDto.RequestDto;
import pt.ist.socialsoftware.mono2micro.manager.CodebaseManager;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import static pt.ist.socialsoftware.mono2micro.domain.source.Source.SourceType.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.utils.Constants.CODEBASES_PATH;
import static pt.ist.socialsoftware.mono2micro.utils.Constants.SCRIPTS_ADDRESS;

public class SciPyClusteringAlgorithm implements ClusteringAlgorithm {

    public SciPyClusteringAlgorithm() {}

    @Override
    public void createDendrogram(Strategy strategy) {
        //Creates dendrogram image
        WebClient.create(SCRIPTS_ADDRESS)
                .get()
                .uri("/scipy/{codebaseName}/{strategyName}/createDendrogram", strategy.getCodebaseName(), strategy.getName())
                .exchange()
                .doOnSuccess(clientResponse -> {
                    if (clientResponse.statusCode() != HttpStatus.OK)
                        throw new RuntimeException("Error Code:" + clientResponse.statusCode());
                }).block();
    }

    @Override
    public void createDecomposition(Strategy strategy, RequestDto requestDto) throws Exception {
        CodebaseManager codebaseManager = CodebaseManager.getInstance();

        switch (strategy.getType()) {
            case Strategy.StrategyType.ACCESSES_SCIPY:
                AccessesSciPyRequestDto accessesSciPyCutDto = (AccessesSciPyRequestDto) requestDto;
                AccessesSciPyStrategy accessesSciPyStrategy = (AccessesSciPyStrategy) strategy;
                AccessesSource source = (AccessesSource) codebaseManager.getCodebaseSource(accessesSciPyStrategy.getCodebaseName(), ACCESSES);

                AccessesSciPyDecomposition decomposition = cut(accessesSciPyStrategy, accessesSciPyCutDto);

                decomposition.setupFunctionalities(
                        source.getInputFilePath(),
                        source.getProfile(accessesSciPyStrategy.getProfile()),
                        accessesSciPyStrategy.getTracesMaxLimit(),
                        accessesSciPyStrategy.getTraceType());

                // Calculate decomposition's metrics
                decomposition.calculateMetrics();

                codebaseManager.writeStrategyDecomposition(accessesSciPyStrategy.getCodebaseName(), accessesSciPyStrategy.getName(), decomposition);
                accessesSciPyStrategy.addDecompositionName(decomposition.getName());
                codebaseManager.writeCodebaseStrategy(accessesSciPyStrategy.getCodebaseName(), accessesSciPyStrategy);
                break;

            default:
                throw new RuntimeException("Unknown strategy type when creating a decomposition: " + strategy.getType());
        }
    }

    private AccessesSciPyDecomposition cut(AccessesSciPyStrategy strategy, AccessesSciPyRequestDto cutDto)
            throws Exception
    {
        AccessesSciPyDecomposition decomposition = new AccessesSciPyDecomposition();
        // If these values are needed, these next lines could be added to the decomposition,
        // but since the name of the decomposition is composed of these values, they are ignored
        //decomposition.setCutValue(cutDto.getCutValue());
        //decomposition.setCutType(cutDto.getCutValue());
        decomposition.setCodebaseName(strategy.getCodebaseName());
        decomposition.setStrategyName(strategy.getName());

        JSONObject clustersJSON;
        if (cutDto.getExpertName() == null) { // Decomposition produced by a cut in the dendrogram
            decomposition.setName(getDecompositionName(strategy, cutDto));
            createDecompositionDirectory(decomposition.getCodebaseName(), decomposition.getStrategyName(), decomposition.getName());

            invokePythonCut(strategy, decomposition.getName(), cutDto);

            clustersJSON = CodebaseManager.getInstance().getClusters(strategy.getCodebaseName(), strategy.getName(), decomposition.getName());
        }
        else { // When in an expert decomposition
            if (strategy.getDecompositionsNames().contains(cutDto.getExpertName()))
                throw new KeyAlreadyExistsException();
            decomposition.setName(cutDto.getExpertName());
            decomposition.setExpert(true);

            createDecompositionDirectory(decomposition.getCodebaseName(), decomposition.getStrategyName(), decomposition.getName());

            if (cutDto.getExpertFile().isPresent()) { // Expert decomposition with file
                InputStream is = new BufferedInputStream(cutDto.getExpertFile().get().getInputStream());
                clustersJSON = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                is.close();
            }
            else {
                createGenericDecomposition(strategy, decomposition);
                return decomposition;
            }
        }

        addClustersAndEntities(decomposition, clustersJSON);

        return decomposition;
    }

    private void createDecompositionDirectory(String codebaseName, String strategyName, String decompositionName) {
        new File(CODEBASES_PATH + codebaseName + "/strategies/" + strategyName + "/decompositions/" + decompositionName).mkdir();
    }

    private void createGenericDecomposition(AccessesSciPyStrategy strategy, AccessesSciPyDecomposition decomposition) throws Exception {
        Cluster cluster = new Cluster((short) 0, "Generic");

        JSONObject similarityMatrixData = CodebaseManager.getInstance().getSimilarityMatrix(
                strategy.getCodebaseName(),
                strategy.getName()
        );

        JSONArray entities = similarityMatrixData.getJSONArray("entities");

        for (int i = 0; i < entities.length(); i++) {
            short entityID = (short) entities.getInt(i);

            cluster.addEntity(entityID);
            decomposition.putEntity(entityID, cluster.getID());
        }

        decomposition.addCluster(cluster);
    }

    private void invokePythonCut(AccessesSciPyStrategy strategy, String decompositionName, AccessesSciPyRequestDto cutDto) {

        WebClient.create(SCRIPTS_ADDRESS)
                .get()
                .uri("/scipy/{codebaseName}/{strategyName}/{graphName}/{cutType}/{cutValue}/createDecomposition",
                        strategy.getCodebaseName(), strategy.getName(), decompositionName, cutDto.getCutType(), Float.toString(cutDto.getCutValue()))
                .exchange()
                .doOnSuccess(clientResponse -> {
                    if (clientResponse.statusCode() != HttpStatus.OK)
                        throw new RuntimeException("Error Code:" + clientResponse.statusCode());
                }).block();
    }

    private String getDecompositionName(AccessesSciPyStrategy strategy, AccessesSciPyRequestDto cutDto) {
        String cutValueString = Float.valueOf(cutDto.getCutValue()).toString().replaceAll("\\.?0*$", "");

        if (strategy.getDecompositionsNames().contains(cutDto.getCutType() + cutValueString)) {
            int i = 2;
            while (strategy.getDecompositionsNames().contains(cutDto.getCutType() + cutValueString + "(" + i + ")"))
                i++;
            return cutDto.getCutType() + cutValueString + "(" + i + ")";

        } else return cutDto.getCutType() + cutValueString;
    }

    private void addClustersAndEntities(AccessesSciPyDecomposition decomposition, JSONObject clustersJSON) throws JSONException {
        Iterator<String> clusters = clustersJSON.getJSONObject("clusters").sortedKeys();
        ArrayList<String> clusterNames = new ArrayList<>();

        while(clusters.hasNext())
            clusterNames.add(clusters.next());

        Collections.sort(clusterNames);

        short clusterID = 0;
        for (String name : clusterNames) {
            JSONArray entities = clustersJSON.getJSONObject("clusters").getJSONArray(name);
            Cluster cluster = new Cluster(clusterID, name);

            for (int i = 0; i < entities.length(); i++) {
                short entityID = (short) entities.getInt(i);

                cluster.addEntity(entityID);
                decomposition.putEntity(entityID, clusterID);
            }
            clusterID++;

            decomposition.addCluster(cluster);
        }
    }
}
